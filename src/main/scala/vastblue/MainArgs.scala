//#!/usr/bin/env -S scala @$HOME/.scala3cp
package vastblue

import vastblue.pathextend._
import vastblue.Platform._
import vastblue.script.legalMainClass
import vastblue.Unexpand._

import scala.sys.process._

// format: off
/**
 * This solves 2 problems:
 *   provide the PID of the current running JVM
 *   reconstruct command line args (jvm bug workarounds)
 *
 * The `args` parameter of `main` can be incorrect in `Windows` jvm:
 *   in a Windows `posix` shell, jvm expands quoted `glob` args
 *   (quoted `glob` args are okay in `CMD` or `powershell`)
 */
// format: on
object MainArgs {
  def main(_args: Array[String]): Unit = {
    try {
      // `prepArgs` returns supplemented, preserved command line args
      val argv = prepArgs(_args.toSeq)
      printf("[%s]\n", argv.mkString("|"))
      printf("[%s]\n", _args.mkString("|"))
    } catch {
      case e: Exception =>
        printf("%s\n", e.getMessage)
    }
  }

  def prepArgs(_args: Seq[String]): Seq[String] = {
    if (verbose) {
      printf("_args [%s]\n", _args.mkString("|"))
      // new Exception().printStackTrace()
    }
    val argv = (scriptProp :: _args.toList).toArray.toSeq
    val validArgs = if (!isWindows || shell.isEmpty) {
      argv
    } else {
      // argz respects globs, but does not respect spaces
      // argv respects spaces, but does not respect globs
      val argz = scriptArgz
      if (verbose) {
        printf("argz [%s]\n", argz.mkString("|"))
        printf("argv [%s]\n", argv.mkString("|"))
      }

      val argzGlobs: Boolean = argz.drop(1).exists { isGlob(_) }
      val argvSpace: Boolean = argv.drop(1).exists { hasSpace(_) }
      if (argzGlobs) {
        argv // no problems
      } else if (argvSpace) {
        argz // no problems
      } else {
        if (true) {
          // rebuild args: get args-with-spaces from argv, globs from argz
          vastblue.Unexpand.unexpandArgs(argv, argz)
        } else {
          // winshell heroics needed
          // (this deals with various use-cases, include IDE runtime env)
          val pid         = thisPid
          val smallerArgv = if (argv.size < argz.size) argv else argz
          val cmdline     = procfsCmdline(pid, smallerArgv)
          if (verbose) printf("cmdline[%s]\n", cmdline)
          cmdline.split("\\^@").toSeq
        }
      }
    }
    if (_thisProc.isEmpty) {
      val cli = validArgs.mkString("^@")
      _thisProc = Some(Proc(thisPid, cli))
    }
    validArgs
  }

  private var _thisProc: Option[Proc] = None

  lazy val thisProc: Proc = _thisProc.getOrElse {
    val cli = selfCommandline
    Proc(thisPid, cli.mkString("^@"))
  }

  def scriptArgs: Seq[String] = thisProc.argv
  def scriptName: String      = scriptArgs.headOption.getOrElse(scriptProp)

  private lazy val scriptProp: String = script.scriptProp(new Exception())

  private val scriptArgz: Seq[String] = {
    val sjc = sunJavaCommand.split(" ")
    val argz: Array[String] = sjc.dropWhile { (s: String) =>
      !s.endsWith(scriptProp) && !legalMainClass(s)
    }.toArray

    if (scriptProp.nonEmpty) {
      argz(0) = scriptProp
    }
    argz.toIndexedSeq
  }

  def shell = envOrEmpty("SHELL")

  // get command line in a way that's portable
  def selfCommandline: Seq[String] = {
    val procSelfDir = "/proc/self".path
    if (procSelfDir.isDirectory) {
      val pid     = procSelfDir.realpath.name
      val cmdline = s"/proc/$pid/cmdline".path.contentAsString
      if (verbose) eprintf("cmdline.split with zero\n")
      cmdline.split('\u0000').toSeq
    } else {
      prepArgs(scriptArgz.tail)
    }
  }

  lazy val thisPid: Long = {
    val pid: Long = ProcessHandle.current().pid()

    osType match {
    case "darwin" | "linux" =>
      pid
    case "windows" =>
      val handle = pid.toString
      val cmd    = Seq(psExe, "-W")
      val winproc: Option[WinProc] = {
        val pslines = cmd.lazyLines_!.tail.filter { s =>
          // minimize number of WinProc objects created
          s.contains(handle)
        }
        // format: off
        pslines.map { (s: String) =>
          WinProc(s)
        }.find { (wp: WinProc) =>
          // only one should pass this filter
          wp.winpid == handle
        }
        // format: on
      }
      winproc match {
      case None =>
        -1L // unknown pid
      case Some(wp) =>
        wp.pid
      }
    }
  }

  def matchingIndices(seq: Seq[String])(func: String => Boolean): Seq[Int] = {
    for {
      (s, i) <- seq.zipWithIndex
      if func(s)
    } yield i
  }

  // PID    PPID    PGID     WINPID   TTY         UID    STIME COMMAND
  object WinProc {
    def apply(psline: String): WinProc = {
      val arr = psline.trim.split(" +")

      val Array(pid, ppid, pgid, winpid) = arr.take(4)
      new WinProc(pid.toLong, ppid, pgid, winpid, arr.drop(4).toSeq)
    }
  }
  case class WinProc(pid: Long, ppid: String, pgid: String, winpid: String, tail: Seq[String] = Nil)

  def procfsCmdline(pid: Long, argv: Seq[String]): String = {
    if (pid > 0L) {
      val cmdlineFile = s"/proc/$pid/cmdline"
      val cmd         = Seq(catExe, "-v", cmdlineFile)
      // avoid error messages to Stdout if proc goes away
      val (exit, stdout, stderr) = spawnCmd(cmd, false)
      stdout.headOption.getOrElse {
        argv.mkString("^@")
      }
    } else {
      argv.mkString("^@")
    }
  }

  lazy val sunJavaCommand: String = propOrEmpty("sun.java.command")

  def getPidFromPs(line: String): Long = {
    line.trim.split("\\s+").toList match {
    case pid :: tty :: time :: cmd :: Nil =>
      pid.toLong
    case pid :: ppid :: pgid :: winpid :: tty :: uid :: stime :: cmd :: tail =>
      pid.toLong
    case other =>
      sys.error(s"""unrecognized ps -e output [$other.mkString("|")]""")
    }
  }

  lazy val DummyProc = Proc(-1L, "")

  case class Proc(pid: Long, pidcmd: String) {
    val rawargv: Seq[String] = pidcmd.split("\\^@").toSeq

    def isJava: Boolean = rawargv.head.contains("java")

    val jvmcmd: Seq[String] = if (isJava) {
      rawargv
    } else {
      sunJavaCommand.split(" ").toSeq
    }
    val scriptArgs: Seq[String] = {
      import vastblue.script._
      val scrpath: String       = MainArgs.scriptProp
      def notScriptPath         = (s: String) => !s.endsWith(scrpath) && !legalMainClass(s)
      val rawtail: List[String] = rawargv.dropWhile(notScriptPath(_)).toList
      (scrpath :: rawtail.drop(1)).toIndexedSeq
    }
    // some args are double-quoted (to prevent unglobbing, e.g.), remove them
    val argv: Seq[String] = scriptArgs.map { _.filter(_ != '"') } // remove quotes, if present

    // verbose toString shows quotes, to disambiguate args having spaces
    val cmdstr = if (verbose) scriptArgs.mkString("|") else scriptArgs.mkString(" ")

    override def toString: String = "pid: %s, argv: %s".format(pid, cmdstr)
  }
}