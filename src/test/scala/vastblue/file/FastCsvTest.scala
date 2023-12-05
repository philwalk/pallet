package vastblue.file

import vastblue.pallet.*
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class FastCsvTest extends AnyFunSpec with Matchers with BeforeAndAfter {
  lazy val TMP = {
    Paths.get("/g").isDirectory match {
    case true =>
      "/g/tmp"
    case false =>
      "/tmp"
    }
  }

  @volatile lazy val testCsv = new java.io.File(s"${TMP}/FastCsvTest01.csv")
  after {
    if (testCsv.exists) testCsv.delete()
  }

  describe("FastCsv") {
    describe("#parseCsvLine") {
      // def parseCsvLine(line: String, columnTypes: String, delimiter: String = "") = {
      it("should correctly parse various test lines") {
        for (test <- testItems) {
//        val rows = FastCsv.parseCsvLine(test.in, test.columnTypes).toList
          val rows = FastCsv.parseCsvLine(test.in).toList
          // System.err.printf("%s\n%s\n", result.getClass, test.out.getClass)
          if (rows != test.rows) {
            printf("rows[%s]\n", rows)
            printf("test.rows[%s]\n", test.rows)
          }
          assert(rows == test.rows)
        }
      }
    }

    describe("#parseFile") {
      it("should throw java.io.FileNotFoundException or java.nio.file.NoSuchFileException when appropriate") {
        // intercept[java.io.FileNotFoundException] {
        // intercept[java.nio.file.NoSuchFileException] {
        var success = false
        try {
          val nsf = Paths.get("/no/such/file")
          import vastblue.file.FastCsv
          FastCsv.parseFile(nsf)
        } catch {
          case _: java.io.FileNotFoundException =>
            success = true // success in scala 2.12
          case _: java.nio.file.NoSuchFileException =>
            success = true // success in scala 2.13
          case other: Throwable =>
            fail(s"unexpected exception [$other]")
        }
        if (!success) {
          fail("expecting fnf or nsf exception")
        }
      }
    }
  }

  case class TestItem(in: String, errors: Int, rows: Seq[String], columnTypes: String = "")
  def q = "\""
  @volatile lazy val testItems = Seq[TestItem](
    TestItem("a,b,c", 0, Seq("a", "b", "c"), ""),
    TestItem("1,2,3", 0, Seq("1", "2", "3"), ""),
//  TestItem("1,2,3", 0, Seq( 1 , 2 , 3 ), "iii"),
//  TestItem("1,2,3", 0, Seq("1", 2 , 3 ), "sii"),
//  TestItem("1,2,3", 0, Seq( 1 , "2", 3 ), "isi"),
//  TestItem("1,2,3", 0, Seq( 1 , 2 , "3"), "iis"),
//  TestItem(s"1,${q}${q}${q}2${q}${q}${q},3", 0, Seq( 1, s"${q}2${q}" ,3 ),"isi")
  )
}
