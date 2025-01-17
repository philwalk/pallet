//#!/usr/bin/env -S scala -cp target/scala-3.4.3/classes
package vastblue.examples

import vastblue.pallet.*

object CsvWriteRead {
  def main(args: Array[String]): Unit = {
    var testFiles = args.filter { _.path.isFile }.toSeq
    if (testFiles.isEmpty) testFiles = Seq("tabTest.csv", "commaTest.csv")

    for (filename <- testFiles) {
      val testFile: Path = filename.toPath

      if (!testFile.exists) {
        // create tab-delimited and comma-delimited test files
        val delim: String = if (filename.startsWith("tab")) "\t" else ","
        testFile.withWriter() { w =>
          w.printf(s"1st${delim}2nd${delim}3rd\n")
          w.printf(s"A${delim}B${delim}C\n")
        }
      }

      assert(testFile.isFile)
      printf("\n# filename: %s\n", testFile.posx)
      // display file text lines
      for ((line: String, i: Int) <- testFile.lines.zipWithIndex) {
        printf("%d: %s\n", i, line)
      }
      // display file csv rows
      for (row: Seq[String] <- testFile.csvRows) {
        printf("%s\n", row.mkString("|"))
      }
    }
  }
}
