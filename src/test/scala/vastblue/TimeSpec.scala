package vastblue

import vastblue.time.FileTime._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TimeSpec extends AnyFunSpec with Matchers { // with BeforeAndAfter {

  describe("time functions and lazy vals should initialize without throwing exceptions") {
    describe("NullDate") {
      it("should correctly initialize") {
        printf("NullDate:%s\n", NullDate)
        assert(true, "NullDate")
      }
    }
    describe("nowUTC show return differing timestamps after elapsed of time") {
      it("should correctly initialize") {
        val now1 = nowUTC
        printf("======= now1[%s]\n", now1)
        Thread.sleep(2000)
        val now2 = nowUTC
        printf("======= now2[%s]\n", now2)
        val elapsedSeconds = secondsBetween(now1, now2)
        printf("between %s and %s: %s Seconds elapsed\n", now1, now2, elapsedSeconds)
        val elapsedMinutes = minutesBetween(now1, now2)
        printf("between %s and %s: %s Minutes elapsed\n", now1, now2, elapsedMinutes)
        val elapsedHours = hoursBetween(now1, now2)
        printf("between %s and %s: %s Hours elapsed\n", now1, now2, elapsedHours)
        val elapsedDays = daysBetween(now1, now2)
        printf("between %s and %s: %s Days elapsed\n", now1, now2, elapsedDays)
        assert(true, "passed")
      }
    }
  }
}
