package vastblue

import java.nio.file.{Files => JFiles}
import java.nio.charset.Charset
import java.io.{ByteArrayInputStream, InputStream}
import java.io.{FileOutputStream, OutputStreamWriter}
import java.security.{DigestInputStream, MessageDigest}
import scala.jdk.CollectionConverters._
import vastblue.Platform.{isWindows, execBinary, catExe, exec}
import vastblue.DriveRoot
import vastblue.DriveRoot._
import vastblue.MainArgs
import vastblue.file.Util._

object pathextend {
  def Paths = vastblue.file.Paths

  type Path        = java.nio.file.Path
  type PrintWriter = java.io.PrintWriter
  type JFile       = java.io.File

  var hook: Int = 0 // breakpoint hook

  def osType: String = vastblue.Platform.osType

  def walkTree(file: JFile, depth: Int = 1, maxdepth: Int = -1): Iterable[JFile] = vastblue.file.Util.walkTree(file, depth, maxdepth)

  def filesTree(dir: JFile)(func: JFile => Boolean = dummyFilter): Seq[JFile] = vastblue.file.Util.filesTree(dir)(func)

  def showLimitedStack(e: Throwable = newEx): Unit = vastblue.file.Util.showLimitedStack(e)

  def scriptProp(e: Exception = new Exception()): String = script.scriptProp(e)

  def prepArgs(args: Seq[String]) = script.prepArgs(args)

  def scriptPath: Path        = script.scriptPath
  def scriptName: String      = script.scriptName
  def thisProc: MainArgs.Proc = script.thisProc

  // def progName: String        = script.progName

  extension (s: String) {
    def path: Path         = vastblue.file.Paths.get(s)
    def toPath: Path       = path
    def absPath: Path      = s.path.toAbsolutePath.normalize
    def toFile: JFile      = toPath.toFile
    def file: JFile        = toFile
    def norm: String       = s.replace('\\', '/')
    def dropSuffix: String = s.reverse.dropWhile(_ != '.').drop(1).reverse
  }

  extension (p: Path) {
    def toFile: JFile = p.toFile
    def length: Long  = p.toFile.length
    def file: JFile   = p.toFile

    def realpath: Path         = vastblue.file.Util.realpath(p)
    def getParentFile: JFile   = p.toFile.getParentFile
    def parentFile: JFile      = getParentFile
    def parentPath: Path       = parentFile.toPath
    def parent: Path           = parentPath
    def exists: Boolean        = JFiles.exists(p) // p.toFile.exists()
    def listFiles: Seq[JFile]  = p.toFile.listFiles.toList
    def localpath: String      = osType match {
      case "windows" =>
        cygpath2driveletter(p.normalize.toString)
      case _ =>
        p.toString
    }
    def dospath: String        = localpath.replace('/', '\\')
    def isDirectory: Boolean   = p.toFile.isDirectory
    def isFile: Boolean        = p.toFile.isFile
    def isRegularFile: Boolean = isFile
    def relpath: Path          = relativize(p)
    def relativePath: String   = relpath.norm
    def getName: String        = p.toFile.getName()
    def name: String           = p.toFile.getName
    def lcname: String         = name.toLowerCase
    def basename: String       = p.toFile.basename
    def lcbasename: String     = basename.toLowerCase
    def suffix: String         = dotsuffix.dropWhile((c: Char) => c == '.')
    def lcsuffix: String       = suffix.toLowerCase
    def dotsuffix: String      = p.toFile.dotsuffix

    // toss Windows drive letter, if present
    def noDrive: String = p.norm.replaceAll("^/?[A-Za-z]:?/", "/")

    def text: String              = p.toFile.contentAsString
    def extension: Option[String] = p.toFile.extension
    def pathFields                = p.iterator.asScala.toList
    def reversePath: String       = pathFields.reverse.mkString("/")
    def lastModified: Long        = p.toFile.lastModified
    def round(number: Double, scale: Int = 6): Double = {
      BigDecimal(number).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble
    }

    def files: Seq[JFile] = p.toFile match {
    case f if f.isDirectory => f.files
    case _                  => Nil
    }
    def paths: Seq[Path]                     = files.map(_.toPath)
    def dirs: Seq[Path]                      = paths.filter { _.isDirectory }
    def filesTree: Seq[JFile]                = p.toFile.filesTree
    def pathsTree: Seq[Path]                 = p.toFile.pathsTree
    def lines: Seq[String]                   = linesCharset(DefaultCharset)
    def lines(encoding: String): Seq[String] = linesCharset(Charset.forName(encoding))

    def linesCharset(charset: Charset): Seq[String] = vastblue.file.Util.linesCharset(p, charset)

    def linesAnyEncoding: Seq[String]                    = getLinesAnyEncoding(p)
    def linesWithEncoding(encoding: String): Seq[String] = getLinesAnyEncoding(p, encoding)
    def firstline                                        = p.linesAnyEncoding.take(1).mkString("")
    def getLinesIgnoreEncodingErrors(): Seq[String]      = linesAnyEncoding

    def contentAsString: String                       = p.toFile.contentAsString()
    def contentWithEncoding(encoding: String): String = p.linesWithEncoding(encoding).mkString("\n")
    def contains(s: String): Boolean                  = p.toFile.contentAsString().contains(s)
    def contentAnyEncoding: String                    = p.toFile.contentAnyEncoding
    def bytes: Array[Byte]                            = JFiles.readAllBytes(p)
    def byteArray: Array[Byte]                        = bytes

    def trimmedLines: Seq[String] = linesCharset(DefaultCharset).map { _.trim }
    def trimmedSql: Seq[String] =
      lines.map { _.replaceAll("\\s*--.*", "") }.filter { _.trim.length > 0 }

    def isSymbolicLink: Boolean = JFiles.isSymbolicLink(p)
    def mkdirs: Boolean = {
      val dir = JFiles.createDirectories(p)
      dir.toFile.isDirectory
    }
    def realpathLs: Path = { // ask ls what symlink references
      exec("ls", "-l", p.norm).split("\\s+->\\s+").toList match {
      case a :: b :: Nil => b.path
      case _             => p
      }
    }
    def lastModifiedYMD: String = {
      def lastModified = p.toFile.lastModified
      val date         = new java.util.Date(lastModified)
      ymd.format(date)
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
      p.toFile.withWriter(charsetName, append)(func)
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

    def renameTo(s: String): Boolean = renameTo(s.path)
    def renameTo(alt: Path): Boolean = {
      p.toFile.renameTo(alt)
    }
    def isEmpty: Boolean    = p.toFile.isEmpty
    def nonEmpty: Boolean   = p.toFile.nonEmpty
    def canRead: Boolean    = p.toFile.canRead
    def canExecute: Boolean = p.toFile.canExecute
  }

  extension (f: JFile) {
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
    def isRegularFile: Boolean    = isFile
    def filesTree: Seq[JFile] = {
      assert(f.isDirectory, s"not a directory [$f]")
      vastblue.file.Util.filesTree(f)()
    }
    def pathsTree: Seq[Path] = filesTree.map { _.path }
    def files: Seq[JFile] = {
      assert(f.isDirectory, s"not a directory [$f]")
      f.listFiles.toList
    }
    def contentAsString: String                                    = contentAsString(DefaultCharset)
    def contentAsString(charset: Charset = DefaultCharset): String = f.lines(charset).mkString("\n")

    def contentAnyEncoding: String = f.linesAnyEncoding.mkString("\n")
    def bytes: Array[Byte]         = f.getBytes("UTF-8") // JFiles.readAllBytes(path)
    def byteArray: Array[Byte]     = bytes
    def getBytes(encoding: String = "utf-8"): Array[Byte] =
      contentAsString.getBytes(Charset.forName(encoding))
    def lines: Seq[String]                   = lines(DefaultCharset)
    def lines(charset: Charset): Seq[String] = path.linesCharset(charset)
    def linesAnyEncoding: Seq[String]        = getLinesAnyEncoding(f.toPath)
    def contentWithEncoding(encoding: String): String =
      f.path.linesWithEncoding(encoding).mkString("\n")

    def withWriter(charsetName: String, append: Boolean)(func: PrintWriter => Any): Unit = {
      def lcname = name.toLowerCase
      if (lcname != "stdout") {
        Option(parentFile) match {
        case Some(parent) if parent.isDirectory =>
        // ok
        case Some(parent) =>
          throw new IllegalArgumentException(s"parent directory not found [${parent}]")
        case None =>
          throw new IllegalArgumentException(s"no parent directory")
        }
      }
      val writer = lcname match {
      case "stdout" =>
        new PrintWriter(new OutputStreamWriter(System.out, charsetName), true)
      case _ =>
        val charset = Charset.forName(charsetName)
        new PrintWriter(new OutputStreamWriter(new FileOutputStream(f, append), charset))
      }
      var junk: Any = 0
      try {
        junk = func(writer) // suppressWarnings:discarded-value
      } finally {
        writer.flush()
        if (lcname != "stdout") {
          // don't close stdout!
          writer.close()
        }
      }
    }

    def renameTo(s: String): Boolean = renameTo(s.path)
    def renameTo(alt: Path): Boolean = {
      f.renameTo(alt.file)
    }

    def isEmpty: Boolean  = f.length == 0
    def nonEmpty: Boolean = f.length != 0
  }
}
