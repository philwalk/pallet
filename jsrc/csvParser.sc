#!/usr/bin/env -S scala

import org.simpleflatmapper.csv.*
import java.io.{FileReader, StringReader}
import scala.jdk.CollectionConverters.*

def main(args: Array[String]): Unit = {
  import vastblue.unifile.*

  var p = java.nio.file.Paths.get("testdates.csv")
  var content = p.contentAsString
  val reader = StringReader(content)
  def iterator: Iterator[Seq[String]] = CsvParser.separator(',').iterator(reader).asScala.map { _.toSeq }
  def rawrows: Seq[Seq[String]] = iterator.toSeq.filter { (cols: Seq[String]) => cols != Seq("") } // discard gratuitous empty rows
  def rows                      = rawrows.map { row => row.map(_.trim) }
  def rowstrimmed               = rows
  for (row <- rows){
    if (row.size > 2) {
      printf("%s: %s\n", row.size, row)
    }
  }

  /*
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
  */
}
