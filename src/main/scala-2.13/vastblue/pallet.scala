package vastblue

import java.nio.file.{Files => JFiles}
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.file.{Path, Paths}
import java.nio.charset.Charset
import vastblue.util.ArgsUtil

//import java.io.{ByteArrayInputStream, InputStream}
import java.io.{FileOutputStream, OutputStreamWriter}
//import java.security.{DigestInputStream, MessageDigest}
import scala.jdk.CollectionConverters._

import vastblue.Platform
//import vastblue.DriveRoot
import vastblue.DriveRoot._
//import vastblue.MainArgs
import vastblue.file.Util
import vastblue.file.Util._
import vastblue.file.FastCsv
import vastblue.file.FastCsv._
import vastblue.math.Cksum
import vastblue.time.TimeDate.*

object pallet {
  def Paths = vastblue.file.Paths

  type Path        = java.nio.file.Path
  type PrintWriter = java.io.PrintWriter
  type JFile       = java.io.File

  var hook: Int = 0 // breakpoint hook

  def osType: String      = Platform._osType
  def osName: String      = Platform._osName
  def isLinux: Boolean    = Platform._isLinux
  def isWinshell: Boolean = Platform._isWinshell
  def isDarwin: Boolean   = Platform._isDarwin
  def isWsl: Boolean      = Platform._unameLong.contains("WSL")

  def isCygwin: Boolean  = Platform._isCygwin
  def isMsys: Boolean    = Platform._isMsys
  def isMingw: Boolean   = Platform._isMingw
  def isGitSdk: Boolean  = Platform._isGitSdk
  def isGitbash: Boolean = Platform._isGitbash
  def isWindows: Boolean = Platform._isWindows // sometimes useful

  def scalaHome: String = Platform._scalaHome
  def javaHome: String  = Platform._javaHome
  def userhome: String  = Platform._userhome
  def verbose: Boolean  = Platform._verbose
  def username: String  = Platform._username
  def hostname: String  = Platform._hostname

  def uname: (String) => String = Platform._uname

  def cygpath(exename: String, args: String*): String = Platform._cygpath(exename, args: _*)

  def unameLong: String  = Platform._unameLong
  def unameShort: String = Platform._unameShort

  def today     = now
  def yesterday = now - 1.days

  def eprintf(fmt: String, xs: Any*): Unit = Platform._eprintf(fmt, xs: _*)

  def envOrEmpty: (String) => String         = Platform._envOrEmpty
  def envOrElse: (String, String) => String  = Platform._envOrElse
  def propOrEmpty: (String) => String        = Platform._propOrEmpty
  def propOrElse: (String, String) => String = Platform._propOrElse
  def where: (String) => String              = Platform._where
  def exec(args: String*): String            = Platform._exec(args: _*)

  def execLines(args: String*): LazyList[String] = Platform._execLines(args: _*)

  def shellExec(str: String): LazyList[String]                           = Platform._shellExec(str)
  def shellExec(str: String, env: Map[String, String]): LazyList[String] = Platform._shellExec(str, env)

  def pwd: Path = Paths.get(".")

  // executable Paths
  def bashPath: Path  = Platform._bashPath
  def catPath: Path   = Platform._catPath
  def findPath: Path  = Platform._findPath
  def whichPath: Path = Platform._whichPath
  def unamePath: Path = Platform._unamePath
  def lsPath: Path    = Platform._lsPath
  def trPath: Path    = Platform._trPath
  def psPath: Path    = Platform._psPath

  // executable Path Strings, suitable for calling exec("bash", ...)
  def bashExe: String  = Platform._bashExe
  def catExe: String   = Platform._catExe
  def findExe: String  = Platform._findExe
  def whichExe: String = Platform._whichExe
  def unameExe: String = Platform._unameExe
  def lsExe: String    = Platform._lsExe
  def trExe: String    = Platform._trExe
  def psExe: String    = Platform._psExe

  def thisArg: String                  = ArgsUtil.thisArg
  def peekNext: String                 = ArgsUtil.peekNext
  def consumeNext: String              = ArgsUtil.consumeNext
  def consumeDouble: Double            = ArgsUtil.consumeDouble
  def consumeLong: Long                = ArgsUtil.consumeLong
  def consumeInt: Int                  = ArgsUtil.consumeInt
  def consumeBigDecimal: BigDecimal    = ArgsUtil.consumeBigDecimal
  def consumeArgs(n: Int): Seq[String] = ArgsUtil.consumeArgs(n)

  def _usage(m: String, info: Seq[String]): Nothing = ArgsUtil.Usage.usage(m, info)

  def eachArg: (Seq[String], String => Nothing) => (String => Unit) => Unit = ArgsUtil.eachArg _

  def walkTree(file: JFile, depth: Int = 1, maxdepth: Int = -1): Iterable[JFile] = vastblue.file.Util.walkTree(file, depth, maxdepth)

  def filesTree(dir: JFile)(func: JFile => Boolean = dummyFilter): Seq[JFile] = vastblue.file.Util.filesTree(dir)(func)

  def showLimitedStack(e: Throwable = newEx): Unit = vastblue.file.Util._showLimitedStack(e)

  def scriptProp(e: Exception = new Exception()): String = script.scriptProp(e)

  def prepArgs(args: Seq[String]) = script.prepArgs(args)

  def scriptPath: Path        = script.scriptPath
  def scriptName: String      = script.scriptName
  def thisProc: MainArgs.Proc = script.thisProc

  def withFileWriter(p: Path, charset: String = "utf-8", append: Boolean = false)(func: PrintWriter => Any): Unit = {
    Util.withFileWriter(p, charset, append)(func)
  }

  def bashCommand(cmdstr: String, envPairs: List[(String, String)] = Nil): (Boolean, Int, Seq[String], Seq[String]) = {
    Platform._bashCommand(cmdstr, envPairs)
  }

  def quikDate(s: String): DateTime     = Util._quikDate(s)
  def quikDateTime(s: String): DateTime = Util._quikDateTime(s)

  def tmpDir = Seq("/f/tmp", "/g/tmp", "/tmp").find { _.path.isDirectory }.getOrElse("/tmp").path.norm

  implicit class ExtendString(s: String) {
    def path: Path    = vastblue.file.Paths.get(s)
    def toPath: Path  = vastblue.file.Paths.get(s) // alias
    def absPath: Path = s.path.toAbsolutePath.normalize
    def toFile: JFile = toPath.toFile
    def file: JFile   = toFile
    def norm: String  = s.replace('\\', '/')

    def dropSuffix: String = s.reverse.dropWhile(_ != '.').drop(1).reverse
  }

  implicit class ExtendPath(p: Path) {
    def toFile: JFile = p.toFile
    def length: Long  = p.toFile.length
    def file: JFile   = p.toFile

    def realpath: Path        = vastblue.file.Util._realpath(p)
    def getParentFile: JFile  = p.toFile.getParentFile
    def parentFile: JFile     = getParentFile
    def parentPath: Path      = parentFile.toPath
    def parent: Path          = parentPath
    def exists: Boolean       = JFiles.exists(p) // p.toFile.exists()
    def listFiles: Seq[JFile] = p.toFile.listFiles.toList

    def localpath: String = osType match {
    case "windows" =>
      cygpath2driveletter(p.normalize.toString)
    case _ =>
      p.toString
    }

    def dospath: String      = localpath.replace('/', '\\')
    def isDirectory: Boolean = p.toFile.isDirectory
    def isFile: Boolean      = p.toFile.isFile

//  def isRegularFile: Boolean = isFile

    def relpath: Path        = relativize(p)
    def relativePath: String = relpath.norm
    def getName: String      = p.toFile.getName()
    def name: String         = p.toFile.getName
    def lcname: String       = name.toLowerCase
    def basename: String     = p.toFile.basename
    def lcbasename: String   = basename.toLowerCase
    def suffix: String       = dotsuffix.dropWhile((c: Char) => c == '.')
    def dotsuffix: String    = p.toFile.dotsuffix
    def lcsuffix: String     = suffix.toLowerCase

    // toss Windows drive letter, if present
    def noDrive: String = p.norm match {
    case s if s.take(2).endsWith(":") => s.drop(2)
    case s                            => s
    }

    def text: String              = p.contentAsString
    def extension: Option[String] = p.toFile.extension
    def pathFields                = p.iterator.asScala.toList
    def reversePath: String       = pathFields.reverse.mkString("/")

    def lastModified: Long     = p.toFile.lastModified
    def lastModifiedTime       = whenModified(p.toFile)
    def lastModMinutes: Double = lastModSeconds / 60.0
    def lastModHours: Double   = lastModMinutes / 60.0
    def lastModDays: Double    = round(lastModHours / 24.0)
    def lastModSeconds: Double = {
      secondsBetween(lastModifiedTime, now).toDouble
    }

    def weekDay: java.time.DayOfWeek = {
      p.lastModifiedTime.getDayOfWeek
    }

    def round(number: Double, scale: Int = 6): Double = {
      BigDecimal(number).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble
    }

    def files: Seq[JFile] = p.toFile match {
    case f if f.isDirectory => f.files
    case _                  => Nil
    }
    def paths: Seq[Path]      = files.map(_.toPath)
    def dirs: Seq[Path]       = paths.filter { _.isDirectory }
    def filesTree: Seq[JFile] = p.toFile.filesTree
    def pathsTree: Seq[Path]  = p.toFile.pathsTree

    def charsetAndContent: (Charset, String) = vastblue.file.Util.charsetAndContent(p)

    def contentAsString: String = readContentAsString(p)
    def lines: Seq[String]      = readLines(p)
    def bytes: Array[Byte]      = Util.readAllBytes(p)

    def linesAnyEncoding: Seq[String] = Util.readLinesAnyEncoding(p)

    def trimmedLines: Seq[String] = lines // alias

    def trimmedSql: Seq[String] =
      lines.map { _.replaceAll("\\s*--.*", "") }.filter { _.trim.length > 0 }

    def isSymbolicLink: Boolean = JFiles.isSymbolicLink(p)
    def mkdirs: Boolean = {
      val dir = JFiles.createDirectories(p)
      dir.toFile.isDirectory
    }
    def realpathLs: Path = { // ask ls what symlink references
      Platform._exec("ls", "-l", p.norm).split("\\s+->\\s+").toList match {
      case a :: b :: Nil => b.path
      case _             => p
      }
    }

    def ymdHms = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def lastModifiedYMD: String = {
      def lastModified = p.toFile.lastModified
      val date         = new java.util.Date(lastModified)
      ymdHms.format(date)
    }

    def norm: String = nativePathString(p)

    def abspath: String = norm

    // output string should be posix format, either because:
    //   A. non-Windows os
    //   B. C: matching default drive is dropped
    //   C. D: (not matching default drive) is converted to /d
    def stdpath: String = {
      // drop drive letter, if present
      val posix = if (notWindows) {
        pathStr(p)
      } else {
        val nm = nativePathString(p)
        withPosixDriveLetter(nm) // case C
      }
      posix
    }
    def posixpath: String = stdpath
    def delete(): Boolean = toFile.delete()
    def withWriter(charsetName: String = DefaultEncoding, append: Boolean = false)(
        func: PrintWriter => Any
    ): Unit = {
      Util.withPathWriter(p, charsetName, append)(func)
    }

    def dateSuffix: String = {
      lcbasename match {
      case DatePattern1(_, yyyymmdd, _) =>
        yyyymmdd
      case DatePattern2(_, yyyymmdd) =>
        yyyymmdd
      case _ =>
        ""
      }
    }

    // useful for examining shebang line
    def firstline = p.linesAnyEncoding.take(1).mkString("")

    def renameViaCopy(newfile: Path, overwrite: Boolean = false): Int = {
      Util._renameViaCopy(p.toFile, newfile.toFile, overwrite)
    }

    def copyTo(destFile: Path): Int = Util.copyFile(p.file, destFile.file)

    def renameTo(s: String): Boolean = renameTo(s.path)
    def renameTo(alt: Path): Boolean = {
      p.toFile.renameTo(alt)
    }
    def isEmpty: Boolean    = p.toFile.isEmpty
    def nonEmpty: Boolean   = p.toFile.nonEmpty
    def canRead: Boolean    = p.toFile.canRead
    def canExecute: Boolean = p.toFile.canExecute

    private def fastCsv(delimiter: String): FastCsv = p.toFile.fastCsv(delimiter)

    def csvRows: Seq[Seq[String]] = FastCsv(p).rows

    def csvColnamesAndRows: (Seq[String], Seq[Seq[String]]) = csvRows.toList match {
    case cnames :: tail => (cnames, tail)
    case _              => (Nil, Nil)
    }
    def headingsAndRows: (Seq[String], Seq[Seq[String]]) = csvColnamesAndRows // alias

    def csvRows(delimiter: String): Seq[Seq[String]] = p.toFile.fastCsv(delimiter).rows

    def cksum: Long    = Util.cksum(p)
    def cksumNe: Long  = Util.cksumNe(p)
    def md5: String    = Util.fileChecksum(p, algorithm = "MD5")
    def sha256: String = Util.fileChecksum(p, algorithm = "SHA-256")

    /*
    // TODO: pareto standard csvRows, return main
    def csvMainRows: Seq[Seq[String]] = {
      // if only interested in rows with the most common column count
      vastblue.FastCsvParser.Stats(p).mainGroup.rowlist
    }
     */

    def delim: String           = toFile.guessDelimiter()
    def columnDelimiter: String = delim // alias
    def guessEncoding: String   = guessCharset.toString

    def guessCharset: Charset = {
      val (charset, _) = vastblue.file.Util.charsetAndContent(p)
      charset
    }
    def ageInDays: Double = vastblue.time.TimeDate.ageInDays(p.toFile)

    def diff(other: Path): Seq[String] = Util.diffExec(p.toFile, other.toFile)

  }

  implicit class ExtendFile(f: JFile) {
    def path                      = f.toPath
    def realfile: JFile           = path.realpath.toFile
    def name: String              = f.getName
    def lcname                    = f.getName.toLowerCase
    def norm: String              = f.path.norm
    def abspath: String           = norm
    def stdpath: String           = norm
    def relpath: Path             = path.relpath
    def posixpath: String         = stdpath
    def lastModifiedYMD: String   = f.path.lastModifiedYMD
    def basename: String          = dropDotSuffix(name)
    def lcbasename: String        = basename.toLowerCase
    def dotsuffix: String         = f.name.drop(f.basename.length) // .txt, etc.
    def suffix: String            = dotsuffix.dropWhile((c: Char) => c == '.')
    def lcsuffix: String          = suffix.toLowerCase
    def extension: Option[String] = f.dotsuffix match { case "" => None; case str => Some(str) }
    def parentFile: JFile         = f.getParentFile
    def parentPath: Path          = parentFile.toPath
    def parent: Path              = parentPath
    def isFile: Boolean           = f.isFile
//  def isRegularFile: Boolean    = isFile
    def filesTree: Seq[JFile] = {
      assert(f.isDirectory, s"not a directory [$f]")
      vastblue.file.Util.filesTree(f)()
    }
    def pathsTree: Seq[Path] = filesTree.map { _.path }
    def files: Seq[JFile] = {
      assert(f.isDirectory, s"not a directory [$f]")
      f.listFiles.toList
    }

    def lines: Seq[String] = readLines(f.toPath)

    def linesAnyEncoding: Seq[String] = Util.readLinesAnyEncoding(f.toPath)

    def fastCsv(delimiter: String): FastCsv = {
      val delim: String = if (delimiter.isEmpty) guessDelimiter() else delimiter
      FastCsv(f.path, delim)
    }
    def csvRows: Seq[Seq[String]] = FastCsv("").rows

    def guessDelimiter(count: Int = 50): String = {
      autoDetectDelimiter(lines.take(count).mkString("\n"), norm)
    }

    def renameTo(s: String): Boolean = renameTo(s.path)
    def renameTo(alt: Path): Boolean = {
      f.renameTo(alt.file)
    }

    def isEmpty: Boolean  = f.length == 0
    def nonEmpty: Boolean = f.length != 0

    def diff(other: JFile): Seq[String] = Util.diffExec(f, other)

    def sha256: String = Util.fileChecksum(f.toPath, algorithm = "SHA-256")
  }
}
