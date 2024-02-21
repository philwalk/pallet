package vastblue.time

import vastblue.time.TimeDate.*
import io.github.chronoscala.Imports.*
import io.github.chronoscala.*

import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.temporal.TemporalAdjusters
import java.time.* // {ZoneId, ZonedDateTime}
import java.time.format.*
import scala.language.implicitConversions

trait TimeExtensions {
  implicit def date2option(date: DateTime): Option[DateTime] = Some(date)

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
        sys.error(s"cannot convert to DateTime from: ${ta.getClass.getName}")
    }
  }
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
  implicit def int2richInt(i: Int): RichInt         = new RichInt(i)
  implicit def int2Period(i: Int): java.time.Period = java.time.Period.ofWeeks(i)

  implicit def sqlDate2LocalDateTime(sd: java.sql.Date): LocalDateTime = sd.toLocalDate.atStartOfDay()
  implicit def sqlDate2LocalDate(sd: java.sql.Date): LocalDate         = sd.toLocalDate

  import java.time.Duration
  extension (i: Interval) {
    def toDuration = i.duration
  }

  def between(d1: DateTime, d2: DateTime) = Duration.between(d1, d2)

  extension (zdt: ZonedDateTime) {
    def toDateTime: LocalDate = zdt.toLocalDate
  }
  extension (format: String) {
    def toDateTime = dateParser(format)
  }
  extension (ldt: java.time.LocalDateTime) {
    def atStartOfDay(): LocalDateTime             = ldt.withHour(0).withMinute(0).withSecond(0).withNano(0)
    def atStartOfDay(zone: ZoneId): ZonedDateTime = ldt.atStartOfDay().atZone(zone)
  }
  // format: off
  extension (d: java.time.DayOfWeek) {
    def >=(other: java.time.DayOfWeek) = { d.compareTo(other) >= 0 }
    def > (other: java.time.DayOfWeek) = { d.compareTo(other) >  0 }
    def <=(other: java.time.DayOfWeek) = { d.compareTo(other) <= 0 }
    def < (other: java.time.DayOfWeek) = { d.compareTo(other) <  0 }
  }
  // format: on

  extension (pd: java.time.Duration) {
    def getStandardSeconds: Long = pd.getSeconds.toLong
    def getStandardMinutes: Long = getStandardSeconds / 60
    def getStandardHours: Long   = getStandardMinutes / 60
    def getStandardDays: Long    = getStandardHours / 24
  }

  extension (d: DateTime) {
    def ymd: String = d.format(dateTimeFormatPattern(dateonlyFmt))

    def ymdhms: String = d.format(dateTimeFormatPattern(datetimeFmt7))

    def startsWith(str: String): Boolean = d.toString(ymdhms).startsWith(str)

    def fmt(fmt: String): String = {
      d.format(dateTimeFormatPattern(fmt))
    }
    def toString(fmt: String): String = {
      d.format(dateTimeFormatPattern(fmt))
    }
    def getMillis(): Long = {
      d.atZone(zoneid).toInstant().toEpochMilli()
    }
    def >(other: DateTime): Boolean = {
      d.compareTo(other) > 0
    }
    def >=(other: DateTime): Boolean = {
      d.compareTo(other) >= 0
    }
    def <(other: DateTime): Boolean = {
      d.compareTo(other) < 0
    }
    def <=(other: DateTime): Boolean = {
      d.compareTo(other) <= 0
    }
    def to(other: DateTime): Duration = {
      Duration.between(d, other)
    }
    def +(p: java.time.Period) = d.plus(p)
    def -(p: java.time.Period) = d.minus(p)

    def year: Int     = d.getYear
    def month: Month  = d.getMonth
    def monthNum: Int = d.getMonth.getValue
    def day: Int      = d.getDayOfMonth
    def hour: Int     = d.getHour
    def minute: Int   = d.getMinute
    def second: Int   = d.getSecond

    def setHour(h: Int): LocalDateTime   = d.plusHours((d.getHour + h).toLong)
    def setMinute(m: Int): LocalDateTime = d.plusMinutes((d.getMinute + m).toLong)

    def compare(that: DateTime): Int = d.getMillis() compare that.getMillis()
    def getDayOfYear: Int            = d.getDayOfYear
    def dayOfYear: Int               = d.getDayOfYear
    def dayOfMonth: Int              = d.getDayOfMonth
    def getDay: Int                  = d.getDayOfMonth
    def getDayOfMonth                = d.getDayOfMonth
    def dayOfWeek: DayOfWeek         = d.getDayOfWeek
    def getDayOfWeek: DayOfWeek      = d.getDayOfWeek

    def withDayOfWeek(dow: java.time.DayOfWeek): DateTime = d.`with`(TemporalAdjusters.next(dow))
    def lastDayOfMonth: LocalDateTime                     = d.`with`(LastDayAdjuster)
  }
}
