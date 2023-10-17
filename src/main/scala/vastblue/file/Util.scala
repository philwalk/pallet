package vastblue.file

import java.nio.file.{Files => JFiles}
import java.nio.charset.Charset
import java.io.{ByteArrayInputStream, InputStream}
import java.io.{FileOutputStream, OutputStreamWriter}
import java.security.{DigestInputStream, MessageDigest}
import scala.jdk.CollectionConverters._
import vastblue.Platform._
import vastblue.DriveRoot
import vastblue.DriveRoot._
import vastblue.script

// code common to scala3 and scala2.13 versions of pathextend
object Util {
  def Paths = vastblue.file.Paths

  type Path        = java.nio.file.Path
  type PrintWriter = java.io.PrintWriter
  type JFile       = java.io.File

  def scriptPathProperty: String = script.scriptPathProperty
  def sunCmdLine: Seq[String]    = script.sunCmdLine
  def scriptPath: Path           = script.scriptPath

  def scriptName: String    = script.scriptName
  def mainArgv: Seq[String] = script.mainArgv
  def progName: String      = script.progName
  def sunCmd: String        = script.sunCmd

  lazy val DefaultEncoding    = DefaultCodec.toString
  lazy val DefaultCharset     = Charset.forName(DefaultEncoding)
  lazy val Utf8               = Charset.forName("UTF-8")
  lazy val Latin1             = Charset.forName("ISO-8859-1")
  lazy val userHome           = sys.props("user.home").replace('\\', '/')
  lazy val userDir            = sys.props("user.dir").replace('\\', '/')
  lazy val ymd                = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  lazy val CygdrivePattern    = "/([a-z])(/.*)?".r
  lazy val DriveLetterPattern = "([a-z]):(/.*)?".r
  private def cwd: Path       = absPath(userDir)

  def absPath(s: String): Path = Paths.get(s).toAbsolutePath.normalize

  def fixHome(s: String): String = {
    s.startsWith("~") match {
    case false => s
    case true  => s.replaceFirst("~", userHome).replace('\\', '/')
    }
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
    val pnorm: String = nativePathString(p)
    val preal: String = exec(realpathExe, pnorm)
    Paths.get(preal)
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

  def toFile(s: String): JFile = {
    Paths.get(s).toFile
  }
  def isFile(s: String): Boolean = {
    toFile(s).isFile
  }
  def fileChecksum(file: JFile, algorithm: String): String = {
    val toolName = algorithm match {
    case "SHA-256" => "sha256sum"
    case "MD5"     => "md5sum"
    case _         => ""
    }
    val toolPath: String = where(toolName)

    val sum = if (bintools && !toolPath.isEmpty && isFile(toolPath)) {
      // very fast
      val fileNorm = nativePathString(file.toPath)
      val binstr   = execBinary(toolPath, fileNorm).take(1).mkString("")
      binstr.replaceAll(" .*", "")
    } else {
      // very slow
      val is = JFiles.newInputStream(file.toPath)
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
    cygpath2driveletter(p.toString)
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
          val cwd = driveRoot.workingDir.toString.replace('\\', '/')
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
  lazy val cwdnorm = cwd.toString.replace('\\', '/')

  def relativize(p: Path): Path = {
    val pnorm = nativePathString(p)
    if (pnorm == cwdnorm) {
      cwd
    } else if (pnorm.startsWith(cwdnorm)) {
      cwd.relativize(p)
    } else {
      p
    }
  }
  // if p == current working directory, toString might return any of the following:
  //  ""
  //  "."
  //  p.toAbsolutePath.toString
  def pathStr(p: Path): String = {
    p.toString match {
    case "" => "."
    case s  => s
    }
  }
  def nativePathString(p: Path): String = {
    val s1 = pathStr(p)
    val s2 = s1 match {
    case "."  => s1
    case ".." => p.getParent.normalize.toString
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

  def realpath(p: Path): Path = if (JFiles.isSymbolicLink(p)) {
    try {
      // p.toRealPath() // good symlinks
      JFiles.readSymbolicLink(p);
    } catch {
      case fse: java.nio.file.FileSystemException =>
        realpathLs(p) // bad symlinks, or file access permission
    }
  } else {
    p // not a symlink
  }
  def realpathLs(p: Path): Path = { // ask ls what symlink references
    val pnorm = nativePathString(p)
    exec("ls", "-l", pnorm).split("\\s+->\\s+").toList match {
    case a :: b :: Nil =>
      Paths.get(b)
    case _ =>
      p
    }
  }
}
