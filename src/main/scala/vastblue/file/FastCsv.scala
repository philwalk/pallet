//#!/usr/bin/env -S scala3
package vastblue.file

import vastblue.pallet.*
import vastblue.file.Util.*
import org.simpleflatmapper.csv.*

import java.io.{FileNotFoundException, Reader, StringReader, File as JFile}
import java.nio.file.{Path, Files as JFiles, Paths as JPaths}
import scala.jdk.CollectionConverters.*
import scala.collection.immutable.ArraySeq

/**
* Csv Parser based on simpleflatmapper.
* (replaces SimpleCsv)
*/
object FastCsv {

  def apply(jfile: JFile, _delimiter: String): FastCsv = {
    apply(jfile.toPath, _delimiter)
  }

  def apply(p: Path, delimiter: String = ""): FastCsv = {
    if (!p.isFile) {
      throw new java.nio.file.NoSuchFileException(s"${p.posx}")
    }
    val lines         = readLines(p)
    def autoDelimiter = autoDetectDelimiter(lines.take(100).mkString("\n"), p.toString, ignoreErrors = false)
    val aDelimiter    = if (delimiter.nonEmpty) delimiter else autoDelimiter
    val str           = p.contentAsString
    val reader        = new StringReader(str)
    new FastCsv(reader, p.toString, aDelimiter)
  }

  def apply(content: String): FastCsv = {
    new FastCsv(new StringReader(content), s"${content.take(10)}...", ",")
  }

  // TODO: verify that this does not process more than the first line of the input String
  def parseLine(str: String): ArraySeq[String] = parseCsvLine(str) // alias

  def parseCsvLine(str: String): ArraySeq[String] = {
    parseCsvStream(str) match {
    case iter if iter.hasNext =>
      iter.next()
    case _ =>
      ArraySeq.empty[String]
    }
  }

  def parseCsvStream(str: String): Iterator[ArraySeq[String]] = {
    val fastCsv = apply(str)
    fastCsv.iterator.map(identity)
  }

  def parseFile(infile: Path): FastCsv = {
    FastCsv(infile, ",")
  }
//  def parseCsvFile(infile: Path): FastCsv = { // alias
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

  def rawrows: Seq[Seq[String]] = iterator.toSeq.filter { (cols: ArraySeq[String]) => cols != Seq("") } // discard gratuitous empty rows
  def rows                      = rawrows.map { row => row.map(_.trim) }
  def rowstrimmed               = rows

  // def stream = CsvParser.separator(delim).iterator(reader).asScala.iterator
  override def toString = identifier
  
  import org.simpleflatmapper.csv.*
  def iterator: Iterator[ArraySeq[String]] = CsvParser.separator(delim).iterator(reader).asScala.map { ArraySeq.unsafeWrapArray(_) }
}
