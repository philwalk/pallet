package vastblue.file

import vastblue.pathextend.*
import vastblue.file.Paths.*
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class FilenameTest extends AnyFunSpec with Matchers with BeforeAndAfter {
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
