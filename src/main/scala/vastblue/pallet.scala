package vastblue

import vastblue.file.Util
import vastblue.file.Util.*
import vastblue.file.FastCsv
import vastblue.file.FastCsv.*
import vastblue.time.TimeDate.*
import vastblue.Platform.envPath

object pallet extends vastblue.util.PathExtensions {
  var hook: Int    = 0 // breakpoint hook
  type PrintWriter = java.io.PrintWriter
  type JFile       = java.io.File
  type Path        = java.nio.file.Path
  //def Paths        = vastblue.file.Paths
  def today        = now
  def yesterday    = now - 1.day

  def posixroot: String = vastblue.Platform.posixroot

  extension (p: Path) {
    def lastModifiedTime          = whenModified(p.toFile)
    def lastModSecondsDbl: Double = {
      secondsBetween(lastModifiedTime, now).toDouble
    }

    def weekDay: java.time.DayOfWeek = {
      p.lastModifiedTime.getDayOfWeek
    }

    def fastCsv(delimiter: String): FastCsv = p.toFile.fastCsv(delimiter)

    def csvRows: Seq[Seq[String]] = FastCsv(p).rows.dropWhile(_.mkString.trim.startsWith("#"))

    def csvColnamesAndRows: (Seq[String], Seq[Seq[String]]) = csvRows.toList match {
    case cnames :: tail => (cnames, tail)
    case _              => (Nil, Nil)
    }
    def headingsAndRows: (Seq[String], Seq[Seq[String]]) = csvColnamesAndRows // alias

    def csvRows(delimiter: String): Seq[Seq[String]] = p.toFile.fastCsv(delimiter).rows

    def delim: String           = p.toFile.guessDelimiter()
    def columnDelimiter: String = delim // alias

    def ageInDays: Double = vastblue.time.TimeDate.ageInDays(p.toFile)
  }

  extension (f: JFile) {
    def fastCsv(delimiter: String): FastCsv = {
      val delim: String = if (delimiter.isEmpty) guessDelimiter() else delimiter
      FastCsv(f.path, delim)
    }
    def csvRows: Seq[Seq[String]] = FastCsv("").rows

    def guessDelimiter(count: Int = 50): String = {
      autoDetectDelimiter(Util.readLines(f.toPath).take(count).mkString("\n"), f.abs)
    }
  }
}
