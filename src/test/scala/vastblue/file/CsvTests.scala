package vastblue.file

import vastblue.pallet.*
//import vastblue.file.Paths.*
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class CsvTests extends AnyFunSpec with Matchers with BeforeAndAfter {
  lazy val TMP = sys.props("java.io.tmpdir")

  @volatile lazy val csvTestFile = {
    val fnamestr = s"${TMP}/youMayDeleteThisDebrisCsvParser.csv"
    printf("csvTestFile[%s]\n", fnamestr)
    val path = Paths.get(fnamestr)
    if (path.parentPath.isDirectory) {
      path.withWriter() { w =>
        // format: off
        w.print(s"""
          |${headingRow.mkString(",")}
          |${row1.mkString(",")}
          |${row2.mkString(",")}
          |${row3.mkString(",")}
        """.stripMargin.trim
        )
        // format: on
      }
    }
    path
  }
  lazy val testFileA  = csvTestFile
  lazy val headingRow = List("One", "Two", "Three", "Four")
  lazy val row1       = List("0.123", "1.567", "2.901", "3.345")
  lazy val row2       = List("0", "1", "2", "3")
  lazy val row3       = List("0", "1", "2", "3", "4")

  before {}
  after {
    // if (testFileA.exists) testFileA.delete()
  }

  describe("CsvParser") {
    describe("#Stats") {
      var hook = 0
      // def parseCsvLine(line: String, columnTypes: String, delimiter: String = "") = {
      val testa = testFileA
      printf("testFileA[%s]\n", testFileA)
      if (!testa.exists) {
        // presumed to be legitimately missing
      } else {
        it("should deliver correct row values") {
          for ((row, idx) <- testFileA.csvRows.zipWithIndex) {
            idx match {
            case 0 => assert(row == headingRow)
            case 1 => assert(row == row1)
            case 2 => assert(row == row2)
            case 3 => assert(row == row3)
            case _ => fail(s"unexpected row: [${row}]")
            }
          }
        }
      }
    }
  }
}
