package vastblue.file

import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import vastblue.file.Util
import java.nio.file.Paths

class UtilSpec extends AnyFunSpec with Matchers with BeforeAndAfter {
  val hasSuffix = Paths.get("basename.suffix")
  val noSuffix  = Paths.get("basename")
  val allSuffix = Paths.get(".basename")

  describe("vastblue.file.Util") {
    it("should correctly determine filename extension") {
      val str1 = Util.suffix(hasSuffix)
      assert(str1 == "suffix")
      val str2 = Util.suffix(noSuffix)
      assert(str2 == "")
      val str3 = Util.suffix(allSuffix)
      assert(str3 == "")

      val str4 = Util.dotsuffix(hasSuffix)
      assert(str4 == ".suffix")
      val str5 = Util.dotsuffix(noSuffix)
      assert(str5 == "")
      val str6 = Util.dotsuffix(allSuffix)
      assert(str6 == "")

      val str7 = Util.basename(hasSuffix) 
      assert(str7 == "basename")
      val str8 = Util.basename(noSuffix)
      assert(str8  == "basename")
      val str9 = Util.basename(allSuffix)
      assert(str9  == ".basename")

      val (base1, ext1) = Util.basenameAndExtension(hasSuffix) 
      assert((base1, ext1) == ("basename", "suffix"))

      val (base2, ext2) = Util.basenameAndExtension(noSuffix) 
      assert((base2, ext2) == ("basename", ""))

      val (base3, ext3) = Util.basenameAndExtension(allSuffix)
      assert((base3, ext3) == (".basename", ""))
    }
  }
}
