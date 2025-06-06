#!/usr/bin/env -S scala-cli shebang

//> using jvm 17
//> using scala 3.6.4
//> using dep org.vastblue::pallet:0.11.0

import org.simpleflatmapper.csv.*
import java.io.{FileReader, StringReader}
import scala.jdk.CollectionConverters.*
import vastblue.pallet.*

var p = java.nio.file.Paths.get("testdates.csv")
var content = p.contentAsString
val reader = StringReader(content)
def iterator: Iterator[Seq[String]] = CsvParser.separator(',').iterator(reader).asScala.map { _.toSeq }
def rawrows: Seq[Seq[String]] = iterator.toSeq.filter { (cols: Seq[String]) => cols != Seq("") } // discard gratuitous empty rows
def rows                      = rawrows.map { row => row.map(_.trim) }
def rowstrimmed               = rows
for (row <- rows){
  printf("%s: %s\n", row.size, row.mkString(", "))
}
