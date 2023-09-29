package vastblue.file

import vastblue.Platform._ // mountMap

import java.io.{File => JFile}
import java.nio.file.{Path => JPath}
import java.nio.file.{Files => JFiles, Paths => JPaths}
import scala.collection.immutable.ListMap
import scala.util.control.Breaks._
import java.io.{BufferedReader, FileReader}
import scala.util.Using
import scala.sys.process._
import vastblue.pathextend.hook

/*
 * Enable access to the synthetic winshell filesystem provided by
 * Cygwin64, MinGW64, Msys2, Gitbash, etc.
 *
 * Permits writing of scripts that are portable portable between
 * Linux, Osx, and windows shell environments.
 *
 * To create a winshell-friendly client script:
 *    +. import vastblue.file.Paths rather than java.nio.file.Paths
 *    +. call `findInPath(binaryPath)` or `where(binaryPath)` to find executable
 *
 * The following are used to navigate the synthetic winshell filesystem.
 *
 * bashPath: String    : valid path to the bash executable
 * shellBaseDir: String: root directory of the synthetic filesystem
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
 * NOTES:
 * Treats the Windows default drive root (typically C:\) as the filesystem root.
 * Other drives may be made available by symlink off of the filesystem root.
 * Shell environment (/cygwin64, /msys64, etc.) must be on the default drive.
 *
 * The preferred way to find an executable on the PATH (very fast):
 *   val p: Path = findInPath(binaryName)
 *
 * A Fallback method (much slower):
 *   val path: String = whichPath(binaryName)
 *
 * Most of the magic is available via Paths.get(), defined below.
 *
 * How to determine where msys2/ mingw64 / cygwin64 is installed?
 * best answer: norm(where(s"bash${exeSuffix}")).replaceFirst("/bin/bash.*","")
 */
object Paths {
  type Path = java.nio.file.Path

  def get(dirpathstr: String, subpath: String): Path = {
    val dirpath       = derefTilde(dirpathstr) // replace leading tilde with sys.props("user.home")
    def subIsAbsolute = JPaths.get(subpath).isAbsolute
    if (subpath.startsWith("/") || subIsAbsolute) {
      sys.error(s"dirpath[$dirpath], subpath[$subpath]")
    }
    get(s"$dirpath/$subpath")
  }
  def driveRelative(p: Path): Boolean = {
    p.toString.startsWith("/") && !p.isAbsolute
  }

  lazy val DriveLetterColonPattern = "([a-zA-Z]):(.*)?".r
  lazy val CygdrivePattern         = s"${cygdrive}([a-zA-Z])(/.*)?".r

  // There are three supported filename patterns:
  //    non-windows (posix by default; the easy case)
  //    windows drive-relative path, with default drive, e.g., /Windows/system32
  //    windows absolute path, e.g., c:/Windows/system32
  // Windows paths are normalized to forward slash
  def get(fnamestr: String): Path = {
    val pathstr = derefTilde(fnamestr) // replace leading tilde with sys.props("user.home")
    if (notWindows) {
      JPaths.get(pathstr)
    } else if (pathstr == ".") {
      herepath // based on sys.props("user.dir")
    } else {
      val _normpath = pathstr.replace('\\', '/')
      val normpath = _normpath.take(2) match {
        case dl if dl.startsWith("/") =>
          // apply mount map to paths with leading slash
          applyPosix2LocalMount(_normpath) // becomes absolute, if mounted
        case _ =>
          _normpath
      }
      def dd = driveRoot.toUpperCase.take(1)
      val (literalDrive, impliedDrive, pathtail) = normpath match {
        case DriveLetterColonPattern(dl, tail) => // windows drive letter
          (dl, dl, tail)
        case CygdrivePattern(dl, tail) => // cygpath drive letter
          (dl, dl, tail)
        case pstr if pstr.matches("/proc(/.*)?") => // /proc file system
          ("", "", pstr) // must not specify a drive letter!
        case pstr if pstr.startsWith("/") => // drive-relative path, with no drive letter
          // drive-relative paths are on the current-working-drive,
          ("", dd, pstr)
        case pstr => // relative path, implies default drive
          (dd, "", pstr)
      }
      val semipath          = Option(pathtail).getOrElse("/")
      val neededDriveLetter = if (impliedDrive.nonEmpty) s"$impliedDrive:" else ""
      val fpstr             = s"${neededDriveLetter}$semipath" // derefTilde(pathstr)
      if (literalDrive.nonEmpty) {
        // no need for cygpath if drive is unambiguous.
        val fpath =
          if (fpstr.endsWith(":") && fpstr.take(3).length == 2 && fpstr.equalsIgnoreCase(driveRoot)) {
            // fpstr is a drive letter expression.
            // Windows interprets a bare drive letter expression as
            // the "working directory" each drive had at jvm startup.
            cwd
          } else {
            jget(fpstr)
          }
        normPath(fpath)
      } else {
        jget(fpstr)
      }
    }
  }

  lazy val posix2localMountMapKeyset = posix2localMountMap.keySet.toSeq.sortBy { -_.length }

  /*
   * @return Some(mapKey) matching pathstr prefix, else None.
   *   posix2localMountMapKeyset pre-sorted by key length (longest first),
   *   to prevent spurious matches if cygpath == "/".
   *   Matched prefix of pathstr must be followed by '/' or end of string.
   */
  def getMounted(pathstr: String): Option[String] = {
    val mounts = posix2localMountMapKeyset
    val lcpath = pathstr.toLowerCase
    mounts.find { (target: String) =>
      def exactMatch: Boolean = {
        target match {
          case "/" =>
            true
          case _ =>
            val pathTail = lcpath.drop(target.length).take(1)
            pathTail match {
              case "" | "/" =>
                true // full segment match
              case _ =>
                false // partial suffix match
            }
        }
      }
      val prefixMatches = lcpath.startsWith(target)
      if (prefixMatches) {
        hook += 1
      }
      prefixMatches && exactMatch
    }
  }

  /*
   * Convert pathstr by applying mountMap.
   */
  def applyPosix2LocalMount(pathstr: String): String = {
    require(pathstr.take(2).last != ':', s"bad argument : ${pathstr}")

    val mounted = getMounted(pathstr)
    val mountTarget = if (mounted.isEmpty) {
      pathstr
    } else {
      val cyg        = s"$cygdrive"
      val mountpoint = mounted.getOrElse("")
      if (mountpoint == cyg) {
        val segments = pathstr.drop(cyg.length).split("/").dropWhile(_.isEmpty)
        require(segments.nonEmpty, s"empty segments for pathstr [$pathstr]")
        val firstSeg = segments.head
        if (firstSeg.length == 1 && isAlpha(firstSeg.charAt(0))) {
          // looks like a cygdrive designator replace '/cygdrive/X' with 'X:/'
          s"$firstSeg:/${segments.tail.mkString("/")}"
        } else {
          pathstr
        }
      } else {
        val Some(posix) = mounted: @unchecked
        val local       = posix2localMountMap(posix)
        pathstr.replaceAll(s"^$posix", local)
      }
    }
    mountTarget
  }

  /*
   * Translate pathstr to posix alias of local path.
   */
  def applyLocal2PosixMount(pathstr: String): String = {
    var nup            = pathstr
    val (dl, segments) = pathSegments(pathstr) // TODO: toss dl?
    require(segments.nonEmpty, s"empty segments for pathstr [$pathstr]")
    var firstSeg = segments.head match {
      case "/" | "" => ""
      case s        => s
    }
    val mounts = posix2localMountMap.keySet.toArray
    val lcpath = pathstr.toLowerCase
    val mounted = mounts.find { (s: String) =>
      lcpath.startsWith(s) && !lcpath.drop(s.length).startsWith("/")
    }
    if (mounted.nonEmpty) {
      // map keys are all lowercase, to provide performant case-insensitive map.
      firstSeg = posix2localMountMap(firstSeg.toLowerCase)
      nup = (firstSeg :: segments.tail.toList).mkString("/")
    } else {
      val driveLetter = (cygdrive, segments.head) match {
        case ("", d) if canBeDriveLetter(d) =>
          s"$d:"
        case _ =>
          ""
      }
      val abs = s"$driveLetter/${segments.tail.mkString("/")}"
      nup = abs
    }
    nup
  }

  def canBeDriveLetter(s: String): Boolean = {
    s.length == 1 && isAlpha(s.charAt(0))
  }
  def isAlpha(c: Char): Boolean = {
    (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
  }

  def jget(fnamestr: String): Path = {
    if (isWindows && fnamestr.contains(";")) {
      sys.error(s"internal error: called JPaths.get with a filename containing a semicolon")
    }
    JPaths.get(fnamestr)
  }

  def derefTilde(str: String): String = if (str.startsWith("~")) {
    // val uh = userhome
    s"${userhome}${str.drop(1)}".replace('\\', '/')
  } else {
    str
  }

  def isSameFile(p1: Path, p2: Path): Boolean = {
    val cs1 = dirIsCaseSensitive(p1)
    val cs2 = dirIsCaseSensitive(p2)
    if (cs1 != cs2) {
      false // not the same file
    } else {
      // JFiles.isSameFile(p1, p2) // requires both files to exist (else crashes)
      val abs1 = p1.toAbsolutePath.toString
      val abs2 = p2.toAbsolutePath.toString
      if (cs1) {
        abs1.equalsIgnoreCase(abs2)
      } else {
        abs1 == abs2
      }
    }
  }

//  in Windows 10+, per-directory case-sensitive filesystem is enabled or not.
//  def dirIsCaseSensitive(p: Path): Boolean = {
//    val s = p.toString.replace('\\', '/')
//    val cmd = Seq("fsutil.exe", "file", "queryCaseSensitiveInfo", s)
//    // windows filesystem case-sensitivity is not common (yet?)
//    cmd.lazyLines_!.mkString("").trim.endsWith(" enabled")
//  }
  // verified on linux and Windows 11; still needed: Darwin/OSX
  def dirIsCaseSensitive(p: Path): Boolean = {
    val pf = p.toFile
    if (!pf.exists) {
      false
    } else {
      val dirpath = if (pf.isFile) {
        pf.getParent
      } else if (pf.isDirectory) {
        p
      } else {
        sys.error(s"internal error: [$p]")
      }
      val dir = dirpath.toString
      val p1  = Paths.get(dir, "A")
      val p2  = Paths.get(dir, "a")
      p1.toAbsolutePath == p2.toAbsolutePath
    }
  }

  lazy val herepath: Path = normPath(sys.props("user.dir"))

  def here = herepath.toString.replace('\\', '/')

  lazy val LetterPath = """([a-zA-Z]):([$\\/a-zA-Z_0-9]*)""".r

  def driveAndPath(filepath: String) = {
    filepath match {
      case LetterPath(letter, path) =>
        (s"$letter:", path)
      case _ =>
        ("", shellRoot)
    }
  }

  def isDirectory(path: String): Boolean = {
    Paths.get(path).toFile.isDirectory
  }

  def userhome: String = sys.props("user.home").replace('\\', '/')
  lazy val home: Path  = Paths.get(userhome)

  def findPath(prog: String, dirs: Seq[String] = envPath): String = {
    // format: off
    dirs.map { dir =>
      Paths.get(s"$dir/$prog")
    }.find { (p: Path) =>
      p.toFile.isFile
    } match {
      case None => ""
      case Some(p) => p.normalize.toString.replace('\\', '/')
    }
  }

  def which(cmdname: String) = {
    val cname = if (exeSuffix.nonEmpty && !cmdname.endsWith(exeSuffix)) {
      s"${cmdname}${exeSuffix}"
    } else {
      cmdname
    }
    findPath(cname)
  }

  def canExist(p: Path): Boolean = {
    // val letters = driveLettersLc.toArray
    val pathdrive = pathDriveletter(p)
    pathdrive match {
      case "" =>
        true
      case letter =>
        driveLettersLc.contains(letter)
    }
  }

  def dirExists(pathstr: String): Boolean = {
    dirExists(Paths.get(pathstr))
  }
  def dirExists(path: Path): Boolean = {
    canExist(path) && JFiles.isDirectory(path)
  }

  def pathDriveletter(ps: String): String = {
    ps.take(2) match {
      case str if str.drop(1) == ":" =>
        str.take(2).toLowerCase
      case _ =>
        ""
    }
  }
  def pathDriveletter(p: Path): String = {
    pathDriveletter(p.toAbsolutePath.toFile.toString)
  }

  // path.toFile.exists very slow if drive not found, fileExists() is faster.
  def fileExists(p: Path): Boolean = {
    canExist(p) &&
      p.toFile.exists
  }
  def exists(path: String): Boolean = {
    exists(Paths.get(path))
  }
  def exists(p: Path): Boolean = {
    canExist(p) && {
      p.toFile match {
        case f if f.isDirectory => true
        case f => f.exists
      }
    }
  }

  // drop drive letter and normalize backslash
  def dropshellDrive(str: String)  = str.replaceFirst(s"^${shellDrive}:", "")
  def dropDriveLetter(str: String) = str.replaceFirst("^[a-zA-Z]:", "")
  def asPosixPath(str: String)     = dropDriveLetter(str).replace('\\', '/')
  def asLocalPath(str: String) = if (notWindows) str
  else
    str match {
      case PosixCygdrive(dl, tail) => s"$dl:/$tail"
      case _                       => str
    }
  lazy val PosixCygdrive = "[\\/]([a-z])([\\/].*)?".r

  def stdpath(path: Path): String = path.toString.replace('\\', '/')
  def stdpath(str: String)        = str.replace('\\', '/')
  def norm(p: Path): String       = p.toString.replace('\\', '/')
  def norm(str: String) =
    str.replace('\\', '/') // Paths.get(str).normalize.toString.replace('\\', '/')

//  lazy val mountMap: Map[String, String] = reverseMountMap.map { (k: String, v: String) => (v -> k)}

//  lazy val cygdrive = mountMap.get("/cygdrive").getOrElse("/cygdrive")

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

  def readWinshellMounts: Map[String, String] = {
    // Map must be ordered: no key may contain an earlier key as a prefix.
    // With an ordered Map, the first match terminates the search.
    var localMountMap = ListMap.empty[String, String]

    // default mounts for cygwin, potentially overridden in fstab
    val bareRoot = shellRoot
    localMountMap += "/usr/bin" -> s"$bareRoot/bin"
    localMountMap += "/usr/lib" -> s"$bareRoot/lib"
    // next 2 are convenient, but MUST be added before reading fstab
    localMountMap += "/bin" -> s"$bareRoot/bin"
    localMountMap += "/lib" -> s"$bareRoot/lib"

    var (cygdrive, usertemp) = ("", "")
    val fpath                = "/proc/mounts"
    val lines: Seq[String] = {
      if (notWindows || shellRoot.isEmpty) {
        Nil
      } else {
        execBinary("cat.exe", "/proc/mounts")
      }
    }

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
          usertemp = mountpoint // need to parse it, but unused here
        case _ =>
          // fstype ignored
          localMountMap += mountpoint -> winpath
      }
    }

    if (cygdrive.isEmpty) {
      cygdrive = "/cygdrive"
    }
    localMountMap += "/cygdrive" -> cygdrive

    val driveLetters: Array[JFile] = {
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

    for (drive <- driveLetters) {
      val letter =
        drive.getAbsolutePath.take(1).toLowerCase // lowercase is typical user expectation
      val winpath = stdpath(
        drive.getCanonicalPath
      ) // retain uppercase, to match cygpath.exe behavior
      // printf("letter[%s], path[%s]\n",letter,winpath)
      localMountMap += s"/$letter" -> winpath
    }
    // printf("bareRoot[%s]\n",bareRoot)
    localMountMap += "/" -> shellRoot // this must be last
    localMountMap
  }

  lazy val driveLettersLc: List[String] = {
    val values = reverseMountMap.values.toList
    val letters = {
      for {
        dl <- values.map { _.take(2) }
        if dl.drop(1) == ":"
      } yield dl.toLowerCase
    }.distinct
    letters
  }

  def eprint(xs: Any*): Unit = {
    System.err.print("%s".format(xs: _*))
  }
  def eprintf(fmt: String, xs: Any*): Unit = {
    System.err.print(fmt.format(xs: _*))
  }

  def fileLines(f: JFile): Seq[String] = {
    val fnorm = f.toString.replace('\\', '/')
    if (isWindows && fnorm.matches("/(proc|sys)(/.*)?")) {
      execBinary("cat.exe", fnorm)
    } else {
      Using
        .resource(new BufferedReader(new FileReader(f))) { reader =>
          Iterator.continually(reader.readLine()).takeWhile(_ != null).toSeq
        }
        .toSeq
    }
  }

  def envOrElse(varname: String, elseValue: String = ""): String = Option(
    System.getenv(varname)
  ) match {
    case None      => elseValue
    case Some(str) => str
  }

  def normPath(_pathstr: String): Path = {
    val jpath: Path = _pathstr match {
      case "." => JPaths.get(sys.props("user.dir"))
      case _   => JPaths.get(_pathstr)
    }
    normPath(jpath)
  }
  def normPath(path: Path): Path = try {
    val s = path.toString
    if (s.length == 2 && s.take(2).endsWith(":")) {
      cwd
    } else {
      path.toAbsolutePath.normalize
    }
  } catch {
    case e: java.io.IOError =>
      path
  }

  // This is limited, in order to work on Windows, which is not Posix-Compliant.
  def _chmod(path: Path, permissions: String = "rw", allusers: Boolean = true): Boolean = {
    val file = path.toFile
    // set application user permissions
    val x = permissions.contains("x") || file.canExecute
    val r = permissions.contains("r") || file.canRead
    val w = permissions.contains("w") || file.canWrite

    var ok = true
    ok &&= file.setExecutable(x)
    ok &&= file.setReadable(r)
    ok &&= file.setWritable(w)
    if (allusers) {
      // change permission for all users
      ok &&= file.setExecutable(x, false)
      ok &&= file.setReadable(r, false)
      ok &&= file.setWritable(w, false)
    }
    ok
  }

  // only verified on linux and Windows 11
  def dirIsCaseSensitiveUniversal(dir: JPath): Boolean = {
    require(dir.toFile.isDirectory, s"not a directory [$dir]")
    val pdir = dir.toAbsolutePath.toString
    val p1   = Paths.get(pdir, "A")
    val p2   = Paths.get(pdir, "a")
    p1.toAbsolutePath == p2.toAbsolutePath
  }
  def sameFile(s1: String, s2: String): Boolean = {
    s1 == s2 || {
      // this addresses filesystem case-sensitivity
      // must NOT call get() from this object (stack overflow)
      val p1 = java.nio.file.Paths.get(s1).toAbsolutePath
      val p2 = java.nio.file.Paths.get(s2).toAbsolutePath
      p1 == p2
    }
  }
  // return drive letter, segments
  def pathSegments(path: String): (String, Seq[String]) = {
    // remove windows drive letter, if present
    val (dl, pathRelative) = path.take(2) match {
      case s if s.endsWith(":") =>
        (path.take(2), path.drop(2))
      case s =>
        (cygdrive, path)
    }
    pathRelative match {
      case "/" | "" =>
        (dl, Seq(pathRelative))
      case _ =>
        (dl, pathRelative.split("[/\\\\]+").filter { _.nonEmpty }.toSeq)
    }
  }
}
