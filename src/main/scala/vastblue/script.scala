package vastblue

import vastblue.Platform._
import vastblue.file.Util.{Path}
import vastblue.pathextend._

object script {
  def stackHeadName: String = new Exception().getStackTrace.head.getFileName
  lazy val scriptPathProperty: String = {
    propOrElse("script.path", stackHeadName)
  }

  def mainProc: ProcInfo.Proc = ProcInfo.thisProc
  def scriptName: String      = ProcInfo.scriptName
  def scriptArgs: Seq[String] = ProcInfo.scriptArgs

  // TODO: this works if running from a script, but need to gracefully do something
  // otherwise.  If executing from a jar file, read manifest to get main class
  // else if running in an IDE, pretend that main class name is script name.
  lazy val scriptPath: Path = {
    vastblue.file.Paths.get(scriptName).toAbsolutePath
  }

  // scriptName, or legal fully qualified class name (must include package)
  def legalMainClass(s: String): Boolean = {
    val validScript             = s.norm.matches("[./a-zA-Z_0-9$]+")
    def validMainClass: Boolean = s.norm.matches("([a-zA-Z_$][a-zA-Z_$0-9]*[.]) {1,}[a-zA-Z_$0-9]+")
    val notMgr: Boolean         = s != "dotty.tools.MainGenericRunner"
    notMgr && (validScript || validMainClass)
  }

  def progName = scriptPath.name

  def stripPackagePrefix(fullname: String): String = fullname.replaceAll(""".*[^a-zA-Z_0-9]""", "")

  def fullClassname(ref: AnyRef): String = ref.getClass.getName.stripSuffix("$")
  def bareClassname(ref: AnyRef): String = stripPackagePrefix(fullClassname(ref))

  def scalaScriptFile: Path = scriptPath

  def scriptFile = scalaScriptFile

  private lazy val scriptFileDataTag = "/* _DATA_"

  def eprintln(text: String): Unit = {
    System.err.print(text)
  }
  def ePrintf(fmt: String, xs: Any*): Unit = {
    System.err.print(fmt.format(xs: _*))
  }
  def eprint(xs: Any*): Unit = {
    System.err.print("%s".format(xs: _*))
  }

  /**
   * Comparable to perl __DATA__ section.
   */
  def _DATA_(implicit encoding: String = "utf8"): List[String] = {
    scalaScriptFile.exists match {
    case true =>
      // var data = scalaScriptFile.getLinesIgnoreEncodingErrors(encoding).dropWhile( !_.startsWith(scriptFileDataTag) )
      // val charset = java.nio.charset.Charset.forName(encoding)
      var data = scalaScriptFile.lines(encoding).dropWhile(!_.startsWith(scriptFileDataTag)).toList
      if (data.isEmpty) {
        ePrintf("# no data section found (expecting start delimiter line: [%s]\n", scriptFileDataTag)
        List[String]() // no data section found
      } else {
        if (data.head.startsWith(scriptFileDataTag)) {
          data = data.tail
        }
        if (data.isEmpty) {
          List[String]() // empty data section
        } else {
          // val endQuote = """^\*/"""
          // toss last line, necessarily a trailing end-quote, e.g. "*/"
          data.init.toList // same as reverse.tail.reverse (discard last item)
        }
      }
    case false =>
      System.err.printf("warning: no script file [%s]\n", scalaScriptFile)
      List[String]() // no data section found
    }
  }
  def _DATA_columnRows(implicit delim: Option[String] = None): List[List[String]] = {
    script_DATA_columnRows(delim)
  }
  def script_DATA_columnRows(implicit splitDelimiter: Option[String] = None): List[List[String]] = {
    val data = _DATA_
    val delim = splitDelimiter match {
    case Some(d) => d
    case None    => """\t""" // default delimiter
    }
    // convert to List[List[String]]
    // negative limit preserves empty columns
    data.map(_.split(delim, -1).map { _.trim }.toList)
  }

  def getClassName(claz: Class[_]): String = {
    claz.getName.stripSuffix("$").replaceAll(""".*[^a-zA-Z_0-9]""", "") // delete thru the last non-identifier char
  }
  def getClassName(main: AnyRef): String = {
    getClassName(main.getClass)
  }
  lazy val thisClassName: String = getClassName(this)

  def eprintf(fmt: String, xs: Any*): Unit = {
    System.err.print(fmt.format(xs: _*))
  }

  lazy val verbose = Option(System.getenv("VERBY")).nonEmpty
}