package vastblue

import vastblue.pathextend._

import java.io.{File => JFile}
import java.nio.file.{FileSystemException, Path}
import java.nio.file.{Files => JFiles, Paths => JPaths}
import scala.collection.immutable.ListMap
import scala.util.control.Breaks._
import java.io.{BufferedReader, FileReader}
import scala.util.Using
import scala.sys.process._
import vastblue.DriveRoot._

/*
 * Support for writing portable/posix scala scripts.
 *
 * Supported environments:
 * Unix / Linux / OSX, Windows shell environments (CYGWIN64, MINGW64,
 * MSYS2, GIT-BASH, etc.).
 *
 * convenience fields:
 * bashPath: Path      : path to bash executable
 * realroot: String    : root directory of the shell environment
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
 *   realroot
 */
object Platform {
  def main(args: Array[String]): Unit = {
    printf("runtime scala version: [%s]\n", vastblue.Info.scalaRuntimeVersion)
    printf("SYSTEMDRIVE: %s\n", envOrElse("SYSTEMDRIVE"))
    for (arg <- args) {
      val list = findAllInPath(arg)
      printf("found %d [%s] in PATH:\n", list.size, arg)
      for (path <- list) {
        printf(" [%s] found at [%s]\n", arg, path.norm)
        printf("--version: [%s]\n", getStdout(path.norm, "--version").take(1).mkString)
      }
    }
    val cwd = ".".path
    for ((p: Path) <- cwd.paths if p.isDirectory) {
      printf("%s\n", p.norm)
    }

    for (line <- "/proc/meminfo".path.lines) {
      printf("%s\n", line)
    }

    for (progname <- prognames) {
      val prog = where(progname)
      printf("%-12s: %s\n", progname, prog)
    }

    printf("bashPath     [%s]\n", bashPath)
    printf("cygpathExe   [%s]\n", cygpathExe)
    printf("realroot     [%s]\n", realroot)
    printf("realrootbare [%s]\n", realrootbare)
    printf("realrootfull [%s]\n", realrootfull)
    printf("osName       [%s]\n", osName)
    printf("unamefull    [%s]\n", unamefull)
    printf("unameshort   [%s]\n", unameshort)
    printf("isCygwin     [%s]\n", isCygwin)
    printf("isMsys64     [%s]\n", isMsys64)
    printf("isMingw64    [%s]\n", isMingw64)
    printf("isGitSdk64   [%s]\n", isGitSdk64)
    printf("isWinshell   [%s]\n", isWinshell)
    printf("bash in path [%s]\n", findInPath("bash").getOrElse(""))
    printf("cygdrive2root[%s]\n", cygdrive2root)
    printf("wsl          [%s]\n", wsl)
    printf("javaHome     [%s]\n", javaHome)
    printf("etcdir       [%s]\n", etcdir)

    printf("\n")
    printf("all bash in path:\n")
    val bashlist = findAllInPath("bash")
    for (path <- bashlist) {
      printf(" found at %-36s : ", s"[$path]")
      printf("--version: [%s]\n", exec(path.toString, "--version").takeWhile(_ != '('))
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

  def notWindows: Boolean = java.io.File.separator == "/"

  def isWindows: Boolean = !notWindows

  def exeSuffix: String = if (isWindows) ".exe" else ""

  def osName = sys.props("os.name")

  lazy val osType: String = osName.takeWhile(_ != ' ').toLowerCase match {
    case "windows"  => "windows"
    case "linux"    => "linux"
    case "mac os x" => "darwin"
    case other =>
      sys.error(s"osType is [$other]")
  }

  def javaHome = Option(sys.props("java.home")) match {
    case None       => envOrElse("JAVA_HOME", "")
    case Some(path) => path
  }

  def getPath(s: String): Path = JPaths.get(s)

  def getPath(dir: Path, s: String): Path = {
    JPaths.get(s"$dir/$s")
  }
  def getPath(dir: String, s: String = ""): Path = {
    JPaths.get(s"$dir/$s")
  }

  def setSuffix(exeName: String): String = {
    if (!exeName.endsWith(".exe")) {
      s"$exeName$exeSuffix"
    } else {
      exeName
    }
  }

  def isDirectory(path: String): Boolean = {
    JPaths.get(path).toFile.isDirectory
  }

//  def cygPath: String     = localPath("cygpath")
//  def localPath(exeName: String): String = where(exeName)

//  def str2ascii(a:String): String = vastblue.util.StringExtras.str2ascii(a)

//  def driveLetterColon = JPaths.get(".").toAbsolutePath.normalize.toString.take(2)

  def isWinshell = isCygwin | isMsys64 | isMingw64 | isGitSdk64 | isGitbash

  lazy val (shellDrive, shellBaseDir) = driveAndPath(shellRoot)

  def driveAndPath(filepath: String) = {
    filepath match {
      case LetterPath(letter, path) =>
        (DriveRoot(letter), path)
      case _ =>
        (DriveRoot(""), realrootfull)
    }
  }
  lazy val LetterPath = """([a-zA-Z]):([$/a-zA-Z_0-9]*)""".r

  // these must all be lowercase
//  def defaultCygroot      = "/cygwin64"
//  def defaultMsysroot     = "/msys64"
//  def defaultMingwroot    = "/mingw"
//  def defaultGitsdkroot   = "/git-sdk-64"
//  def defaultGitbashroot  = "/gitbash"
//  def defaultRtoolsroot   = "/rtools42"
//  def defaultAnacondaroot = "/ProgramData/anaconda3/Library"

  def uname(arg: String) = {
    val unamepath: String = where("uname") match {
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

  def bashPath: Path = {
    val pathstr: String = where("bash")

    val p = pathstr.path
    try {
      p.toRealPath()
    } catch {
      case fse: FileSystemException =>
        p // no permission to follow link
    }
  }

  def execBinary(args: String*): Seq[String] = {
    args.take(1) match {
      case Nil =>
        sys.error(s"missing program name")
        Nil
      case progname =>
        if (progname.isEmpty) {
          hook += 1
        }
        Process(Array(args: _*)).lazyLines_!
    }
  }
  def shellExec(cmd: String): Seq[String] = {
    execBinary(bashPath.norm, "-c", cmd)
  }

  def spawnCmd(cmd: Seq[String], verbose: Boolean = false): (Int, List[String], List[String]) = {
    var (out, err) = (List[String](), List[String]())

    def toOut(str: String): Unit = {
      if (verbose) printf("stdout[%s]\n", str)
      out ::= str
    }

    def toErr(str: String): Unit = {
      err ::= str
      if (verbose) System.err.printf("stderr[%s]\n", str)
    }
    val exit = cmd ! ProcessLogger((o) => toOut(o), (e) => toErr(e))
    (exit, out.reverse, err.reverse)
  }

  def exeFilterList = Set(
    // intellij provides anemic Path; filter problematic versions of various Windows executables.
    "~/AppData/Local/Programs/MiKTeX/miktex/bin/x64/pdftotext.exe",
    "C:/ProgramData/anaconda3/Library/usr/bin/cygpath.exe",
    "C:/Windows/System32/bash.exe",
    "C:/Windows/System32/find.exe",
  )

//  def execShell(args: String*): Seq[String] = {
//    val cmd = bashPath.norm :: "-c" :: args.toList
//    execBinary(cmd: _*)
//  }

  def exec(args: String*): String = {
    execBinary(args: _*).toList.mkString("")
  }

  /*
   * capture stdout, discard stderr.
   */
  def getStdout(prog: String, arg: String): Seq[String] = {
    val cmd = Seq(prog, arg)

    val (exit, out, err) = spawnCmd(cmd, verbose)

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
        Seq("where.exe", "where").lazyLines_!.take(1).toList.mkString("").replace('\\', '/')
      case path =>
        s"$path/System32/where.exe"
    }
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
  def where(binaryName: String): String = {
    if (isWindows) {
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
      exec("which", binaryName)
    }
  }

  // in Windows, cat is needed to read /proc/ files
  lazy val catExe: String = {
    val result = where("cat")
    result match {
      case "" =>
        "cat"
      case prog =>
        prog.replace('\\', '/')
    }
  }

  lazy val unamefull  = uname("-a")
  lazy val unameshort = unamefull.toLowerCase.replaceAll("[^a-z0-9].*", "")
  lazy val isCygwin   = unameshort.toLowerCase.startsWith("cygwin")
  lazy val isMsys64   = unameshort.toLowerCase.startsWith("msys")
  lazy val isMingw64  = unameshort.toLowerCase.startsWith("mingw")
  lazy val isGitSdk64 = unameshort.toLowerCase.startsWith("git-sdk")
  lazy val isGitbash  = unameshort.toLowerCase.startsWith("gitbash")

  lazy val home: Path = JPaths.get(sys.props("user.home"))
  lazy val verbose    = Option(System.getenv("VERBY")).nonEmpty

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
  lazy val shellRoot: String = {
    if (notWindows) "/"
    else {
      val guess     = bashPath.norm.replaceFirst("/[^/]*exe$", "")
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

  lazy val envPath: Seq[String] = Option(System.getenv("PATH")) match {
    case None      => Nil
    case Some(str) => str.split(psep).toList.map { canonical(_) }.distinct
  }

  def canonical(str: String): String = {
    JPaths.get(str) match {
      case p if p.toFile.exists => p.normalize.toString
      case p                    => p.toString
    }
  }

  def realrootfull: String = realroot

  lazy val mountMap = {
    fstabEntries.map { (e: FsEntry) => (e.posix -> e.dir) }.toMap
  }
  lazy val (
    cygdrive: String,
    posix2localMountMap: Map[String, String],
    reverseMountMap: Map[String, String]
  ) = {
    def emptyMap = Map.empty[String, String]
    if (notWindows || shellRoot.isEmpty) {
      ("", emptyMap, emptyMap)
    } else {
      val mmap = mountMap.toList.map { case (k: String, v: String) => (k.toLowerCase -> v) }.toMap
      val rmap = mountMap.toList.map { case (k: String, v: String) => (v.toLowerCase -> k) }.toMap

      val cygdrive = rmap.get("cygdrive").getOrElse("")
      // to speed up map access, convert keys to lowercase
      (cygdrive, mmap, rmap)
      // readWinshellMounts
    }
  }

  case class FsEntry(dir: String, posix: String, ftype: String) {
    override def toString = "%-22s, %-18s, %s".format(dir, posix, ftype)
  }
  lazy val fstabEntries: Seq[FsEntry] = {
    val rr: String = realroot
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
  lazy val cwd  = pwd
  def pwd: Path = JPaths.get(".").toAbsolutePath

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
    if (notWindows) {
      ""
    } else {
      val cpexe = where("cygpath.exe")
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

  lazy val realroot: String = {
    if (notWindows) {
      "/"
    } else {
      val cpe: String = cygpathExe
      if (cpe.isEmpty) {
        "/"
      } else {
        exec(cygpathExe, "-m", "/").mkString("")
      }
    }
  }
  def realrootbare = if (notWindows) {
    realroot
  } else {
    val noDriveLetter = realroot.replaceFirst(s"^(?i)${workingDrive.string}", "")
    noDriveLetter match {
      case "" =>
        "/"
      case str =>
        str
    }
  }

  lazy val binDir = {
    val binDirString = s"${realroot}/bin"
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

  def norm(str: String) = JPaths.get(str).normalize.toString match {
    case "." => "."
    case p   => p.replace('\\', '/')
  }

  def checkPath(dirs: Seq[String], prog: String): String = {
    dirs
      .map { dir =>
        JPaths.get(s"$dir/$prog")
      }
      .find { (p: Path) =>
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

  def verbyshow(str: String): Unit = if (verbose) eprintf("verby[%s]\n", str)

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

  def etcdir = getPath(realrootfull, "etc") match {
    case p if JFiles.isSymbolicLink(p) =>
      p.toRealPath()
    case p =>
      p
  }
  def defaultCygdrivePrefix = unamefull match {
    case "cygwin" => "/cygdrive"
    case _        => ""
  }
  lazy val (_mountMap, cygdrive2root) = {
    if (verbose) printf("etcdir[%s]\n", etcdir)
    val fpath = JPaths.get(s"$etcdir/fstab")
    // printf("fpath[%s]\n",fpath)
    val lines: Seq[String] = if (fpath.toFile.isFile) {
      val src = scala.io.Source.fromFile(fpath.toFile, "UTF-8")
      src.getLines().toList.map { _.replaceAll("#.*$", "").trim }.filter { !_.isEmpty }
    } else {
      Nil
    }

    // printf("fpath.lines[%s]\n",lines.toSeq.mkString("\n"))
    var (cygdrive, _usertemp) = ("", "")
    // map order prohibits any key to contain an earlier key as a prefix.
    // this implies the use of an ordered Map, and is necessary so that
    // when converting posix-to-windows paths, the first matching prefix terminates the search.
    var localMountMap = ListMap.empty[String, String]
    var cd2r          = true // by default /c should mount to c:/ in windows
    if (isWindows) {
      // cygwin provides default values, potentially overridden in fstab
      val bareRoot = realrootfull
      localMountMap += "/usr/bin" -> s"$bareRoot/bin"
      localMountMap += "/usr/lib" -> s"$bareRoot/lib"
      // next 2 are convenient, but MUST be added before reading fstab
      localMountMap += "/bin" -> s"$bareRoot/bin"
      localMountMap += "/lib" -> s"$bareRoot/lib"
      for (line <- lines) {
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

      // // sometimes very slow
      // java.io.File.listRoots()

      // // 1000 times faster
      // val dlfiles = for {
      //   locl <- localMountMap.values.toList
      //   dl = locl.take(2)
      //   if dl.drop(1) == ":"
      //   ff = new JFile(s"$dl/")
      // } yield ff
      // dlfiles.distinct.toArray

      for (drive <- driveLetters) {
        // lowercase posix drive letter, e.g. "C:" ==> "/c"
        val letter = drive.string.toLowerCase // .take(1).toLowerCase
        // winpath preserves uppercase DriveRoot (cygpath.exe behavior)
        val winpath = stdpath(s"$drive/".path.toAbsolutePath)
        localMountMap += s"/$letter" -> winpath
      }
    }
    localMountMap += "/" -> realrootfull // this must be last
    (localMountMap, cd2r)
  }

  lazy val cygdrivePrefix = reverseMountMap.get("cygdrive").getOrElse("")

  def eprint(xs: Any*): Unit = {
    System.err.print("%s".format(xs: _*))
  }
  def eprintf(fmt: String, xs: Any*): Unit = {
    System.err.print(fmt.format(xs: _*))
  }

  def userhome: String = sys.props("user.home").replace('\\', '/')

  def fileLines(f: JFile): Seq[String] = {
    Using.resource(new BufferedReader(new FileReader(f))) { reader =>
      Iterator.continually(reader.readLine()).takeWhile(_ != null).toSeq
    }
  }

  def envOrElse(varname: String, elseValue: String = ""): String = Option(
    System.getenv(varname)
  ) match {
    case None      => elseValue
    case Some(str) => str
  }

  lazy val wsl: Boolean = {
    val f                       = JPaths.get("/proc/version").toFile
    def lines: Seq[String]      = fileLines(f)
    def contentAsString: String = lines.mkString("\n")
    val test0                   = f.isFile && contentAsString.contains("Microsoft")
    val test1                   = unamefull.contains("microsoft")
    test0 || test1
  }
}
