//#!/usr/bin/env -S scala @$HOME/.scala3cp
package vastblue

import vastblue.pathextend._
import vastblue.Platform._
import scala.sys.process._

object ProcInfo {
  def main(args: Array[String]): Unit = {
    try {
      val procs = pidCommandlines()
      for (proc <- procs) {
        printf("[%s]\n", proc)
      }

    } catch {
      case ex: Exception =>
        printf("%s\n", ex.getMessage)
    }
  }

  lazy val thisProc: Proc          = selfCommandline
  lazy val scriptArgs: Seq[String] = thisProc.scriptArgs
  lazy val scriptName: String      = scriptArgs.headOption.getOrElse(scriptPathProperty)

  // Where /proc/self is a file, you can do the following:
  //    val pid = "/proc/self".path.realpath.name
  //    val cmdline = "/proc/self".path.contentAsString.trim
  // Windows must spawn `ls` and `cat`, which report on themselves.
  def selfCommandline: Proc = {
    if (notWindows) {
      val pid     = "/proc/self".path.realpath.name
      val cmdline = s"/proc/$pid/cmdline".path.contentAsString
      Proc(pid, cmdline)
    } else {
      pidCommandlines().toList match {
      case self :: tail => self
      case Nil          => Proc("", sunJavaCommand)
      }
    }
  }

  def pidCommandlines(allpids: Boolean = false): Seq[Proc] = {
    val handle: String = if (allpids) "" else ProcessHandle.current().pid().toString
    if (verbose) printf("handle: %s\n", handle)
    val psArg    = if (isWindows) "-W" else "-e"
    val cmd      = Seq(psExe, psArg)
    val alllines = cmd.lazyLines_!
    val colnames = alllines.head.trim.split(" +")
    if (verbose) printf("%s\n", colnames.mkString("|"))

    val lines = alllines.filter { allpids || _.contains(handle) }

    def getCmdline(pid: String): String = {
      val cmdlineFile = s"/proc/$pid/cmdline"
      val cmd         = Seq(catExe, "-v", cmdlineFile)
      // avoid error messages to Stdout if proc goes away
      val (exit, stdout, stderr) = spawnCmd(cmd, false)
      stdout.headOption.getOrElse("")
    }
    val procs = for {
      psline <- lines
      if handle == "" || psline.contains(handle)
      pid    = getPidFromPs(psline)
      pidcmd = getCmdline(pid)
      if pidcmd.nonEmpty
      proc = Proc(pid, pidcmd)
    } yield proc
    procs
  }
  lazy val sunJavaCommand: String = propOrEmpty("sun.java.command")

  def getPidFromPs(line: String): String = {
    line.trim.split("\\s+").toList match {
    case pid :: tty :: time :: cmd :: Nil =>
      pid
    case pid :: ppid :: pgid :: winpid :: tty :: uid :: stime :: cmd :: Nil =>
      pid
    case other =>
      sys.error(s"""unrecognized ps -e output [$other.mkString("|")]""")
    }
  }

  case class Proc(pid: String, pidcmd: String) {
    val rawargv: Seq[String] = pidcmd.split("\\^@").toSeq
    val jvmcmd: Seq[String] = if (rawargv.head.contains("java")) {
      rawargv
    } else {
      sunJavaCommand.split(" ").toSeq
    }
    val scriptArgs: Seq[String] = {
      import vastblue.script._
      rawargv.dropWhile((s: String) => !s.endsWith(scriptPathProperty) && !legalMainClass(s)).toIndexedSeq
    }
    // some args are double-quoted (to prevent unglobbing, e.g.), remove them
    val argv: Seq[String] = scriptArgs.map { _.filter(_ != '"') } // remove quotes, if present

    // verbose toString shows quotes, to disambiguate args having spaces
    val cmdstr = if (verbose) scriptArgs.mkString("\n[", "]\n[", "]") else scriptArgs.mkString(" ")

    override def toString: String = "pid: %s, argv: %s".format(pid, cmdstr)
  }
}
