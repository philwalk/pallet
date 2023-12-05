package vastblue

import vastblue.pallet._

import java.io.{File => JFile}
import java.nio.file.{FileSystemException, Path}
import java.nio.file.{Files => JFiles, Paths => JPaths}
import scala.collection.immutable.ListMap
import scala.util.control.Breaks._
import java.io.{BufferedReader, FileReader}
import scala.util.Using
import scala.sys.process._
import vastblue.DriveRoot
import vastblue.DriveRoot._
import scala.collection.mutable.{Map => MutMap}

/*
 * Support for writing portable/posix scala scripts.
 *
 * Supported environments:
 * Unix / Linux / OSX, Windows shell environments (CYGWIN64, MINGW64,
 * MSYS2, GIT-BASH, etc.).
 *
 * convenience fields:
 * bashPath: Path      : path to bash executable
 * posixroot: String   : root directory of the shell environment
 * unamefull: String   : value reported by `uname -a`
 *
 * To identify the environment:
 *   isCygwin: Boolean   : true if running cygwin64
 *   isMsys64: Boolean   : true if running msys64
 *   isMingw64: Boolean  : true if running mingw64
 *   isGitSdk64: Boolean : true if running gitsdk
 *   isMingw64: Boolean  : true if running mingw64
 *   isWinshell
 *   wsl: Boolean
 *
 * The preferred way to find an executable on the PATH (very fast):
 *   val p: Path = findInPath(binaryName)
 *
 * A Fallback method (much slower):
 *   val path: String = whichPath(binaryName)
 *
 * How to determine where msys2/ mingw64 / cygwin64 is installed?
 *   posixroot
 */
object Platform {
//  import PidCmd._

  def main(args: Array[String]): Unit = {
    printf("runtime scala version: [%s]\n", vastblue.Info.scalaRuntimeVersion)
    printf("SYSTEMDRIVE: %s\n", _envOrEmpty("SYSTEMDRIVE"))
    for (arg <- args) {
      val list = findAllInPath(arg)
      printf("found %d [%s] in PATH:\n", list.size, arg)
      for (path <- list) {
        printf(" [%s] found at [%s]\n", arg, path.norm)
        printf("--version: [%s]\n", getStdout(path.norm, "--version").take(1).mkString)
      }
    }
    val cwd = ".".path.toAbsolutePath

    for ((p: Path) <- cwd.paths if p.isDirectory) {
      printf("%s\n", p.norm)
    }

    val meminfo = "/proc/meminfo".path
    for (line <- meminfo.lines) {
      printf("%s\n", line)
    }

    for (progname <- prognames) {
      val prog = _where(progname)
      printf("%-12s: %s\n", progname, prog)
    }

    printf("cygdrive     [%s]\n", cygdrive)
    printf("bashPath     [%s]\n", _bashPath)
    printf("cygpathExe   [%s]\n", cygpathExe)
    printf("posixroot    [%s]\n", posixroot)
    printf("osName       [%s]\n", _osName)
    printf("unameLong    [%s]\n", _unameLong)
    printf("unameshort   [%s]\n", _unameShort)
    printf("isCygwin     [%s]\n", _isCygwin)
    printf("isMsys64     [%s]\n", _isMsys)
    printf("isMingw64    [%s]\n", _isMingw)
    printf("isGitSdk64   [%s]\n", _isGitSdk)
    printf("isWinshell   [%s]\n", _isWinshell)
    printf("bash in path [%s]\n", findInPath("bash").getOrElse(""))
    printf("cygdrive2root[%s]\n", cygdrive2root)
    printf("wsl          [%s]\n", wsl)
    printf("javaHome     [%s]\n", _javaHome)
    printf("etcdir       [%s]\n", etcdir)

    printf("\n")
    printf("all bash in path:\n")
    val bashlist = findAllInPath("bash")
    for (path <- bashlist) {
      printf(" found at %-36s : ", s"[$path]")
      printf("--version: [%s]\n", _exec(path.toString, "--version").takeWhile(_ != '('))
    }
    if (possibleWinshellRootDirs.nonEmpty) {
      printf("\nfound %d windows shell root dirs:\n", possibleWinshellRootDirs.size)
      for (root <- possibleWinshellRootDirs) {
        printf(" %s\n", root)
      }
    }
  }
  lazy val prognames = Seq(
    "basename",
    "bash",
    "cat",
    "chgrp",
    "chmod",
    "chown",
    "cksum",
    "cp",
    "curl",
    "date",
    "diff",
    "env",
    "file",
    "find",
    "git",
    "gzip",
    "head",
    "hostname",
    "ln",
    "ls",
    "md5sum",
    "mkdir",
    "nohup",
    "uname"
  )

  // private fields to be imported by pallet.*
  private[vastblue] lazy val _unameLong: String  = _uname("-a")
  private[vastblue] lazy val _unameShort: String = _unameLong.toLowerCase.replaceAll("[^a-z0-9].*", "")

  private[vastblue] def _isWinshell: Boolean = _isMsys | _isCygwin | _isMingw | _isGitSdk | _isGitbash
  private[vastblue] def _isDarwin: Boolean   = _osType == "darwin"

  private[vastblue] def unameTest(s: String): Boolean = _unameShort.toLowerCase.startsWith(s)

  private[vastblue] lazy val _isLinux: Boolean   = unameTest("linux")
  private[vastblue] lazy val _isCygwin: Boolean  = unameTest("cygwin")
  private[vastblue] lazy val _isMsys: Boolean    = unameTest("msys")
  private[vastblue] lazy val _isMingw: Boolean   = unameTest("mingw")
  private[vastblue] lazy val _isGitSdk: Boolean  = unameTest("git-sdk")
  private[vastblue] lazy val _isGitbash: Boolean = unameTest("gitbash")

  private[vastblue] def _notWindows: Boolean = java.io.File.separator == "/"
  private[vastblue] def _isWindows: Boolean  = !_notWindows

  private[vastblue] def _osName: String = sys.props("os.name")

  private[vastblue] lazy val _osType: String = _osName.toLowerCase match {
  case s if s.contains("windows")  => "windows"
  case s if s.contains("linux")    => "linux"
  case s if s.contains("mac os x") => "darwin"
  case other =>
    sys.error(s"osType is [$other]")
  }

  private[vastblue] def _javaHome: String  = _propElseEnv("java.home", "JAVA_HOME")
  private[vastblue] def _scalaHome: String = _propElseEnv("scala.home", "SCALA_HOME")
  private[vastblue] def _username: String  = _propOrElse("user.name", "unknown")
  private[vastblue] def _userhome: String  = _propOrElse("user.home", _envOrElse("HOST", "unknown")).replace('\\', '/')
  private[vastblue] def _hostname: String  = _envOrElse("HOSTNAME", _envOrElse("COMPUTERNAME", exec("hostname"))).trim

  private[vastblue] def _eprint(xs: Any*): Unit               = System.err.print("%s".format(xs: _*))
  private[vastblue] def _oprintf(fmt: String, xs: Any*): Unit = System.out.printf(fmt, xs) // suppresswarnings:discarded-value
  private[vastblue] def _eprintf(fmt: String, xs: Any*): Unit = System.err.print(fmt.format(xs: _*))

  private[vastblue] def _propOrElse(name: String, alt: String): String = System.getProperty(name, alt)

  private[vastblue] def _propOrEmpty(name: String): String = _propOrElse(name, "")

  private[vastblue] def _envOrElse(name: String, alt: String): String = Option(System.getenv(name)).getOrElse(alt)

  private[vastblue] def _envOrEmpty(name: String) = _envOrElse(name, "")

  private[vastblue] def _propElseEnv(propName: String, envName: String, alt: String = ""): String = {
    Option(sys.props("java.home")) match {
    case None       => altJavaHome
    case Some(path) => path
    }
  }
  // executable Paths
  private[vastblue] def _bashPath: Path  = _pathCache("bash")
  private[vastblue] def _catPath: Path   = _pathCache("cat")
  private[vastblue] def _findPath: Path  = _pathCache("find")
  private[vastblue] def _whichPath: Path = _pathCache("which")
  private[vastblue] def _unamePath: Path = _pathCache("uname")
  private[vastblue] def _lsPath: Path    = _pathCache("ls")
  private[vastblue] def _trPath: Path    = _pathCache("tr")
  private[vastblue] def _psPath: Path    = _pathCache("ps")

  // executable Path Strings, suitable for calling exec("bash", ...)
  private[vastblue] def _bashExe: String  = _exeCache("bash")
  private[vastblue] def _catExe: String   = _exeCache("cat")
  private[vastblue] def _findExe: String  = _exeCache("find")
  private[vastblue] def _whichExe: String = _exeCache("which")
  private[vastblue] def _unameExe: String = _exeCache("uname")
  private[vastblue] def _lsExe: String    = _exeCache("ls")
  private[vastblue] def _trExe: String    = _exeCache("tr")
  private[vastblue] def _psExe: String    = _exeCache("ps")

  private val foundPaths: MutMap[String, Path]  = MutMap.empty[String, Path]
  private val foundExes: MutMap[String, String] = MutMap.empty[String, String]

  def getPath(s: String): Path = Paths.get(s)

  def getPath(dir: Path, s: String): Path = JPaths.get(s"$dir/$s") // JPaths

  def getPath(dir: String, s: String = ""): Path = Paths.get(s"$dir/$s")

  def exeSuffix: String = if (_isWindows) ".exe" else ""

  def setSuffix(exeName: String): String = {
    if (_isWindows && !exeName.endsWith(".exe")) {
      s"$exeName$exeSuffix"
    } else {
      exeName
    }
  }

  def isDirectory(path: String): Boolean = {
    JPaths.get(path).toFile.isDirectory
  }

  def cygPath: String = localPath("cygpath")

  def altJavaHome: String = _envOrElse("JAVA_HOME", "")

  def localPath(exeName: String): String = _where(exeName)

  lazy val (shellDrive, shellBaseDir) = driveAndPath(_shellRoot)

  def driveAndPath(filepath: String) = {
    filepath match {
    case LetterPath(letter, path) =>
      (DriveRoot(letter), path)
    case _ =>
      (DriveRoot(""), posixroot)
    }
  }
  lazy val LetterPath = """([a-zA-Z]): ([$/a-zA-Z_0-9]*)""".r

  def _uname(arg: String) = {
    val unamepath: String = _where("uname") match {
    case "" =>
      val prdirs = possibleWinshellRootDirs.sortBy { -_.lastModified }.take(1)
      prdirs match {
      case Seq(dir) =>
        dir.toString
      case _ =>
        "uname"
      }
    case str =>
      str
    }
    try {
      Process(Seq(unamepath, arg)).lazyLines_!.mkString("")
    } catch {
      case _: Exception =>
        ""
    }
  }

  def driveRoot = JPaths.get("").toAbsolutePath.getRoot.toString.take(2)

  def _pathCache(name: String): Path = {
    val exePath = foundPaths.get(name) match {
    case Some(path) =>
      path
    case None =>
      val rr = posixroot
      // search for `name` below posixroot, else take first in PATH
      val candidates = Seq(
        s"${rr}usr/bin/$name$exeSuffix",
        s"${rr}bin/$name$exeSuffix"
      ).map { (s: String) =>
        Paths.get(s)
      }.filter {
        _.isFile
      }

      val pathstr: String = candidates.toList match {
      case exe :: tail => exe.norm
      case Nil         => _where(name)
      }

      var pexe: Path = Paths.get(pathstr)
      try {
        pexe = pexe.toRealPath()
      } catch {
        case fse: FileSystemException =>
        // no permission to follow link
      }
      foundPaths(name) = pexe
      foundExes(name) = pexe.norm
      pexe
    }
    exePath
  }

  def _exeCache(name: String): String = {
    foundExes.get(name) match {
    case Some(pathString) => pathString
    case None             => _pathCache(name).norm
    }
  }

  def prepArgs(args: String*): Seq[String] = {
    args.take(1) match {
    case Nil =>
      sys.error(s"missing program name")
      Nil
    case Seq(progname) =>
      val exe = _exeCache(progname)
      exe :: args.toList
    }
  }

  /*
   * Generic command line, return stdout.
   * Stderr is handled by `func` (println by default).
   */
  def executeCmd(_cmd: Seq[String])(func: String => Unit = System.err.println): (Int, List[String]) = {
    val cmd    = prepArgs(_cmd: _*).toArray
    var stdout = List[String]()

    val exit = Process(cmd) ! ProcessLogger(
      (out) => stdout ::= out,
      (err) => func(err)
    )
    (exit, stdout.reverse)
  }

  private[vastblue] def _shellExec(str: String): LazyList[String] = {
    val cmd = Seq(_exeCache("bash"), "-c", str)
    Process(cmd).lazyLines_!
  }

  // similar to _shellExec, but more control
  private[vastblue] def _bashCommand(cmdstr: String, envPairs: List[(String, String)] = Nil): (Boolean, Int, Seq[String], Seq[String]) = {
    import scala.sys.process.*
    var (stdout, stderr) = (List.empty[String], List.empty[String])
    if (bashExe.toFile.exists) {
      val cmd  = Seq(bashExe, "-c", cmdstr)
      val proc = Process(cmd, None, envPairs*)

      val exitVal = proc ! ProcessLogger((out: String) => stdout ::= out, (err: String) => stderr ::= err)

      // a misconfigured environment (e.g., script is not executable) can prevent script execution
      val validTest = !stderr.exists(_.contains("Permission denied"))
      if (!validTest) {
        printf("\nunable to execute script, return value is %d\n", exitVal)
        stderr.foreach { System.err.printf("stderr [%s]\n", _) }
      }

      (validTest, exitVal, stdout.reverse, stderr.reverse)
    } else {
      (false, -1, Nil, Nil)
    }
  }

  lazy val here: java.io.File = Paths.get(".").toFile

  private[vastblue] def _shellExec(str: String, env: Map[String, String]): LazyList[String] = {
    val cmd      = Seq(_exeCache("bash"), "-c", str)
    val envPairs = env.map { case (a, b) => (a, b) }.toList
    val proc     = Process(cmd, here, envPairs: _*)
    proc.lazyLines_!
  }

  def spawnCmd(cmd: Seq[String], verbose: Boolean = false): (Int, List[String], List[String]) = {
    var (out, err) = (List[String](), List[String]())

    def toOut(str: String): Unit = {
      if (verbose) printf("stdout[%s]\n", str)
      out ::= str
    }

    def toErr(str: String): Unit = {
      err ::= str
      if (verbose) System.err.printf("stderr[%s]\n[%s]\n", str, cmd.mkString("|"))
    }
    val exit = cmd ! ProcessLogger((o) => toOut(o), (e) => toErr(e))
    (exit, out.reverse, err.reverse)
  }

  def procCmdlineReader(pidfile: String) = {
    import scala.sys.process._
    val cmd = Seq(_catExe, "-A", pidfile)
    val str = cmd.lazyLines_!.mkString("\n")
    // -v causes non-printing characters to be displayed (zero becomes '^@'?)
    // val str = Seq(bashExe, "-c", "bash", bashcmd).lazyLines_!.mkString("\n")
    str
  }

  def exeFilterList: Seq[String] = {
    Seq(
      // intellij provides anemic Path; filter problematic versions of various Windows executables.
      s"${_userhome}/AppData/Local/Programs/MiKTeX/miktex/bin/x64/pdftotext.exe",
      "C:/ProgramData/anaconda3/Library/usr/bin/cygpath.exe",
      s"${_userhome}/AppData/Local/Microsoft/WindowsApps/bash.exe",
      "C:/Windows/System32/find.exe",
    )
  }

  // returns a String
  private[vastblue] def _exec(args: String*): String = {
    _execLines(args: _*).toList.mkString("")
  }

  // returns a LazyList
  private[vastblue] def _execLines(args: String*): LazyList[String] = {
    // depends on PATH
    require(args.nonEmpty)
    Process(args).lazyLines_!
  }

  /*
   * capture stdout, discard stderr.
   */
  def getStdout(prog: String, arg: String): Seq[String] = {
    val cmd = Seq(prog, arg)

    val (exit, out, err) = spawnCmd(cmd, _verbose)

    out.map { _.replace('\\', '/') }.filter { s =>
      !exeFilterList.contains(s)
    }
  }

  def getStdout(prog: String, args: Seq[String]): Seq[String] = {
    val cmd = prog :: args.toList

    val (exit, out, err) = spawnCmd(cmd, _verbose)

    out.map { _.replace('\\', '/') }.filter { s =>
      !exeFilterList.contains(s)
    }
  }

  // find binaryName in PATH
  def findInPath(binaryName: String): Option[Path] = {
    findAllInPath(binaryName, findAll = false) match {
    case Nil          => None
    case head :: tail => Some(head)
    }
  }

  // find all occurences of binaryName in PATH
  def findAllInPath(prog: String, findAll: Boolean = true): Seq[Path] = {
    // isolate program name
    val progname = prog.replace('\\', '/').split("/").last
    var found    = List.empty[Path]
    breakable {
      for (dir <- envPath) {
        // sort .exe suffix ahead of no .exe suffix
        for (name <- Seq(s"$dir$fsep$progname$exeSuffix", s"$dir$fsep$progname").distinct) {
          val p = JPaths.get(name)
          if (p.toFile.isFile) {
            found ::= p.normalize
            if (!findAll) {
              break() // quit on first one
            }
          }
        }
      }
    }
    found.reverse.distinct
  }

  lazy val WINDIR = Option(System.getenv("SYSTEMROOT")).getOrElse("").replace('\\', '/')
  lazy val whereExe = {
    WINDIR match {
    case "" =>
      _whereFunc("where")
    case path =>
      s"$path/System32/where.exe"
    }
  }

  private[vastblue] def _whereFunc(s: String): String = {
    Seq("where.exe", s).lazyLines_!.toList
      .map { _.replace('\\', '/') }
      .filter { (s: String) =>
        !exeFilterList.contains(s)
      }
      .take(1)
      .mkString
  }

  // the following is to assist finding a usable posix environment
  // when cygpath.exe is not found in the PATH.
  lazy val winshellBinDirs: Seq[String] = Seq(
    "c:/msys64/usr/bin",
    "c:/cygwin64/bin",
    "c:/rtools42/usr/bin",
    "c:/Program Files/Git/bin",
    "c:/Program Files/Git/usr/bin",
  ).filter { _.path.isDirectory }

  def discovered(progname: String): String = {
    val found = winshellBinDirs
      .find { (dir: String) =>
        val cygpath: Path = JPaths.get(dir, progname)
        cygpath.isFile
      }
      .map { (dir: String) => s"$dir/$progname" }
    found.getOrElse("")
  }

  // get path to binaryName via 'which.exe' or 'where'
  private[vastblue] def _where(binaryName: String): String = {
    if (_isWindows) {
      // prefer binary with .exe extension, ceteris paribus
      val binName = setSuffix(binaryName)
      // getStdout hides stderr: INFO: Could not find files for the given pattern(s)
      val fname: String = getStdout(whereExe, binName).take(1).toList match {
      case Nil =>
        discovered(binName)
      case str :: tail =>
        str
      }
      fname.replace('\\', '/')
    } else {
      _exec("which", binaryName)
    }
  }

  lazy val _verbose = Option(System.getenv("VERBY")).nonEmpty

  def listPossibleRootDirs(startDir: String): Seq[JFile] = {
    JPaths.get(startDir).toAbsolutePath.toFile match {
    case dir if dir.isDirectory =>
      def defaultRootNames = Seq(
        "cygwin64",
        "msys64",
        "git-sdk-64",
        "Git",
        "gitbash",
        "MinGW",
        "rtools42",
      )
      dir.listFiles.toList.filter { f =>
        f.isDirectory && defaultRootNames.exists { name =>
          f.getName.contains(name)
        }
      }
    case path =>
      Nil
    }
  }

  // root from the perspective of shell environment
  lazy val _shellRoot: String = {
    if (_notWindows) "/"
    else {
      val guess     = _bashPath.norm.replaceFirst("/[^/]*exe$", "")
      val guessPath = JPaths.get(guess) // call JPaths.get here to avoid circular reference
      if (JFiles.isDirectory(guessPath)) {
        guess
      } else {
        // sys.error(s"unable to determine winshell root dir in $osName")
        "" // no path prefix applicable
      }
    }
  }

  lazy val programFiles = Option(System.getenv("PROGRAMFILES")).getOrElse("")
  def possibleWinshellRootDirs = {
    listPossibleRootDirs("/") ++ listPossibleRootDirs("/opt") ++ listPossibleRootDirs(programFiles)
  }

  lazy val envPath: List[String] = Option(System.getenv("PATH")) match {
  case None      => Nil
  case Some(str) => str.split(psep).toList.map { canonical(_) }.distinct
  }

  def canonical(str: String): String = {
    JPaths.get(str) match {
    case p if p.toFile.exists => p.normalize.toString
    case p                    => p.toString
    }
  }

  lazy val mountMap = {
    fstabEntries.map { (e: FsEntry) => (e.posix -> e.dir) }.toMap
  }
  lazy val (
    cygdrive: String,
    posix2localMountMap: Map[String, String],
    reverseMountMap: Map[String, String]
  ) = {
    def emptyMap = Map.empty[String, String]
    if (_notWindows || _shellRoot.isEmpty) {
      ("", emptyMap, emptyMap)
    } else {
      val mmap = mountMap.toList.map { case (k: String, v: String) => (k.toLowerCase -> v) }.toMap
      val rmap = mountMap.toList.map { case (k: String, v: String) => (v.toLowerCase -> k) }.toMap

      val cygdrive = rmap.get("cygdrive").getOrElse("") match {
      case "/" =>
        "/"
      case "" =>
        ""
      case s =>
        s"$s/" // need trailing slash
      }
      // to speed up map access, convert keys to lowercase
      (cygdrive, mmap, rmap)
      // readWinshellMounts
    }
  }

  case class FsEntry(dir: String, posix: String, ftype: String) {
    override def toString = "%-22s, %-18s, %s".format(dir, posix, ftype)
  }
  lazy val fstabEntries: Seq[FsEntry] = {
    val rr: String = posixroot
    val etcFstab   = s"$rr/etc/fstab".replaceAll("[\\/]+", "/")
    val f          = JPaths.get(etcFstab)
    val entries = if (!f.isFile) {
      Nil
    } else {
      val lines = f.lines
        .map {
          _.trim.replaceAll("\\s*#.*", "")
        }
        .filter {
          !_.trim.isEmpty
        }
      for {
        trimmed <- lines
        ff = trimmed.split("\\s+", -1)
        if ff.size >= 3
        Array(local, posix, ftype) = ff.take(3).map { _.trim }
        dir                        = if (ftype == "cygdrive") "cygdrive" else local
      } yield FsEntry(dir, posix, ftype)
    }
    entries
  }

  def currentWorkingDir(drive: DriveRoot): Path = {
    JPaths.get(drive.string).toAbsolutePath
  }
  def pwd: Path      = JPaths.get(".").toAbsolutePath
  lazy val cwd: Path = pwd

  def fsep = java.io.File.separator
  def psep = sys.props("path.separator")

  def cwdstr = pwd.toString.replace('\\', '/')

  lazy val workingDrive: DriveRoot = DriveRoot(cwdstr.take(2))
//lazy val workingDrive = if (isWindows) cwdstr.replaceAll(":.*", ":") else ""

  lazy val driveLetters: List[DriveRoot] = {
    val values = mountMap.values.toList
    val letters = {
      for {
        dl <- values.map { _.take(2) }
        if dl.drop(1) == ":"
      } yield DriveRoot(dl)
    }.distinct
    letters
  }

  lazy val cygpathExes = Seq(
    "c:/msys64/usr/bin/cygpath.exe",
    "c:/cygwin64/bin/cygpath.exe",
    "c:/rtools64/usr/bin/cygpath.exe",
  )

  lazy val cygpathExe: String = {
    if (_notWindows) {
      ""
    } else {
      val cpexe = _where(s"cygpath${exeSuffix}")
      val cp = cpexe match {
      case "" =>
        // scalafmt: { optIn.breakChainOnFirstMethodDot = false }
        cygpathExes.find { s => JPaths.get(s).toFile.isFile }.getOrElse(cpexe)
      case f =>
        f
      }
      cp
    }
  }

  lazy val posixroot: String = {
    if (_notWindows) {
      "/"
    } else {
      val cpe: String = cygpathExe
      if (cpe.isEmpty) {
        "/"
      } else {
        _exec(cygpathExe, "-m", "/").mkString("")
      }
    }
  }
  lazy val posixrootBare = {
    posixroot.reverse.dropWhile(_ == '/').reverse
  }
  lazy val binDir = {
    val binDirString = s"${posixroot}/bin"
    val binDirPath   = JPaths.get(binDirString)
    binDirPath.toFile.isDirectory match {
    case true =>
      binDirString
    case false =>
      sys.error(s"unable to find binDir at [${binDirString}]")
    }
  }

  def dumpPath(): Unit = {
    envPath.foreach { println }
  }

  def norm(str: String) = {
    try {
      JPaths.get(str).normalize.toString match {
      case "." => "."
      case p   => p.replace('\\', '/')
      }
    } catch {
      case e: Exception =>
        str.replace('\\', '/')
    }
  }

  def checkPath(dirs: Seq[String], prog: String): String = {
    dirs.map { dir => JPaths.get(s"$dir/$prog") }.find { (p: Path) =>
      p.toFile.isFile
    } match {
    case None    => ""
    case Some(p) => p.normalize.toString.replace('\\', '/')
    }
  }

  def whichInPath(prog: String): String = {
    checkPath(envPath, prog)
  }
  def which(cmdname: String) = {
    val cname = if (!exeSuffix.isEmpty && !cmdname.endsWith(exeSuffix)) {
      s"${cmdname}${exeSuffix}"
    } else {
      cmdname
    }
    whichInPath(cname)
  }

  def verbyshow(str: String): Unit = if (_verbose) _eprintf("verby[%s]\n", str)

  def dirExists(pathstr: String): Boolean = {
    dirExists(JPaths.get(pathstr))
  }
  def dirExists(path: Path): Boolean = {
    canExist(path) && JFiles.isDirectory(path)
  }

  def pathDriveletter(ps: String): DriveRoot = {
    ps.take(2) match {
    case str if str.drop(1) == ":" =>
      DriveRoot(str.take(2))
    case _ =>
      DriveRoot("")
    }
  }
  def pathDriveletter(p: Path): DriveRoot = {
    pathDriveletter(p.toAbsolutePath.toString)
  }

  def canExist(p: Path): Boolean = {
    val pathdrive: DriveRoot = pathDriveletter(p)
    pathdrive.string match {
    case "" =>
      true
    case letter =>
      driveLetters.contains(letter)
    }
  }

  // fileExists() solves the Windows jvm problem that path.toFile.exists
  // is VEEERRRY slow for files on a non-existent drive (e.g., q:/).
  def fileExists(p: Path): Boolean = {
    canExist(p) &&
    p.toFile.exists
  }
  def exists(path: String): Boolean = {
    exists(JPaths.get(path))
  }
  def exists(p: Path): Boolean = {
    canExist(p) && {
      p.toFile match {
      case f if f.isDirectory => true
      case f                  => f.exists
      }
    }
  }

  // drop drive letter and normalize backslash
  def dropDefaultDrive(str: String) = str.replaceFirst(s"^${workingDrive}", "")
  def dropDriveLetter(str: String)  = str.replaceFirst("^[a-zA-Z]:", "")
  def asPosixPath(str: String)      = dropDriveLetter(str).replace('\\', '/')
  def stdpath(path: Path): String   = path.toString.replace('\\', '/')
  def stdpath(str: String)          = str.replace('\\', '/')
  def norm(p: Path): String         = p.toString.replace('\\', '/')

  def etcdir = getPath(posixroot, "etc") match {
  case p if JFiles.isSymbolicLink(p) =>
    p.toRealPath()
  case p =>
    p
  }

  def defaultCygdrivePrefix = _unameLong match {
  case "cygwin" => "/cygdrive"
  case _        => ""
  }
  lazy val (_mountMap, cygdrive2root) = {
    if (_verbose) printf("etcdir[%s]\n", etcdir)
    val fpath = JPaths.get(s"$etcdir/fstab")
    // printf("fpath[%s]\n", fpath)
    val lines: Seq[String] = if (fpath.toFile.isFile) {
      val src = scala.io.Source.fromFile(fpath.toFile, "UTF-8")
      src.getLines().toList.map { _.replaceAll("#.*$", "").trim }.filter { !_.isEmpty }
    } else {
      Nil
    }

    // printf("fpath.lines[%s]\n", lines.toSeq.mkString("\n"))
    var (cygdrive, _usertemp) = ("", "")
    // map order prohibits any key to contain an earlier key as a prefix.
    // this implies the use of an ordered Map, and is necessary so that
    // when converting posix-to-windows paths, the first matching prefix terminates the search.
    var localMountMap = ListMap.empty[String, String]
    var cd2r          = true // by default /c should mount to c:/ in windows
    if (_isWindows) {
      // cygwin provides default values, potentially overridden in fstab
      val rr = posixrootBare
      localMountMap += "/usr/bin" -> s"$rr/bin"
      localMountMap += "/usr/lib" -> s"$rr/lib"
      // next 2 are convenient, but MUST be added before reading fstab
      localMountMap += "/bin" -> s"$rr/bin"
      localMountMap += "/lib" -> s"$rr/lib"
      for (line <- lines) {
        // printf("line[%s]\n", line)
        val cols = line.split("\\s+", -1).toList
        val List(winpath, _mountpoint, fstype) = cols match {
        case a :: b :: Nil       => a :: b :: "" :: Nil
        case a :: b :: c :: tail => a :: b :: c :: Nil
        case list                => sys.error(s"bad line in ${fpath}: ${list.mkString("|")}")
        }
        val mountpoint = _mountpoint.replaceAll("\\040", " ")
        fstype match {
        case "cygdrive" =>
          cygdrive = mountpoint
        case "usertemp" =>
          _usertemp = mountpoint // need to parse it, but unused here
        case _ =>
          // fstype ignored
          localMountMap += mountpoint -> winpath
        }
      }
      cd2r = cygdrive == "/" // cygdrive2root (the cygwin default mapping)
      if (cygdrive.isEmpty) {
        cygdrive = defaultCygdrivePrefix
      }
      localMountMap += "/cygdrive" -> cygdrive

      for (drive <- driveLetters) {
        // lowercase posix drive letter, e.g. "C:" ==> "/c"
        val letter = drive.string.toLowerCase // .take(1).toLowerCase
        // winpath preserves uppercase DriveRoot (cygpath.exe behavior)
        val winpath = stdpath(s"$drive/".path.toAbsolutePath)
        localMountMap += s"/$letter" -> winpath
      }
    }
    localMountMap += "/" -> posixroot // this must be last
    (localMountMap, cd2r)
  }

  lazy val cygdrivePrefix = reverseMountMap.get("cygdrive").getOrElse("")

  def fileLines(f: JFile): Seq[String] = {
    Using.resource(new BufferedReader(new FileReader(f))) { reader =>
      Iterator.continually(reader.readLine()).takeWhile(_ != null).toSeq
    }
  }

  lazy val wsl: Boolean = {
    val f                       = JPaths.get("/proc/version").toFile
    def lines: Seq[String]      = fileLines(f)
    def contentAsString: String = lines.mkString("\n")
    val test0                   = f.isFile && contentAsString.contains("Microsoft")
    val test1                   = _unameLong.contains("microsoft")
    test0 || test1
  }

  def ostype = _uname("-s")

  // this may be needed to replace `def canExist` in vastblue.os
  lazy val driveLettersLc: List[String] = {
    val values = mountMap.values.toList
    val letters = {
      for {
        dl <- values.map { _.take(2) }
        if dl.drop(1) == ":"
      } yield dl.toLowerCase
    }.distinct
    letters
  }

  // useful for benchmarking functions
  def time(n: Int, func: (String) => Any): Unit = {
    val t0 = System.currentTimeMillis
    for (i <- 0 until n) {
      func("bash${exeSuffix}")
    }
    for (i <- 0 until n) {
      func("bash")
    }
    val elapsed = System.currentTimeMillis - t0
    printf("%d iterations in %9.6f seconds\n", n * 2, elapsed.toDouble / 1000.0)
  }

  // TODO: for WSL, provide `wslpath`
  // by default, returns -m path
  def _cygpath(exename: String, args: String*): String = {
    if (cygpathExe.nonEmpty) {
      val cmd   = cygpathExe :: (args.toList ::: List(exename))
      val lines = Process(cmd).lazyLines_!
      lines.toList.mkString("").trim
    } else {
      exename
    }
  }

  def cygpathM(path: Path): Path = {
    val cygstr = cygpathM(path.normalize.toString)
    JPaths.get(cygstr)
  }

  def cygpathM(pathstr: String): String = {
    val normed = pathstr.replace('\\', '/')
    val tupes: Option[(String, String)] = reverseMountMap.find { case (k, v) =>
      val normtail = normed.drop(k.length)
      // detect whether a fstab prefix is an exactly match of a normed path string.
      normed.startsWith(k) && (normtail.isEmpty || normtail.startsWith("/"))
    }
    val cygMstr: String = tupes match {
    case Some((k, v)) =>
      val normtail = normed.drop(k.length)
      s"$v$normtail"
    case None =>
      // apply the convention that single letter paths below / are cygdrive references
      if (normed.take(3).matches("/./?")) {
        val dl: String = normed.drop(1).take(1) + ":"
        normed.drop(2) match {
        case "" =>
          s"$dl/" // trailing slash is required here
        case str =>
          s"$dl$str"
        }
      } else {
        normed
      }
    }
    // replace multiple slashes with single slash
    cygMstr.replaceAll("//+", "/")
  }
}
