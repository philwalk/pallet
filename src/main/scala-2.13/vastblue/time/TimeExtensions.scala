package vastblue.time

import java.time.*
//import java.time.format.*
import java.time.temporal.TemporalAdjusters
import scala.runtime.RichInt

//import io.github.chronoscala.Imports.*
//import io.github.chronoscala.*
import vastblue.time.FileTime.*
import java.time.DayOfWeek
//import java.time.DayOfWeek.*
//import scala.language.implicitConversions

trait TimeExtensions {
  // implicit def ld2zdt(ld:LocalDate):ZonedDateTime = { ld.atStartOfDay.withZoneSameLocal(zoneid) }
  implicit def date2option(date: LocalDateTime): Option[LocalDateTime] = Some(date)

  implicit def ldt2zdt(ldt: LocalDateTime): ZonedDateTime = {
    ldt.atZone(UTC)
  }
  implicit def str2richStr(s: String): RichString = new RichString(s)
  implicit def ta2zdt(ta: java.time.temporal.TemporalAccessor): ZonedDateTime = {
    try {
      ta match {
        case ld: LocalDateTime =>
          ld.atZone(zoneid)
      }
    } catch {
      case _: java.time.DateTimeException =>
        sys.error(s"cannot convert to LocalDateTime from: ${ta.getClass.getName}")
    }
  }
  implicit def dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  // implicit def sqlDate2LocalDateTime(sd:java.sql.Date):LocalDateTime = sd.toLocalDate.atStartOfDay()
  // implicit def sqlDate2LocalDate(sd:java.sql.Date):LocalDate = sd.toLocalDate
  implicit def int2richInt(i: Int): RichInt         = new RichInt(i)
  implicit def int2Period(i: Int): java.time.Period = java.time.Period.ofWeeks(i)

  import java.time.Duration
  // implicit class aInterval(val i:Interval) { def toDuration = i.duration }
  /*
  implicit class aDayOfWeek(val d:java.time.DayOfWeek){
    def >=(other:java.time.DayOfWeek) = { d.compareTo(other) >= 0 }
    def > (other:java.time.DayOfWeek) = { d.compareTo(other) >  0 }
    def <=(other:java.time.DayOfWeek) = { d.compareTo(other) <= 0 }
    def < (other:java.time.DayOfWeek) = { d.compareTo(other) <  0 }
  }
  implicit class aDuration(val pd:java.time.Duration) {
    def getStandardSeconds:Long = pd.seconds
    def getStandardMinutes: Long = getStandardSeconds / 60
    def getStandardHours: Long = getStandardMinutes / 60
    def getStandardDays: Long = getStandardHours / 24
  }
   */
  def between(d1: LocalDateTime, d2: LocalDateTime) = Duration.between(d1, d2)

  implicit class aDateTime(val d: LocalDateTime) extends Ordered[aDateTime] {
    override def compare(that: aDateTime): Int = {
      val (a, b) = (getMillis(), that.getMillis())
      if (a < b) -1
      else if (a > b) +1
      else 0
    }
    def ymd: String = d.format(dateTimeFormatPattern(dateonlyFmt))

    def ymdhms: String = d.format(dateTimeFormatPattern(datetimeFmt7))

    def startsWith(str: String): Boolean = d.toString(ymdhms).startsWith(str)

    def toString(fmt: String): String = {
      d.format(dateTimeFormatPattern(fmt))
    }
    def getMillis(): Long = {
      d.atZone(zoneid).toInstant().toEpochMilli()
    }
    def >(other: LocalDateTime): Boolean = {
      d.compareTo(other) > 0
    }
    def >=(other: LocalDateTime): Boolean = {
      d.compareTo(other) >= 0
    }
    def to(other: LocalDateTime): Duration = {
      between(d, other)
    }
//    def to(other:LocalDateTime):Duration = {
//      Duration.between(d,other)
//    }
    def +(p: java.time.Period) = d.plus(p)
    def -(p: java.time.Period) = d.minus(p)

    def minute = d.getMinute
    def second = d.getSecond
    def hour   = d.getHour
    def day    = d.getDayOfMonth
    def month  = d.getMonth
    def year   = d.getYear

    def setHour(h: Int): LocalDateTime   = d.plusHours((d.getHour + h).toLong)
    def setMinute(m: Int): LocalDateTime = d.plusMinutes((d.getMinute + m).toLong)

    def compare(that: LocalDateTime): Int = d.getMillis() compare that.getMillis()
    def dayOfYear                         = d.getDayOfYear
    def getDayOfYear                      = d.getDayOfYear
    def dayOfMonth                        = d.getDayOfMonth
    def getDayOfMonth                     = d.getDayOfMonth
    def dayOfWeek: DayOfWeek              = d.getDayOfWeek // .getValue
    def getDayOfWeek: DayOfWeek           = d.getDayOfWeek // .getValue
    def withDayOfWeek(dow: java.time.DayOfWeek): LocalDateTime =
      d.`with`(TemporalAdjusters.next(dow))
    def lastDayOfMonth: LocalDateTime = d.`with`(LastDayAdjuster)
  }
}
