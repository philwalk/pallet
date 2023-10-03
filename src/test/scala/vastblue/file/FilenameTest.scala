package vastblue.file

import vastblue.pathextend._
import vastblue.file.Paths._
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class FilenameTest extends AnyFunSpec with Matchers with BeforeAndAfter {
  val verbose = Option(System.getenv("VERBOSE_TESTS")).nonEmpty
  describe("File.exists") {
    it("should correctly see whether a mapped dir (like W:/alltickers) exists or not") {
      val testdir = "w:/alltickers"
      val jf      = Paths.get(testdir)
      printf("jf.exists [%s]\n", jf.exists)
      // val bf = vastblue.Platform.getPath("/share","alltickers")
      // assert(bf.exists) // not yet ready from prime-time
    }
  }
}
