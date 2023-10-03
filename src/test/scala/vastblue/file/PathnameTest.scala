package vastblue.file

import vastblue.pathextend._
import vastblue.file.Paths._
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class PathnameTest extends AnyFunSpec with Matchers with BeforeAndAfter {
  val verbose = Option(System.getenv("VERBOSE_TESTS")).nonEmpty
  lazy val TMP = {
    val gdir = Paths.get("/g")
    gdir.isDirectory && gdir.paths.nonEmpty match {
      case true =>
        "/g/tmp"
      case false =>
        "/tmp"
    }
  }
  val testfilenames = Seq(
    s"${TMP}/_ÐÐ°Ð²ÐµÑÐ°Ð½Ð¸Ðµ&chapter=all",
    s"${TMP}/Canada's_Border.mp3"
    // ,s"${TMP}/ï"
    ,
    s"${TMP}/Canada&s_Border.mp3",
    s"${TMP}/Canada=s_Border.mp3",
    s"${TMP}/Canada!s_Border.mp3",
    s"${TMP}/philosophy&chapter=all",
    s"${TMP}/_2&chapter=all",
    s"${TMP}/_3&chapter=all"
  )

  describe("special-chars") {
    for (testfilename <- testfilenames) {
      it(s"should correctly handle filename [$testfilename] ") {
        val testfile = Paths.get(testfilename)
        val testPossible = testfile.parentFile match {
          case dir if dir.isDirectory =>
            true
          case _ =>
            false
        }
        if (!testPossible) {
          hook += 1
        } else {
          if (!testfile.exists) {
            // create dummy test file
            testfile.withWriter() { w =>
              w.printf("abc\n")
            }
          }
          printf("[%s]\n", testfile.stdpath)
        }
      }
    }
  }
}
