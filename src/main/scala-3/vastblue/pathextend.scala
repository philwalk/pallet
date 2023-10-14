package vastblue

import java.nio.file.{Files => JFiles}
import java.nio.charset.Charset
import java.io.{ByteArrayInputStream, InputStream}
import java.io.{FileOutputStream, OutputStreamWriter}
import java.security.{DigestInputStream, MessageDigest}
import scala.jdk.CollectionConverters._
import vastblue.Platform._
import vastblue.DriveRoot._

// TODO: factor out code common to scala3 and scala2.13 versions
object pathextend {
  def Paths = vastblue.file.Paths
  type Path        = java.nio.file.Path
  type PrintWriter = java.io.PrintWriter
  type JFile       = java.io.File
  var hook                    = 0
  lazy val DefaultEncoding    = DefaultCodec.toString
  lazy val DefaultCharset     = Charset.forName(DefaultEncoding)
  lazy val Utf8               = Charset.forName("UTF-8")
  lazy val Latin1             = Charset.forName("ISO-8859-1")
  lazy val userHome           = sys.props("user.home").replace('\\', '/')
  lazy val userDir            = sys.props("user.dir").replace('\\', '/')
  lazy val ymd                = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  lazy val CygdrivePattern    = "/([a-z])(/.*)?".r
  lazy val DriveLetterPattern = "([a-z]):(/.*)?".r
  private def cwd: Path       = userDir.path.toAbsolutePath.normalize

  def scriptPathProperty: String = script.scriptPathProperty
  def sunCmdLine: Seq[String]    = script.sunCmdLine

  def scriptPath: Path      = script.scriptPath
  def scriptName: String    = script.scriptName
  def mainArgs: Seq[String] = script.mainArgs
  def progName: String      = script.progName
  def sunCmd: String        = script.sunCmd

  def fixHome(s: String): String = {
    s.startsWith("~") match {
    case false => s
    case true  => s.replaceFirst("~", userHome).replace('\\', '/')
    }
  }

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

    def realpath: Path = if (p.isSymbolicLink) {
      try {
        // p.toRealPath() // good symlinks
        JFiles.readSymbolicLink(p);
      } catch {
        case fse: java.nio.file.FileSystemException =>
          p.realpathLs // bad symlinks, or file access permission
      }
    } else {
      p // not a symlink
    }
    def getParentFile: JFile   = p.toFile.getParentFile
    def parentFile: JFile      = getParentFile
    def parentPath: Path       = parentFile.toPath
    def parent: Path           = parentPath
    def exists: Boolean        = JFiles.exists(p) // p.toFile.exists()
    def listFiles: Seq[JFile]  = p.toFile.listFiles.toList
    def localpath: String      = cygpath2driveletter(p.normalize.toString)
    def dospath: String        = localpath.replace('/', '\\')
    def isDirectory: Boolean   = p.toFile.isDirectory
    def isFile: Boolean        = p.toFile.isFile
    def isRegularFile: Boolean = isFile
    def relpath: Path          = if (p.norm.startsWith(cwd.norm)) cwd.relativize(p) else p
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
    def linesCharset(charset: Charset): Seq[String] = {
      if (p.norm.startsWith("/proc/")) {
        execBinary(catExe, p.norm)
      } else {
        try {
          JFiles.readAllLines(p, charset).asScala.toSeq
        } catch {
          case mie: java.nio.charset.MalformedInputException =>
            sys.error(s"malformed input reading file [$p] with charset [$charset]")
        }
      }
    }
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
    def norm: String = {
      val s1 = p.toString
      val s2 = s1 match {
      case "."  => s1
      case ".." => p.parentPath.normalize.toString
      case _    => p.normalize.toString
      }
      s2.replace('\\', '/') match {
      case CygdrivePattern(dr, p) if isWindows =>
        s"$dr:$p" // this can never happen, because cygdrive prefix never reproduced by Path.toString
      case DriveLetterPattern(dr, p) if isWindows =>
        s"$dr:$p" // not strictly needed, but useful in IDE
      case s =>
        s
      }
    }

    def abspath: String = norm

    // output string should be posix format, either because:
    //   A. non-Windows os
    //   B. C: matching default drive is dropped
    //   C. D: (not matching default drive) is converted to /d
    def stdpath: String = {
      // drop drive letter, if present
      val rawString = p.toString
      val posix = if (notWindows) {
        rawString // case A
      } else {
        val nm = norm
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
      pathextend.filesTree(f)()
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

  def aFile(s: String): Path            = Paths.get(s)
  def aFile(dir: Path, s: String): Path = Paths.get(s"$dir/$s")
  def _chmod(p: Path, permissions: String, allusers: Boolean): Boolean =
    Paths._chmod(p, permissions, allusers)

  def newEx: RuntimeException                      = new RuntimeException("LimitedStackTrace")
  def showLimitedStack(e: Throwable = newEx): Unit = { System.err.println(getLimitedStackTrace(e)) }
  def getLimitedStackTrace(implicit ee: Throwable = newEx): String = {
    getLimitedStackList.mkString("\n")
  }

  /** default filtering of stack trace removes known debris */
  def getLimitedStackList(implicit
      ee: Throwable = new RuntimeException("getLimitedStackTrace")
  ): List[String] = {
    currentStackList(ee).filter { entry =>
      entry.charAt(0) == ' ' || // keep lines starting with non-space
      (!entry.contains("at scala.")
        && !entry.contains("at oracle.")
        && !entry.contains("at org.")
        && !entry.contains("at codehaus.")
        && !entry.contains("at sun.")
        && !entry.contains("at java")
        && !entry.contains("at scalikejdbc")
        && !entry.contains("at net.")
        && !entry.contains("at dotty.")
        && !entry.toLowerCase.contains("(unknown source)"))
    }
  }
  def currentStack(ee: Throwable = new RuntimeException("currentStack")): String = {
    import java.io.{StringWriter}
    val result      = new StringWriter()
    val printWriter = new PrintWriter(result)
    ee.printStackTrace(printWriter)
    result.toString
  }
  def currentStackList(ee: Throwable = new RuntimeException("currentStackList")): List[String] = {
    currentStack(ee).split("[\r\n]+").toList
  }
  def notWindows: Boolean = java.io.File.separator == "/"
  def isWindows: Boolean  = !notWindows

  // set initial codec value, affecting default usage.
  import scala.io.Codec
  def writeCodec: Codec = {
    def osDefault = if (isWindows) {
      Codec.ISO8859
    } else {
      Codec.UTF8
    }
    val lcAll: String = Option(System.getenv("LC_ALL")).getOrElse(osDefault.toString)
    lcAll match {
    case "UTF-8" | "utf-8" | "en_US.UTF-8" | "en_US.utf8" =>
      Codec.UTF8 // "mac" | "linux"
    case s if s.toLowerCase.replaceAll("[^a-zA-Z0-9]", "").contains("utf8") =>
      Codec.UTF8 // "mac" | "linux"
    case "ISO-8859-1" | "latin1" =>
      Codec(lcAll)
    case encodingName =>
      // System.err.printf("warning : unrecognized charset encoding: LC_ALL==[%s]\n",encodingName)
      Codec(encodingName)
    }
  }
  lazy val DefaultCodec = writeCodec
  object JFile {
    def apply(dir: String, fname: String): JFile = new JFile(dir, fname)
    def apply(dir: JFile, fname: String): JFile  = new JFile(dir, fname)
    def apply(fpath: String): JFile              = new JFile(fpath)
  }

  def dummyFilter(f: JFile): Boolean = f.canRead()

  import scala.annotation.tailrec

  /**
   * Recursive list of all files below rootfile.
   *
   * Filter for directories to be descended and/or files to be retained.
   */
  def filesTree(dir: JFile)(func: JFile => Boolean = dummyFilter): Seq[JFile] = {
    assert(dir.isDirectory, s"error: not a directory [$dir]")
    @tailrec
    def filesTree(files: List[JFile], result: List[JFile]): List[JFile] = {
      files match {
      case Nil => result
      case head :: tail if Option(head).isEmpty =>
        Nil
      case head :: tail if head.isDirectory =>
        // filtered directories are pruned
        if (head.canRead()) {
          val subs: List[JFile] = head.listFiles.toList.filter { func(_) }
          filesTree(subs ::: tail, result) // depth-first
        } else {
          Nil
        }
      // filesTree(tail ::: subs, result) // width-first
      case head :: tail => // if head.isFile =>
        val newResult = func(head) match {
        case true  => head :: result // accepted
        case false => result         // rejected
        }
        filesTree(tail, newResult)
      }
    }
    filesTree(List(dir), Nil).toSeq
  }
  def autoDetectDelimiter(
      sampleText: String,
      fname: String,
      ignoreErrors: Boolean = true
  ): String = {
    var (tabs, commas, semis, pipes) = (0, 0, 0, 0)
    sampleText.toCharArray.foreach {
      case '\t' =>
        tabs += 1
      case ',' =>
        commas += 1
      case ';' =>
        semis += 1
      case '|' =>
        pipes += 1
      case _ =>
    }
    // This approach provides a reasonably fast guess, but sometimes fails:
    // Premise:
    //   tab-delimited files usually contain more tabs than commas,
    //   while comma-delimited files contain more commas than tabs.
    //
    // A much slower but more thorough approach would be:
    //    1. replaceAll("""(?m)"[^"]*","") // remove quoted strings
    //    2. split("[\r\n]+") // extract multiple lines
    //    3. count columns-per-row tallies using various delimiters
    //    4. the tally with the most consistency is the "winner"
    (commas, tabs, pipes, semis) match {
    case (cms, tbs, pps, sms) if cms > tbs && cms >= pps && cms >= sms =>
      ","
    case (cms, tbs, pps, sms) if tbs >= cms && tbs >= pps && tbs >= sms =>
      "\t"
    case (cms, tbs, pps, sms) if pps > cms && pps > tbs && pps > sms =>
      "|"
    case (cms, tbs, pps, sms) if sms > cms && sms > tbs && sms > pps =>
      ";"
    case _ if ignoreErrors =>
      ""
    case _ =>
      sys.error(
        s"unable to choose delimiter: tabs[$tabs], commas[$commas], semis[$semis], pipes[$pipes] for file:\n[${fname}]"
      )
    }
  }

  def toRealPath(p: Path): Path = {
    exec(realpathExe, p.norm).path
  }
  lazy val realpathExe = {
    val rp = where(s"realpath${exeSuffix}")
    rp
  }

  def getLinesAnyEncoding(p: Path, encoding: String = "utf-8"): Seq[String] = {
    getLinesIgnoreEncodingErrors(p, encoding).toSeq
  }
  def getLinesIgnoreEncodingErrors(p: Path, encoding: String = DefaultEncoding): Seq[String] = {
    import java.nio.charset.CodingErrorAction
    var discardWarningAbsorber = Codec(encoding)
    implicit val codec         = Codec(encoding)

    // scalafmt: { optIn.breakChainOnFirstMethodDot = true }
    discardWarningAbsorber = codec
      .onMalformedInput(CodingErrorAction.REPLACE)
      .onUnmappableCharacter(CodingErrorAction.REPLACE)
    try {
      JFiles.readAllLines(p, codec.charSet).asScala.toSeq
    } catch {
      case _: Exception =>
        encoding match {
        case "utf-8" =>
          implicit val codec = Codec("latin1")
          discardWarningAbsorber = codec
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
          JFiles.readAllLines(p, codec.charSet).asScala.toSeq
        case _ =>
          implicit val codec = Codec("utf-8")
          discardWarningAbsorber = codec
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
          JFiles.readAllLines(p, codec.charSet).asScala.toSeq
        }
    }
  }
//  def withPathWriter(p: Path, charsetName: String = DefaultEncoding, append: Boolean = false)(func: PrintWriter => Any): Unit = {
//    p.withWriter(charsetName, append)(func)
//  }

  import scala.util.matching.Regex
  lazy val DatePattern1: Regex = """(.+)(\d\d\d\d\d\d\d\d)(\D.*)?""".r
  lazy val DatePattern2: Regex = """(.+)(\d\d\d\d\d\d\d\d)""".r

  lazy val bintools = true // faster than MessageDigest
  def fileChecksum(file: JFile, algorithm: String): String = {
    val toolName = algorithm match {
    case "SHA-256" => "sha256sum"
    case "MD5"     => "md5sum"
    case _         => ""
    }
    val toolPath = where(toolName)
    val sum = if (bintools && !toolPath.isEmpty && toolPath.path.isFile) {
      // very fast
      val binstr = execBinary(toolPath, file.norm).take(1).mkString("")
      binstr.replaceAll(" .*", "")
    } else {
      // very slow
      val is = JFiles.newInputStream(file.path)
      checkSum(is, algorithm)
    }
    sum
  }
  lazy val PosixDriveLetterPrefix   = "(?i)/([a-z])(/.*)".r
  lazy val WindowsDriveLetterPrefix = "(?i)([a-z]):(/.*)".r

  def cygpath2driveletter(str: String): String = {
    val strtmp = str.replace('\\', '/')
    strtmp match {
    case PosixDriveLetterPrefix(dl, tail) =>
      val tailstr = Option(tail).getOrElse("/")
      s"$dl:$tailstr"
    case WindowsDriveLetterPrefix(dl, tail) =>
      val tailstr = Option(tail).getOrElse("/")
      s"$dl:$tailstr"
    case _ =>
      s"$workingDrive:$strtmp"
    }
  }
  def cygpath2driveletter(p: Path): String = {
    cygpath2driveletter(p.stdpath)
  }

  // return posix path string, with cygdrive prefix, if not the default drive
  def withPosixDriveLetter(str: String) = {
    if (notWindows) {
      str
    } else {
      val posix = if (str.drop(1).startsWith(":")) {
        val driveRoot = DriveRoot(str.take(2))
        str.drop(2) match {
        case "/" =>
          driveRoot.posix
        case pathstr if pathstr.startsWith("/") =>
          if (driveRoot == workingDrive) {
            pathstr // implicit drive prefix
          } else {
            s"${driveRoot.posix}$pathstr" // explicit drive prefix
          }
        case pathstr =>
          // Windows drive letter not followed by a slash resolves to
          // the "current working directory" for the drive.
          val cwd = driveRoot.workingDir.norm
          s"$cwd/$pathstr"
        }
      } else {
        // if str prefix matches workingDrive.posix, remove it
        if (!str.startsWith(cygdrive) || !workingDrive.isDrive) {
          str // no change
        } else {
          val prefixLength = cygdrive.length + 3 // "/cygdrive" + "/c/"
          if (str.take(prefixLength).equalsIgnoreCase(workingDrive.posix + "/")) {
            // drop working drive prefix, for implicit root-relative path
            str.drop(prefixLength - 1) // don't drop slash following workingDrive.posix prefix
          } else {
            str
          }
        }
      }
      posix
    }
  }

  def dropDotSuffix(s: String): String =
    if (!s.contains(".")) s else s.replaceFirst("[.][^.\\/]+$", "")

  def commonLines(f1: Path, f2: Path): Map[String, List[String]] = {
    val items = (f1.trimmedLines ++ f2.trimmedLines).groupBy { line =>
      line.replaceAll("""[^a-zA-Z_0-9]+""", "") // remove spaces and punctuation
    }
    items.map { case (key, items) => (key, items.toList) }
  }
  // supported algorithms: "MD5" and "SHA-256"
  def checkSum(bytes: Array[Byte], algorithm: String): String = {
    val is: InputStream = new ByteArrayInputStream(bytes)
    checkSum(is, algorithm)
  }
  def checkSum(is: InputStream, algorithm: String): String = {
    val md  = MessageDigest.getInstance(algorithm)
    val dis = new DigestInputStream(is, md)
    var num = 0
    while (dis.available > 0) {
      num += dis.read
    }
    dis.close
    val sum = md.digest.map(b => String.format("%02x", Byte.box(b))).mkString
    sum
  }
  def walkTree(file: JFile, depth: Int = 1, maxdepth: Int = -1): Iterable[JFile] = {
    val children = new Iterable[JFile] {
      def iterator = if (file.isDirectory) file.listFiles.iterator else Iterator.empty
    }
    Seq(file) ++ children.flatMap((f: JFile) =>
      if ((maxdepth < 0 || depth < maxdepth) && f.isDirectory) {
        walkTree(f, depth + 1, maxdepth)
      } else {
        Seq(f)
      }
    )
  }
}
