//#!/usr/bin/env scala3
package vastblue

//import vastblue.ExtUtils.getPath
import vastblue.pathextend.*

import java.io.{File => JFile}
import java.nio.file.{Files, Path, Paths}
import scala.collection.immutable.ListMap
import scala.util.control.Breaks.*
import java.io.{BufferedReader, FileReader}
import scala.util.Using
import scala.sys.process.*

/*
 * Low level support for scala in Windows SHELL environments.
 * Makes it easy to write scala scripts that are portable between
 * Linux, Osx and cygwin64/mingw64/msys2 in Windows.
 * Everything should work as expected in all environments.
 *
 * Treats the default drive as the filesystem root. (typically C:/)
 * To make other drives available, symlink them off of the default drive.
 * Assumes shell environment (/cygwin64, /msys64, etc.) is on the default drive.
 *
 * The following are available to navigate the synthetic winshell filesystem.
 * bashPath: String    : valid path to the bash executable
 * realroot: String    : root directory of the synthetic filesystem
 * unamefull: String   : value reported by `uname -a`
 * To identify the environment:
 * isCygwin: Boolean   : true if running cygwin64
 * isMsys64: Boolean   : true if running msys64
 * isMingw64: Boolean  : true if running mingw64
 * isGitSdk64: Boolean : true if running gitsdk
 * isMingw64: Boolean  : true if running mingw64
 * isWinshell
 * wsl: Boolean
 *
 * The preferred way to find an executable on the PATH (very fast):
 *   val p: Path = findInPath(binaryName)
 *
 * A Fallback method (much slower):
 *   val path: String = whichPath(binaryName)
 *
 *
 * How to determine where msys2/ mingw64 / cygwin64 is installed?
 * best answer: norm(where(s"bash${exeSuffix}")).replaceFirst("/bin/bash.*","")
 */
object Platform {
  
  def main(args:Array[String]):Unit = {
    for (arg <- args) {
      val list = findAllInPath(arg)
      printf("found %d [%s] in PATH:\n", list.size, arg)
      for (path <- list) {
        printf(" [%s] found at [%s]\n",arg, path)
        printf("--version: [%s]\n", exec(path.toString, "--version").takeWhile(_ != '('))
      }
    }
    val cwd = ".".path
    for ((p: Path) <- cwd.paths if p.isDirectory){
      printf("%s\n", p.norm)
    }
    for (line <- "/proc/meminfo".path.lines) {
      printf("%s\n", line)
    }

    val prognames = Seq(
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
      "uname",
    )
    for (progname <- prognames){
      val prog = where(progname)
      printf("%-12s: %s\n", progname, prog)
    }

    printf("bashPath     [%s]\n",bashPath)
    printf("cygPath      [%s]\n",cygPath)
    printf("realroot     [%s]\n",realroot)
    printf("realrootbare [%s]\n",realrootbare)
    printf("realrootfull [%s]\n",realrootfull)
    printf("osName       [%s]\n",osName)
    printf("unamefull    [%s]\n",unamefull)
    printf("unameshort   [%s]\n",unameshort)
    printf("isCygwin     [%s]\n",isCygwin)
    printf("isMsys64     [%s]\n",isMsys64)
    printf("isMingw64    [%s]\n",isMingw64)
    printf("isGitSdk64   [%s]\n",isGitSdk64)
    printf("isWinshell   [%s]\n",isWinshell)
    printf("bash in path [%s]\n",findInPath("bash").getOrElse(""))
    printf("cygdrive2root[%s]\n",cygdrive2root)
    printf("wsl          [%s]\n",wsl)
    printf("javaHome     [%s]\n",javaHome)
    printf("etcdir       [%s]\n",etcdir)

    printf("\n")
    printf("all bash in path:\n")
    val bashlist = findAllInPath("bash")
    for (path <- bashlist) {
      printf(" found at %-36s : ", s"[$path]")
      printf("--version: [%s]\n", exec(path.toString, "--version").takeWhile(_ != '('))
    }
    if (possibleWinshellRootDirs.nonEmpty) {
      printf("\nfound %d windows shell root dirs:\n", possibleWinshellRootDirs.size)
      for (root <- possibleWinshellRootDirs){
        printf(" %s\n", root)
      }
    }
  }

  def getPath(s: String): Path = Paths.get(s)

  def getPath(dir: Path, s: String): Path = {
    Paths.get(s"$dir/$s")
  }
  def getPath(dir: String, s: String=""): Path = {
    Paths.get(s"$dir/$s")
  }

  def setSuffix(exeName: String): String = {
    if (!exeName.endsWith(".exe")) {
      s"$exeName$exeSuffix"
    } else {
      exeName
    }
  }

  def localPath(exeName: String): String = where(exeName)

  def bashPath: String = localPath("bash")
  def cygPath: String = localPath("cygpath")
  def notWindows: Boolean = java.io.File.separator == "/"
  def isWindows: Boolean = !notWindows
  def exeSuffix: String = if (isWindows) ".exe" else ""

  // get path to binaryName via 'which.exe' or 'where'
  def where(binaryName: String): String = {
    if (isWindows) {
      val binName = setSuffix(binaryName)
      // exec3 hides stderr: INFO: Could not find files for the given pattern(s)
      exec3("c:/Windows/System32/where.exe", binName).replace('\\', '/')
    } else {
      exec("which", binaryName)
    }
  }

  // a "binary" is a standalone executable
  // counterexamples:
  //    shell builtins (e.g., "echo")
  //    script with a hashbang line
  def execBinary(args: String *): Seq[String] = {
    import scala.sys.process.*
    Process(Array(args:_*)).lazyLines_!
  }

//  private def exec2(prog: String, arg: String): String = {
//    val lines = Process(Seq(prog, arg)).lazyLines_!
//    lines.take(1).mkString("")
//  }
  def spawnCmd(cmd: Seq[String], verbose: Boolean = false): (Int, List[String], List[String]) = {
    var (out, err) = (List[String](), List[String]())

    def toOut(str: String): Unit = {
      if (verbose) printf("stdout[%s]\n", str)
      out ::= str
    }

    def toErr(str: String): Unit = {
      err ::= str
      if (verbose) System.err.printf("stderr[%s]\n", str).asInstanceOf[Unit]
    }
    import scala.sys.process._
    val exit = cmd ! ProcessLogger((o) => toOut(o), (e) => toErr(e))
    (exit, out.reverse, err.reverse)
  }
  private def exec3(prog: String, arg: String): String = {
    val cmd = Seq(prog, arg)
    val (exit, out, err) = spawnCmd(cmd, verbose)
    out.map { _.replace('\\', '/') }.filter{ s => !ignoreList.contains(s) }.take(1).mkString("")
  }

  def ignoreList = Set(
    "C:/ProgramData/anaconda3/Library/usr/bin/cygpath.exe",
    "C:/Windows/System32/bash.exe",
    "C:/Windows/System32/find.exe",
  )
  def exec(args: String *): String = {
    execBinary(args:_*).toList.mkString("")
  }
  def execShell(args: String *): Seq[String] = {
    val cmd = bashPath.norm :: "-c" :: args.toList
    execBinary(cmd:_*)
  }

  // get first path to prog by searching the PATH
  def findInPath(binaryName: String): Option[Path] = {
    findAllInPath(binaryName, findAll = false) match {
      case Nil => None
      case head :: tail => Some(head)
    }
  }

  // get all occurences of binaryName int the PATH
  def findAllInPath(prog: String, findAll: Boolean = true): Seq[Path] = {
 // val path = Paths.get(prog)
    val progname = prog.replace('\\','/').split("/").last // remove path, if present
    var found = List.empty[Path]
    breakable {
      for (dir <- envPath){
        // sort .exe suffix ahead of no .exe suffix
        for (name <- Seq(s"$dir$fsep$progname$exeSuffix", s"$dir$fsep$progname").distinct){
          val p = Paths.get(name)
          if (p.toFile.isFile){
            found ::= p.normalize
            if (! findAll){
              break() // quit on first one
            }
          }
        }
      }
    }
    found.reverse.distinct
  }

  // this is quite slow, you probably should use `where(binaryName)` instead.
  def whichPath(binaryName: String): String = {
    if (isWindows) {
      def exeName = if (binaryName.endsWith(".exe")) {
        binaryName
      } else {
        s"$binaryName.exe"
      }
      def findFirst(binName: String): String = {
        execBinary("where", binName).headOption.getOrElse("")
      }
      findFirst(binaryName) match {
      case "" => findFirst(exeName)
      case pathstr => pathstr
      }
    } else {
      exec("which",binaryName)
    }
  }

  def altJavaHome = envOrElse("JAVA_HOME", "")

  def javaHome = Option(sys.props("java.home")) match {
    case None => altJavaHome
    case Some(path) => path
  }
  lazy val osName = sys.props("os.name")
  lazy val osType: String = osName.takeWhile(_!=' ').toLowerCase match {
    case "windows" => "windows"
    case "linux" => "linux"
    case "mac os x" => "darwin"
    case other =>
      sys.error(s"osType is [$other]")
  }

  lazy val winshellFlag = {
     isDirectory(realroot) && (isCygwin || isMsys64 || isMingw64 || isGitSdk64)
  }
  def isDirectory(path: String): Boolean = {
    Paths.get(path).toFile.isDirectory
  }

  lazy val programFilesX86: String = System.getenv("ProgramFiles(x86)") match {
    case other:String => other
    case null => "c:/Program Files (x86)"
  }

  lazy val home: Path = Paths.get(sys.props("user.home"))
  lazy val debug: Boolean = Paths.get(".debug").toFile.exists
  lazy val verbose = Option(System.getenv("VERBY")) != None

//  def str2ascii(a:String): String = vastblue.util.StringExtras.str2ascii(a)
  lazy val _debug: Boolean = Option(System.getenv("DEBUG")) != None

  def driveLetterColon = Paths.get(".").toAbsolutePath.normalize.toString.take(2)

  // these must all be lowercase
  lazy val defaultCygroot = "/cygwin64"
  lazy val defaultMsysroot = "/msys64"
  lazy val defaultMingwroot = "/mingw"
  lazy val defaultGitsdkroot = "/git-sdk-64"
  lazy val defaultGitbashroot = "/gitbash"
  def isWinshell = isCygwin | isMsys64 | isMingw64 | isGitSdk64 | isGitbash

  def uname(arg: String) = {
    val unamepath = where("uname") match {
    case "" => "uname"
    case str => str
    }
    val ostype = try {
      Process(Seq(unamepath, arg)).lazyLines_!.mkString("")
    } catch {
      case _:Exception =>
        ""
    }
    ostype
  }
  lazy val unamefull = uname("-a")
  lazy val unameshort = ostype.toLowerCase.replaceAll("[^a-z0-9].*","")

  lazy val isCygwin = unameshort.toLowerCase.startsWith("cygwin")
  lazy val isMsys64 = unameshort.toLowerCase.startsWith("msys")
  lazy val isMingw64 = unameshort.toLowerCase.startsWith("mingw")
  lazy val isGitSdk64 = unameshort.toLowerCase.startsWith("git-sdk")
  lazy val isGitbash = unameshort.toLowerCase.startsWith("gitbash")

  def listPossibleRootDirs(startDir: String): Seq[JFile] = {
    Paths.get(startDir).toAbsolutePath.toFile match {
      case dir if dir.isDirectory =>
        // NOTE: /opt/gitbash is excluded by this approach:
        def defaultRootNames = Seq(
          "cygwin64",
          "msys64",
          "git-sdk-64",
          "gitbash",
          "MinGW",
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
  def possibleWinshellRootDirs = {
    listPossibleRootDirs("/") ++ listPossibleRootDirs("/opt")
  }

  // this is less than ideal, better to use `cygpath -m /`
  lazy val rootFromPath = {
    val javalibpath = sys.props("java.library.path").split(psep).toArray.toSeq
    val fromPathTest = javalibpath.map { asPosixPath(_) }.map { str =>
      str match {
        case str if str.startsWith("/cygwin") =>
          s"/${str.drop(1).replaceAll("/.*","")}"
        case str if str.startsWith("/msys64") =>
          s"/${str.drop(1).replaceAll("/.*","")}"
        case _ =>
          ""
      }
    }
    fromPathTest.filter { _.nonEmpty }.take(1).toList match {
    case Nil =>
      ""
    case str :: _ =>
      s"${driveLetterColon}${str}"
    }
  }

  def rootFromBashPath: String = {
    val nb = norm(bashPath)
    val guess = nb.replaceFirst("/bin/bash.*","") match {
    case str if str.endsWith("/usr") =>
      str.substring(0, str.length - 4)
    case str => str
    }
    val guessPath = Paths.get(guess)
    if ( Files.isDirectory(guessPath) ){
      guess
    } else {
      sys.error(s"unable to determine winshell root dir in $osName")
    }
    if ( Files.isDirectory(guessPath) ){
      guess
    } else {
      sys.error(s"unable to determine winshell root dir in $osName")
    }
  }

  def realrootfull: String = realroot

  lazy val mountMap = {
    val rr: String = realroot
    val etcFstab = s"$rr/etc/fstab".replaceAll("[\\/]+", "/")
    val f = java.nio.file.Paths.get(etcFstab)
    if (!f.isFile) {
      Map.empty[String, String]
    } else {
      {
        for {
          line <- f.lines
          trimmed = line.trim.replaceAll("\\s*#.*", "")
          if trimmed.nonEmpty
          ff = trimmed.replaceAll("\\s*#.*", "").split("\\s+", -1)
          if ff.size >= 3
          Array(local, _posix, tag) = ff.take(3)
          posix = if (tag == "cygdrive") "/cygdrive" else _posix
        } yield (posix, local)
      }.toMap
    }
  }

  def cwdstr = java.nio.file.Paths.get(".").toAbsolutePath.toString.replace('\\', '/')

  lazy val defaultDrive = if (isWindows) {
    cwdstr.replaceAll(":.*", ":")
  } else {
    ""
  }
  lazy val cygpathExes = Seq(
    "c:/msys64/usr/bin/cygpath.exe",
    "c:/cygwin64/bin/cygpath.exe",
  )
  lazy val cygpathExe: String = {
    val cpexe = where("cygpath.exe")
    val cp = cpexe match {
      case "" =>
        cygpathExes.find { s =>
          java.nio.file.Paths.get(s).toFile.isFile
        }.getOrElse(cpexe)
      case f =>
        f
    }
    cp
  }
  lazy val realroot: String = {
    if (!isWindows) {
      "/"
    } else {
      val cpe: String = cygpathExe
      if (cpe.isEmpty){
        "/"
      } else {
        exec(cygpathExe, "-m", "/").mkString("")
      }
    }
  }
  def realrootbare = realroot.replaceFirst(s"^(?i)${defaultDrive}:","") match {
    case "" =>
      "/"
    case str =>
      str
  }

  lazy val binDir = {
    val binDirString = s"${realroot}/bin"
    val binDirPath = Paths.get(binDirString)
    binDirPath.toFile.isDirectory match {
    case true =>
      binDirString
    case false =>
      sys.error(s"unable to find binDir at [${binDirString}]")
    }
  }

  def dumpPath():Unit = {
    envPath.foreach { println }
  }

  def ostype = uname("-s")

  lazy val LetterPath = """([a-zA-Z]):([$/a-zA-Z_0-9]*)""".r

  def driveAndPath(filepath:String) = {
    filepath match {
    case LetterPath(letter,path) =>
      (letter.toLowerCase, path)
    case _ =>
      ("",realrootfull)
    }
  }

  lazy val envPath: Seq[String] = Option(System.getenv("PATH")) match {
    case None => Nil
    case Some(str) => str.split(psep).toList.map { canonical(_) }.distinct
  }
  def canonical(str: String): String = {
    Paths.get(str) match {
    case p if p.toFile.exists => p.normalize.toString
    case p => p.toString
    }
  }

  def norm(str:String) = Paths.get(str).normalize.toString match {
    case "." => "."
    case p => p.replace('\\','/')
  }

  def checkPath(dirs:Seq[String],prog:String): String = {
    dirs.map { dir =>
      Paths.get(s"$dir/$prog")
    }.find { (p:Path) =>
      p.toFile.isFile
    } match {
      case None => ""
      case Some(p) => p.normalize.toString.replace('\\','/')
    }
  }

  def whichInPath(prog:String):String = {
    checkPath(envPath,prog)
  }
  def which(cmdname:String) = {
    val cname = if (exeSuffix.nonEmpty && ! cmdname.endsWith(exeSuffix)) {
      s"${cmdname}${exeSuffix}"
    } else {
      cmdname
    }
    whichInPath(cname)
  }

  def verbyshow(str: String): Unit = if( verbose ) eprintf("verby[%s]\n",str)

  def dirExists(pathstr:String):Boolean = {
    dirExists(Paths.get(pathstr))
  }
  def dirExists(path:Path):Boolean = {
    canExist(path) && Files.isDirectory(path)
  }

  def pathDriveletter(ps:String):String = {
    ps.take(2) match {
    case str if str.drop(1) == ":" =>
      str.take(2).toLowerCase
    case _ =>
      ""
    }
  }
  def pathDriveletter(p:Path):String = {
    pathDriveletter(p.toAbsolutePath.toFile.toString)
  }

  def canExist(p:Path):Boolean = {
 // val letters = driveLettersLc.toArray
    val pathdrive = pathDriveletter(p)
    pathdrive match {
    case "" =>
      true
    case letter =>
      driveLettersLc.contains(letter)
    }
  }

  // fileExists() solves the Windows jvm problem that path.toFile.exists
  // is VEEERRRY slow for files on a non-existent drive (e.g., q:/).
  def fileExists(p:Path):Boolean = {
    canExist(p) &&
      p.toFile.exists
  }
  def exists(path:String): Boolean = {
    exists(Paths.get(path))
  }
  def exists(p:Path): Boolean = {
    canExist(p) && {
      p.toFile match {
        case f if f.isDirectory => true
        case f => f.exists
      }
    }
  }

  // drop drive letter and normalize backslash
  def dropDefaultDrive(str: String) = str.replaceFirst(s"^${defaultDrive}:","")
  def dropDriveLetter(str: String) = str.replaceFirst("^[a-zA-Z]:","")
  def asPosixPath(str:String) = dropDriveLetter(str).replace('\\','/')
  def stdpath(path:Path):String = path.toString.replace('\\','/')
  def stdpath(str:String) = str.replace('\\','/')
  def norm(p: Path): String = p.toString.replace('\\', '/')

  def etcdir = getPath(realrootfull,"etc") match {
    case p if Files.isSymbolicLink(p) =>
      p.toRealPath()
    case p =>
      p
  }
  def defaultCygdrivePrefix = unamefull match {
  case "cygwin" => "/cygdrive"
  case _ => ""
  }
  lazy val (_mountMap, cygdrive2root) = {
    if( verbose ) printf("etcdir[%s]\n",etcdir)
    val fpath = Paths.get(s"$etcdir/fstab")
    //printf("fpath[%s]\n",fpath)
    val lines: Seq[String] = if (fpath.toFile.isFile) {
      val src = scala.io.Source.fromFile(fpath.toFile,"UTF-8")
      src.getLines().toList.map { _.replaceAll("#.*$","").trim }.filter { _.nonEmpty }
    } else {
      Nil
    }

    //printf("fpath.lines[%s]\n",lines.toSeq.mkString("\n"))
    var (cygdrive,_usertemp) = ("","")
    // map order prohibits any key to contain an earlier key as a prefix.
    // this implies the use of an ordered Map, and is necessary so that
    // when converting posix-to-windows paths, the first matching prefix terminates the search.
    var localMountMap = ListMap.empty[String,String]
    var cd2r = true // by default /c should mount to c:/ in windows
    if (isWindows) {
      // cygwin provides default values, potentially overridden in fstab
      val bareRoot = realrootfull
      localMountMap += "/usr/bin" -> s"$bareRoot/bin"
      localMountMap += "/usr/lib" -> s"$bareRoot/lib"
      // next 2 are convenient, but MUST be added before reading fstab
      localMountMap += "/bin"     -> s"$bareRoot/bin"
      localMountMap += "/lib"     -> s"$bareRoot/lib"
      for( line <- lines ){
        //printf("line[%s]\n",line)
        val cols = line.split("\\s+",-1).toList
        val List(winpath, _mountpoint, fstype) = cols match {
        case a :: b :: Nil =>         a :: b :: "" :: Nil
        case a :: b :: c :: tail =>   a :: b :: c :: Nil
        case list => sys.error(s"bad line in ${fpath}: ${list.mkString("|")}")
        }
        val mountpoint = _mountpoint.replaceAll("\\040"," ")
        fstype match {
        case "cygdrive" =>
          cygdrive = mountpoint
        case "usertemp" =>
          _usertemp = mountpoint // need to parse it, but unused here
        case _ =>
          // fstype ignored
          localMountMap += mountpoint -> winpath
        }
        //printf("%s\n",cols.size)
        // printf("%s -> %s\n",cols(1),cols(0))
      }
      cd2r = cygdrive == "/" // cygdrive2root (the cygwin default mapping)
      if (cygdrive.isEmpty) {
        cygdrive =defaultCygdrivePrefix
      }
      localMountMap += "/cygdrive" -> cygdrive

      val driveLetters:Array[JFile] = {
        if (false) {
          java.io.File.listRoots() // veeery slow (potentially)
        } else {
          // 1000 times faster
          val dlfiles = for {
            locl <- localMountMap.values.toList
            dl = locl.take(2)
            if dl.drop(1) == ":"
            ff = new JFile(s"$dl/")
          } yield ff
          dlfiles.distinct.toArray
        }
      }

      for( drive <- driveLetters ){
        val letter = drive.getAbsolutePath.take(1).toLowerCase // lowercase is typical user expectation
        val winpath = stdpath(drive.getCanonicalPath) // retain uppercase, to match cygpath.exe behavior
        //printf("letter[%s], path[%s]\n",letter,winpath)
        localMountMap += s"/$letter" -> winpath
      }
      //printf("bareRoot[%s]\n",bareRoot)
    }
    localMountMap +=  "/"        -> realrootfull // this must be last
    (localMountMap,cd2r)
  }

  lazy val driveLettersLc:List[String] = {
    val values = mountMap.values.toList
    val letters = {
      for {
        dl <- values.map { _.take(2) }
        if dl.drop(1) == ":"
      } yield dl.toLowerCase
    }.distinct
    letters
  }

  def eprint(xs: Any*):Unit = {
    System.err.print("%s".format(xs:_*))
  }
  def eprintf(fmt: String, xs: Any*):Unit = {
    System.err.print(fmt.format(xs:_*))
  }

  def userhome: String = sys.props("user.home")

  def fileLines(f: JFile): Seq[String] = {
    Using.resource(new BufferedReader(new FileReader(f))) { reader =>
      Iterator.continually(reader.readLine()).takeWhile(_ != null).toSeq
    }
  }

  def envOrElse(varname: String, elseValue: String = ""): String = Option(System.getenv(varname)) match {
  case None => elseValue
  case Some(str) => str
  }

  lazy val wsl: Boolean = {
    val f = Paths.get("/proc/version").toFile
    def lines: Seq[String] = fileLines(f)
    def contentAsString: String = lines.mkString("\n")
    val test0 = f.isFile && contentAsString.contains("Microsoft")
    val test1 = unamefull.contains("microsoft")
    test0 || test1
  }

  def cwd = Paths.get(".").toAbsolutePath
  def fsep = java.io.File.separator
  def psep = sys.props("path.separator")

  // useful for benchmarking
  /*
  def time(n: Int, func : (String) => Any): Unit = {
    val t0 = System.currentTimeMillis
    for (i <- 0 until n) {
      func("bash.exe")
    }
    for (i <- 0 until n) {
      func("bash")
    }
    val elapsed = System.currentTimeMillis - t0
    printf("%d iterations in %9.6f seconds\n", n * 2, elapsed.toDouble/1000.0)
  }
  */
}
