//#!/usr/bin/env -S scala3
package vastblue.file

import vastblue.pallet.*
import vastblue.file.Util.*
import org.simpleflatmapper.csv.*

import java.io.{FileNotFoundException, Reader, StringReader, File as JFile}
import java.nio.file.{Path, Files as JFiles, Paths as JPaths}
import scala.jdk.CollectionConverters.*

/**
* Csv Parser based on simpleflatmapper.
* (replaces SimpleCsv)
*/
object FastCsv {

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
  def parseFile(infile: Path): FastCsv = {
    FastCsv(infile, ",")
  }
//  def parseCsvFile(infile: Path): FastCsv = { // alias
//    parseFile(infile)
//  }

  def apply(jfile: JFile, _delimiter: String): FastCsv = {
    apply(jfile.toPath, _delimiter)
  }
  def apply(p: Path, delimiter: String = ""): FastCsv = {
    if (!p.isFile) {
      throw new java.nio.file.NoSuchFileException(s"${p.norm}")
    }
    val lines         = readLines(p)
    def autoDelimiter = autoDetectDelimiter(lines.take(20).mkString("\n"), p.toString, ignoreErrors = false)
    val aDelimiter    = if (delimiter.nonEmpty) delimiter else autoDelimiter
    val str           = p.contentAsString
    val reader        = new StringReader(str)
    new FastCsv(reader, p.toString, aDelimiter)
  }
  def apply(content: String): FastCsv = {
    new FastCsv(new StringReader(content), s"${content.take(10)}...", ",")
  }

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
    case (cms, tbs, pps, sms) if cms > tbs && cms >= pps && cms >= sms  => ","
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

  /*
  def readLines(p: Path): Seq[String] = {
    try {
      JFiles.readAllLines(p).asScala.toSeq
    } catch {
      case t: Throwable =>
        sys.error(s"${p.norm}")
    }
  }
  def contentAsString(p: Path): String = {
    readLines(p).mkString("\n")
  }
   */
}

case class FastCsv(val reader: Reader, identifier: String, delimiter: String) {
  if (delimiter.length != 1) {
    System.err.printf("warning: will use only the first character delimiter [%s]\n", delimiter)
  }

  def delim: Char = delimiter match {
  case ""   => ' ' // treat rows with no delimiter as a single column
  case ","  => ','
  case "\t" => '\t'
  case "|"  => '|'
  case ";"  => ';'
  case _    => delimiter.charAt(0)
  }
  def iterator: Iterator[Seq[String]] = CsvParser.separator(delim).iterator(reader).asScala.map { _.toSeq }

  def rawrows: Seq[Seq[String]] = iterator.toSeq.filter { (cols: Seq[String]) => cols != Seq("") } // discard gratuitous empty rows
  def rows                      = rawrows.map { row => row.map(_.trim) }
  def rowstrimmed               = rows

  // def stream = CsvParser.separator(delim).iterator(reader).asScala.iterator
  override def toString = identifier
}
