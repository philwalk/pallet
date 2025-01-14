//#!/usr/bin/env -S scala -explain
package vastblue.file

import vastblue.pallet.*
import vastblue.file.Util.*
import com.github.tototoshi.csv.*

import java.io.{FileNotFoundException, Reader, StringReader, File as JFile}
import java.nio.file.{Path, Files as JFiles, Paths as JPaths}
import scala.jdk.CollectionConverters.*

/**
* Csv Parser based on simpleflatmapper.
* (replaces SimpleCsv)
*/
object FastCsvToto {
  
  def apply(jfile: JFile, _delimiter: String): FastCsvToto = {
    apply(jfile.toPath, _delimiter)
  }

  def apply(p: Path, delimiter: String = ""): FastCsvToto = {
    if (!p.isFile) {
      throw new java.nio.file.NoSuchFileException(s"${p.posx}")
    }
    val lines         = readLines(p)
    def autoDelimiter = autoDetectDelimiter(lines.take(100).mkString("\n"), p.toString, ignoreErrors = false)
    val aDelimiter    = if (delimiter.nonEmpty) delimiter else autoDelimiter
    val str           = p.contentAsString
    val reader        = new StringReader(str)
    new FastCsvToto(reader, p.toString, aDelimiter)
  }

  def apply(content: String): FastCsvToto = {
    new FastCsvToto(new StringReader(content), s"${content.take(10)}...", ",")
  }

  // TODO: verify that this does not process more than the first line of the input String
  def parseLine(str: String): List[String] = parseCsvLine(str) // alias

  def parseCsvLine(str: String): List[String] = {
    parseCsvStream(str).toList match {
    case cols :: tail =>
      cols.toList
    case Nil =>
      Nil
    }
  }

  def parseCsvStream(str: String): Iterator[List[String]] = {
    apply(str).iterator.map { _.toList }
  }

  def parseFile(infile: Path): FastCsvToto = {
    FastCsvToto(infile, ",")
  }
//  def parseCsvFile(infile: Path): FastCsvToto = { // alias
//    parseFile(infile)
//  }

  /* will not quit on error unless override ignoreErrors = false */
  def autoDetectDelimiter(sampleText: String, fname: String, ignoreErrors: Boolean = true): String = {
    var (tabs, commas, semis, pipes) = (0, 0, 0, 0)
    sampleText.toCharArray.foreach {
      case '\t' => tabs += 1
      case ','  => commas += 1
      case ';'  => semis += 1
      case '|'  => pipes += 1
      case _    =>
    }
    // Premise:
    //   tab-delimited files contain more tabs than commas,
    //   comma-delimited files contain more commas than tabs.
    // Provides a reasonably fast guess, but can potentially fail.
    //
    // A much slower but more thorough approach would be:
    //    1. replaceAll("""(?m)"[^"]*", "") // remove quoted strings
    //    2. split("[\r\n]+") // extract multiple lines
    //    3. count columns-per-row tallies using various delimiters
    //    4. the tally with the most consistency is the "winner"
    (commas, tabs, pipes, semis) match {
      // in case of a tie between commas and tabs, commas win (TODO: configurable)
    case (cms, tbs, pps, sms) if cms >= tbs && cms >= pps && cms >= sms  => ","
    case (cms, tbs, pps, sms) if tbs >= cms && tbs >= pps && tbs >= sms => "\t"
    case (cms, tbs, pps, sms) if pps > cms && pps > tbs && pps > sms    => "|"
    case (cms, tbs, pps, sms) if sms > cms && sms > tbs && sms > pps    => ";"

    case _ if ignoreErrors => ""

    case _ =>
      sys.error(
        s"unable to choose delimiter: tabs[$tabs], commas[$commas], semis[$semis], pipes[$pipes] for file:\n[${fname}]"
      )
    }
  }
}

case class FastCsvToto(val reader: Reader, identifier: String, delimiter: String) {
  if (delimiter.length != 1) {
    System.err.printf("warning: only sees the first character of the delimiter [%s]\n", delimiter)
  }

  def delim: Char = delimiter match {
  case ""   => ' ' // treat rows with no delimiter as a single column
  case ","  => ','
  case "\t" => '\t'
  case "|"  => '|'
  case ";"  => ';'
  case _    => delimiter.charAt(0)
  }

  def rawrows: Seq[Seq[String]] = iterator.toSeq.filter { (cols: Seq[String]) => cols != Seq("") } // discard gratuitous empty rows
  def rows                      = rawrows.map { row => row.map(_.trim) }
  def rowstrimmed               = rows

  // def stream = CsvParser.separator(delim).iterator(reader).asScala.iterator
  override def toString = identifier
  
  import java.io.BufferedReader
  import scala.util.Using
  val br: BufferedReader = new BufferedReader(reader)

  inline def iterateLines: Iterator[String] = Iterator.continually(readLine).takeWhile { _ != null }

  class csvFormat extends CSVFormat {
    val delimiter: Char = delim
    val quoteChar: Char = '"'
    val escapeChar: Char = '"'
    val lineTerminator: String = "\n" // only used by tototoshi CSVWriter
    val quoting: Quoting = QUOTE_MINIMAL
    val treatEmptyLineAsNil: Boolean = false
  }

  lazy val csvParser = new CSVParser(new csvFormat)

  inline def iterator: Iterator[Seq[String]] = {
    for {
      line <- iterateLines
      // cols = CSVParser.parse(line, escapeChar, delimiterChar, quoteChar) match {
      colsopt = csvParser.parseLine(line)
      if colsopt != None
    } yield colsopt.get
  }

  inline def readLine: String = {
    val sb = new StringBuilder()
    var c: Int = 0
    def cc: Char = c.asInstanceOf[Char]
    def nonEOL: Boolean  = c != -1 && c != '\n' && c != '\u2028' && c != '\u2029' && c != '\u0085'

    while (nonEOL) {
      c = br.read()
      if (c != -1) {
        sb.append(cc)
        if (nonEOL) {
          if (c == '\r') {
            br.mark(1)
            c = br.read()
            if (c != -1) {
              if (c == '\n') {
                sb.append('\n')
              } else {
                br.reset()
              }
            }
          }
        }
      }
    }
    if (sb.isEmpty) {
      null
    } else { 
      sb.toString()
    }
  }
}
