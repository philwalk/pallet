package vastblue.file

import vastblue.pallet._
import vastblue.file.Paths._
import vastblue.Platform
import vastblue.file.Util

import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class PathnameTest extends AnyFunSpec with Matchers with BeforeAndAfter {
  val verbose = Option(System.getenv("VERBOSE_TESTS")).nonEmpty
  lazy val TMP = {
    val gdir = vastblue.pallet.Paths.get("/g")
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

  val testpath = "./bin"
  val testPaths = Seq(
    testpath.path.toString,
    Util.nativePathString(testpath.path),
    Util.nativePathString(testpath.path.relpath),
    testpath.path.relpath.norm,
    testpath.path.relativePath,
    testpath.path.stdpath,
    testpath.path.norm,
    testpath.path.localpath,
  )
  describe("file paths") {
    for ((str, i) <- testPaths.zipWithIndex) {
      it(s"path [$str] should be correct for os type [$osType] output index $i") {
        if (!isWindows) {
          if (str.contains(":")) {
            hook += 1
          }
          assert(!str.contains(":"))
        }
      }
    }
  }
  describe("special-chars") {
    for (testfilename <- testfilenames) {
      it(s"should correctly handle filename [$testfilename] ") {
        val testfile = vastblue.pallet.Paths.get(testfilename)
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
