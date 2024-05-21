package vastblue

import vastblue.pallet.*
import vastblue.MainArgs.*
import vastblue.Unexpand.*
import java.nio.file.{Paths => JPaths}
import java.nio.file.{FileSystems, PathMatcher}

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ArgsTest extends AnyFunSpec with Matchers {
  var hook    = 0
  val verbose = Option(System.getenv("VERBOSE_TESTS")).nonEmpty
  val cp      = sys.props("java.class.path")
  describe("vastblue.MainArgs.prepArgs(args)") {
    for (triple <- testTriples) {
      it(s"should not expand glob args for ${triple}") {
        assert(triple.valid)
      }
    }
  }
  case class TestTriple(argv: IndexedSeq[String], argz: IndexedSeq[String], expected: Seq[String]) {
    val result = unexpandArgs(argv, argz)
    def valid = {
      if (result != expected) {
        hook += 1
      }
      result == expected
    }
  }

  // format: off
  lazy val testTriples = Seq(
    TestTriple(ix("a.sc", "tu"),          ix("a.sc", "tu"), ix("a.sc", "tu")),

    TestTriple(ix("*.sc", "tu"),          ix("*.sc", "tu"), ix("*.sc", "tu")),
    TestTriple(ix("a.sc", "tu"),          ix("*.sc", "tu"), ix("*.sc", "tu")),
    TestTriple(ix("a.sc", "b.sc", "tu"),  ix("*.sc", "tu"), ix("*.sc", "tu")),

    TestTriple(ix("*.sc", "t u"),         ix("*.sc", "t", "u"), ix("*.sc", "t u")),
    TestTriple(ix("a.sc", "t u"),         ix("*.sc", "t", "u"), ix("*.sc", "t u")),
    TestTriple(ix("x.sc", "t u"),         ix("*.sc", "t", "u"), ix("*.sc", "t u")),
    TestTriple(ix("a.sc", "b.sc", "t u"), ix("*.sc", "t", "u"), ix("*.sc", "t u")),

    TestTriple(ix("a.sc", "b.sc", "t u"), ix("*.sc", "t u"), ix("*.sc", "t u")),
    TestTriple(ix("m n", "a.sc", "b.sc", "t u"), ix("m", "n", "*.sc", "t u"), ix("m n", "*.sc", "t u")),
  )
  // format: on

  def ix(arg: String*): IndexedSeq[String] = {
    IndexedSeq(arg *)
  }
}
