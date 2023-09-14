package vastblue

import java.nio.file.{Files => JFiles}
import java.nio.charset.Charset
import java.io.{ByteArrayInputStream, InputStream}
import java.io.{FileOutputStream, OutputStreamWriter}
import java.security.{DigestInputStream, MessageDigest}
import scala.jdk.CollectionConverters.*
import vastblue.Platform.*
import vastblue.time.FileTime
import vastblue.time.FileTime.*

object pathextend {
  def Paths = vastblue.file.Paths
  //def Files = vastblue.file.Files
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

  def scriptPath = Option(sys.props("script.path")) match {
    case None       => ""
    case Some(path) => path
  }

  def fixHome(s: String): String = {
    s.startsWith("~") match {
      case false => s
      case true  => s.replaceFirst("~", userHome).replace('\\', '/')
    }
  }

  implicit class ExtendString(s: String) {
    def path: Path         = vastblue.file.Paths.get(s)
    def toPath: Path       = path
    def absPath: Path      = s.path.toAbsolutePath.normalize // alias
    def toFile: JFile      = toPath.toFile
    def file: JFile        = toFile
    def norm: String       = s.replace('\\', '/')
    def dropSuffix: String = s.reverse.dropWhile(_ != '.').drop(1).reverse
  }

  implicit class ExtendPath(p: Path) {
    def toFile: JFile          = p.toFile
    def length: Long           = p.toFile.length
    def file: JFile            = p.toFile
    def realpath: Path         = if (p.isSymbolicLink) p.toRealPath() else p // toRealPath(p)
    def getParentFile: JFile   = p.toFile.getParentFile
    def parentFile: JFile      = getParentFile                               // alias
    def parentPath: Path       = parentFile.toPath
    def parent: Path           = parentPath                                  // alias
    def exists: Boolean        = JFiles.exists(p)                             // p.toFile.exists
    def listFiles: Seq[JFile]  = p.toFile.listFiles.toList
    def localpath: String      = cygpath2driveletter(p.normalize.toString)
    def dospath: String        = localpath.replace('/', '\\')
    def isDirectory: Boolean   = p.toFile.isDirectory
    def isFile: Boolean        = p.toFile.isFile
    def isRegularFile: Boolean = isFile                                      // alias
    def relpath: Path          = if (p.norm.startsWith(cwd.norm)) cwd.relativize(p) else p
    def relativePath: String   = relpath.norm
    def getName: String        = p.toFile.getName()
    def name: String           = p.toFile.getName                            // alias
    def lcname: String         = name.toLowerCase
    def basename: String       = p.toFile.basename
    def lcbasename: String     = basename.toLowerCase
    def suffix: String         = dotsuffix.dropWhile((c: Char) => c == '.')
    def lcsuffix: String       = suffix.toLowerCase
    def dotsuffix: String      = p.toFile.dotsuffix
    def noDrive: String =
      p.norm.replaceAll("^/?[A-Za-z]:?/", "/") // toss Windows drive letter, if present

    def text: String              = p.toFile.contentAsString // alias
    def extension: Option[String] = p.toFile.extension
    def pathFields                = p.iterator.asScala.toList
    def reversePath: String       = pathFields.reverse.mkString("/")
    def lastModified: Long        = p.toFile.lastModified
    def lastModifiedTime          = whenModified(p.toFile)
    def lastModSeconds: Double = {
      secondsBetween(lastModifiedTime, now).toDouble
    }
    def lastModMinutes: Double = lastModSeconds / 60.0
    def lastModHours: Double   = lastModMinutes / 60.0
    def lastModDays: Double    = round(lastModHours / 24.0)
    def weekDay: java.time.DayOfWeek = {
      p.lastModifiedTime.getDayOfWeek
    }
    def round(number: Double, scale: Int = 6): Double = {
      BigDecimal(number).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble
    }
    def age: String = { // readable description of lastModified
      if (lastModMinutes <= 60.0) {
        "%1.2f minutes".format(lastModMinutes)
      } else if (lastModHours <= 24.0) {
        "%1.2f hours".format(lastModHours)
      } else if (lastModDays <= 365.25) {
        "%1.2f days".format(lastModDays)
      } else {
        "%1.2f years".format(lastModDays / 365.25)
      }
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
        execBinary("cat", p.norm)
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
    def contentAsString: String                          = p.toFile.contentAsString()
    def contentWithEncoding(encoding: String): String = p.linesWithEncoding(encoding).mkString("\n")
    def contains(s: String): Boolean                  = p.toFile.contentAsString().contains(s)
    def contentAnyEncoding: String                    = p.toFile.contentAnyEncoding
    def bytes: Array[Byte]                            = JFiles.readAllBytes(p)
    def byteArray: Array[Byte]                        = bytes // alias
    def ageInDays: Double                             = FileTime.ageInDays(p.toFile)

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

    def abspath: String = norm // alias

    // output string should be posix format, either because:
    //   A. non-Windows os
    //   B. C: matching default drive is dropped
    //   C. D: (not matching default drive) is converted to /d
    def stdpath: String = { // alias
      // drop drive letter, if present
      val rawString = p.toString
      val posix = if (notWindows) {
        rawString // case A
      } else {
        val nm = norm
        posixDriveLetter(nm) // case C
      }
      posix
    }
    def posixpath: String = stdpath // alias
    def delete(): Boolean = toFile.delete()
    def withWriter(charsetName: String = DefaultEncoding, append: Boolean = false)(
        func: PrintWriter => Any
    ): Unit = {
      p.toFile.withWriter(charsetName, append)(func)
    }

    /*
    def overwrite(text: String): Unit = p.toFile.overwrite(text)
     */

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

  implicit class ExtendFile(f: JFile) {
    def path                      = f.toPath
    def realfile: JFile           = path.realpath.toFile
    def name: String              = f.getName                      // alias
    def lcname                    = f.getName.toLowerCase
    def norm: String              = f.path.norm
    def abspath: String           = norm                           // alias
    def stdpath: String           = norm                           // alias
    def posixpath: String         = stdpath                        // alias
    def lastModifiedYMD: String   = f.path.lastModifiedYMD
    def basename: String          = dropDotSuffix(name)
    def lcbasename: String        = basename.toLowerCase
    def dotsuffix: String         = f.name.drop(f.basename.length) // .txt, etc.
    def suffix: String            = dotsuffix.dropWhile((c: Char) => c == '.')
    def lcsuffix: String          = suffix.toLowerCase
    def extension: Option[String] = f.dotsuffix match { case "" => None; case str => Some(str) }
    def parentFile: JFile         = f.getParentFile
    def parentPath: Path          = parentFile.toPath
    def parent: Path              = parentPath                     // alias
    def isFile: Boolean           = f.isFile
    def isRegularFile: Boolean    = isFile                         // alias
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
    def byteArray: Array[Byte]     = bytes               // alias
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
//          new PrintWriter(new FileWriter(f, charset, append))
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

  /** Recursive list of all files below rootfile. Filter for directories to be descended and/or
    * files to be retained.
    */
  def dummyFilter(f: JFile): Boolean = f.canRead()

  import scala.annotation.tailrec
  def filesTree(dir: JFile)(func: JFile => Boolean = dummyFilter): Seq[JFile] = {
    assert(dir.isDirectory, s"error: not a directory [$dir]")
    @tailrec
    def filesTree(files: List[JFile], result: List[JFile]): List[JFile] = files match {
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
    val sum = if (bintools && toolPath.nonEmpty && toolPath.path.isFile) {
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
  // return a posix version of path string; include drive letter, if not the default drive
  def posixDriveLetter(str: String) = {
    val posix = if (str.drop(1).startsWith(":")) {
      val letter = str.take(1).toLowerCase
      val tail   = str.drop(2)
      tail match {
        case "/" =>
          s"/$letter"
        case s if s.startsWith("/") =>
          if (letter == workingDrive.take(1).toLowerCase) {
            tail
          } else {
            s"/$letter$tail"
          }
        case _ =>
          s"/$letter/$tail"
      }
    } else {
      if (workingDrive.nonEmpty && str.startsWith(s"/$workingDrive")) {
        str.drop(2) // drop default drive
      } else {
        str
      }
    }
    posix
  }
  def dropDotSuffix(s: String): String =
    if (!s.contains(".")) s else s.reverse.dropWhile(_ != '.').drop(1).reverse
  def commonLines(f1: Path, f2: Path): Map[String, List[String]] = {
    val items = (f1.trimmedLines ++ f2.trimmedLines).groupBy { line =>
      line.replaceAll("""[^a-zA-Z_0-9]+""", "") // remove whitespace and punctuation
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
}
