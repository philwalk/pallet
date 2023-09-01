package vastblue

import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ParseDateSpec extends AnyFunSpec with Matchers with BeforeAndAfter {
//import vastblue.MDate
  @volatile lazy val datePairs:List[(String,String)] = List(
    ("01/16/15","2015/01/16"),
    ("01/25/15","2015/01/25"),
    ("01/26/15","2015/01/26"),
    ("01/27/15","2015/01/27"),
    ("01/29/15","2015/01/29"),
    ("03/02/15","2015/03/02"),
    ("03/05/15","2015/03/05"),
    ("03/20/15","2015/03/20"),
    ("04/01/15","2015/04/01"),
    ("04/09/15","2015/04/09"),
    ("06/05/15","2015/06/05"),
    ("06/11/15","2015/06/11"),
    ("06/26/15","2015/06/26"),
    ("07/14/15","2015/07/14"),
    ("07/16/15","2015/07/16"),
    ("07/19/15","2015/07/19"),
    ("08/04/15","2015/08/04"),
    ("08/06/15","2015/08/06"),
    ("08/18/15","2015/08/18"),
    ("08/23/15","2015/08/23"),
    ("08/31/15","2015/08/31"),
    ("09/08/15","2015/09/08"),
    ("10/21/15","2015/10/21"),
    ("10/25/15","2015/10/25"),
    ("11/14/15","2015/11/14"),
    ("11/15/15","2015/11/15"),
    ("11/16/15","2015/11/16"),
    ("11/17/15","2015/11/17"),
    ("11/22/15","2015/11/22"),
    ("11/23/15","2015/11/23"),
    ("11/29/15","2015/11/29"),
    ("12/08/15","2015/12/08"),
    ("12/11/15","2015/12/11"),
    ("12/12/15","2015/12/12"),
    ("12/13/15","2015/12/13"),
    ("12/14/15","2015/12/14"),
    ("12/25/15","2015/12/25"),
  )
  @volatile lazy val dateTimePairs:List[(String,String)] = List(
    ("Sat Oct 16 13:04:02 2021 -0600", "2021/10/16 13:04:02"),
  )
  describe("MDate") {
    describe ("parse()") {
      it("should correctly parse dateTime Strings") {
        var lnum = 0
        dateTimePairs.foreach { case (str,expected) =>
          import vastblue.time.FileTime.*
          val md = parseDateStr(str)
          val value:String = md.toString("yyyy/MM/dd HH:mm:ss")
//          printf("expected[%s], value[%s]\n",expected,value)
          assert(value == expected,s"line ${lnum}:\n  [$value]\n  [$expected]")
          lnum += 1
        }
      }
      it("should correctly parse date Strings") {
        var lnum = 0
        datePairs.foreach { case (str,expected) =>
          import vastblue.time.FileTime.*
          val md = parseDateStr(str)
          val value:String = md.toString("yyyy/MM/dd")
//          printf("expected[%s], value[%s]\n",expected,value)
          assert(value == expected,s"line ${lnum}:\n  [$value]\n  [$expected]")
          lnum += 1
        }
      }
    }
  }
}
