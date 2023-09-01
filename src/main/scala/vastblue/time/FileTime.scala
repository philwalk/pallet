package vastblue.time

//import vastblue.pathextend.*

import java.util.concurrent.TimeUnit
import java.time.ZoneId
//import java.time.{ZoneOffset}
import java.time.format.*
import java.time.LocalDateTime
//import io.github.chronoscala.Imports.*

import java.time.temporal.{TemporalAdjuster, TemporalAdjusters}
//import java.time.temporal.{ChronoField}
import scala.util.matching.Regex
//import java.time.DayOfWeek
//import java.time.DayOfWeek.*

object FileTime extends vastblue.time.TimeExtensions {
  def zoneid = ZoneId.systemDefault
  def zoneOffset = zoneid.getRules().getStandardOffset(now.toInstant())
  def zoneOffsetHours = zoneOffset.getHour

//  support usage "DateTimeConstants.FRIDAY" 
  /*
  object DateTimeConstants {
    def SUNDAY = java.time.DayOfWeek.SUNDAY    
    def MONDAY = java.time.DayOfWeek.MONDAY    
    def TUESDAY = java.time.DayOfWeek.TUESDAY    
    def WEDNESDAY = java.time.DayOfWeek.WEDNESDAY    
    def THURSDAY = java.time.DayOfWeek.THURSDAY    
    def FRIDAY = java.time.DayOfWeek.FRIDAY    
    def SATURDAY = java.time.DayOfWeek.SATURDAY    
  }
  def SUNDAY = java.time.DayOfWeek.SUNDAY    
  def MONDAY = java.time.DayOfWeek.MONDAY    
  def TUESDAY = java.time.DayOfWeek.TUESDAY    
  def WEDNESDAY = java.time.DayOfWeek.WEDNESDAY    
  def THURSDAY = java.time.DayOfWeek.THURSDAY    
  def FRIDAY = java.time.DayOfWeek.FRIDAY    
  def SATURDAY = java.time.DayOfWeek.SATURDAY    
  */

  //type DateTimeFormat = DateTimeFormatter
  //type DateTimeZone = java.time.ZoneId
  //type LocalDate = java.time.LocalDate
  //type DateTime = LocalDateTime
//val DateTime = LocalDateTime

  /*
  def parseLocalDate(_datestr:String, offset: Int=0): DateTime = {
    parseDateJoda(_datestr, offset) // .toLocalDate
  }

  lazy val timeDebug:Boolean = Option(System.getenv("TIME_DEBUG")) match {
    case None => false
    case _ => true
  }
  */
  lazy val NullDate: LocalDateTime = LocalDateTime.parse("0000-01-01T00:00:00") // .ofInstant(Instant.ofEpochMilli(0))
 
  // Patterns permit but don't require time fields
  // Used to parse both date and time from column 1.
  // Permits but does not require column to be double-quoted.
  lazy val YMDColumnPattern: Regex = """[^#\d]?(2\d{3})[-/](\d{1,2})[-/](\d{1,2})(.*)""".r
  lazy val MDYColumnPattern: Regex = """[^#\d]?(\d{1,2})[-/](\d{1,2})[-/](2\d{3})(.*)""".r

  lazy val standardTimestampFormat = datetimeFmt6
  lazy val datetimeFmt8 = "yyyy-MM-dd HH:mm:ss-ss:S"
  lazy val datetimeFmt7 = "yyyy-MM-dd HH:mm:ss.S"
  lazy val datetimeFmt6 = "yyyy-MM-dd HH:mm:ss" // date-time-format
  lazy val datetimeFmt5 = "yyyy-MM-dd HH:mm" // 12-hour format
  lazy val datetimeFmt5b = "yyyy-MM-dd kk:mm" // 24-hour format
  lazy val dateonlyFmt = "yyyy-MM-dd" // date-only-format

  lazy val datetimeFormatter8: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt8)
  lazy val datetimeFormatter7: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt7)
  lazy val datetimeFormatter6: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt6)
  lazy val datetimeFormatter5: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt5)
  lazy val datetimeFormatter5b: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt5b)
  lazy val dateonlyFormatter: DateTimeFormatter = dateTimeFormatPattern(dateonlyFmt)
    
  lazy val EasternTime: ZoneId = java.time.ZoneId.of("America/New_York")
  lazy val MountainTime: ZoneId = java.time.ZoneId.of("America/Denver")
  lazy val UTC: ZoneId = java.time.ZoneId.of("UTC")

  def LastDayAdjuster: TemporalAdjuster = TemporalAdjusters.lastDayOfMonth()

  // ==============================

  def dateTimeFormatPattern(fmt:String,zone:ZoneId = ZoneId.systemDefault()):DateTimeFormatter = {
    val dtf1 = DateTimeFormatter.ofPattern(fmt).withZone(zone)
    val dtf = if( fmt.length <= "yyyy-mm-dd".length ){
      import java.time.temporal.ChronoField
      new DateTimeFormatterBuilder().append(dtf1)
      .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
      .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
      .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
      .toFormatter()
    } else {
      dtf1
    }
    dtf
  }

  /**
   * Get a diff between two dates
   * @param date1 the oldest date
   * @param date2 the newest date
   * @param timeUnit the unit in which you want the diff
   * @return the diff value, in the provided unit
   */
  def diffDays(date1: LocalDateTime, date2: LocalDateTime): Long = {
    diff(date1, date2, TimeUnit.DAYS)
  }
  def diffHours(date1: LocalDateTime, date2: LocalDateTime): Long = {
    diff(date1, date2, TimeUnit.HOURS)
  }
  def diffSeconds(date1: LocalDateTime, date2: LocalDateTime): Long = {
    diff(date1, date2, TimeUnit.SECONDS)
  }
  def diff(date1: LocalDateTime, date2: LocalDateTime, timeUnit: TimeUnit): Long = {
    val diffInMillies = date2.getMillis() - date1.getMillis()
    timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS)
  }

  // type LocalDateTime = LocalDateTime
  // signed number of days between specified dates.
  // if date1 > date2, a negative number of days is returned.
  def daysBetween(idate1:LocalDateTime, idate2:LocalDateTime): Long = {
    assert(idate1 != null,"idate1 is null")
    assert(idate2 != null,"idate2 is null")
    val elapsedDays:Long = if( idate1.getMillis() < idate2.getMillis() ){
      diffDays(idate1,idate2)
    } else {
      - diffDays(idate2,idate1)
    }
    elapsedDays
  }
  //private var hook = 0
  def secondsBetween(idate1:LocalDateTime,idate2:LocalDateTime): Long = {
    val seconds = diffSeconds(idate1,idate2) // .getStandardDays
    val elapsedSeconds:Long = if( idate1.getMillis() <= idate2.getMillis() ){
        seconds
    } else {
      - seconds // negative number
    }
    elapsedSeconds
  }
  def secondsSince(date1:LocalDateTime): Long = secondsBetween(date1,now)

  def endOfMonth(d: LocalDateTime): LocalDateTime = {
    val month: java.time.YearMonth = { java.time.YearMonth.from(d) }
    month.atEndOfMonth.atStartOfDay
  }

  def minutesBetween(date1:LocalDateTime,date2:LocalDateTime):Double = {
    secondsBetween(date1,date2).toDouble / 60.0
  }
  def minutesSince(date1:LocalDateTime): Double = minutesBetween(date1,now)

  def hoursBetween(date1:LocalDateTime,date2:LocalDateTime):Double = {
    minutesBetween(date1,date2) / 60.0
  }
  def hoursSince(date1:LocalDateTime): Double = hoursBetween(date1,now)

  def whenModified(f:java.io.File):LocalDateTime = {
    val lastmod = if (f.exists) f.lastModified else -1
    epoch2DateTime(lastmod, MountainTime)
  }

  def epoch2DateTime(epoch:Long,timezone:java.time.ZoneId=UTC):LocalDateTime = {
    val instant = java.time.Instant.ofEpochMilli(epoch)
    java.time.LocalDateTime.ofInstant(instant,timezone)
  }

  /**
  * Returns days, hours, minutes, seconds between timestamps.
  */
//  def getDuration(date1:LocalDateTime,date2:LocalDateTime): (Long, Long, Long, Long) = {
//    val reverse = date1.getMillis() > date2.getMillis()
//    val (d1,d2) = reverse match {
//    case true => (date2,date1)
//    case _    => (date1,date2)
//val d2d = idate1 to idate2 // new RichDuration(duration)
//val d2d = new RichDuration(between(idate1, idate2))
//    }
//    val duration = diffDays(d1,d2) // .toDuration
//    val days = duration.getStandardDays
//
//    var (hours:Long, minutes:Long, seconds:Long) = (
//      duration.getStandardHours,
//      duration.getStandardMinutes,
//      duration.getStandardSeconds
//    )
//    if( minutes > 0 ){
//      seconds -= minutes*60
//    }
//    if( hours > 0 ){
//      minutes -= hours*60
//    }
//    if( days > 0 ){
//      hours -= days * 24
//    }
//    (days,hours,minutes,seconds)
//  }

  def nowZoned(zone:ZoneId = MountainTime): LocalDateTime = LocalDateTime.now(zone)
  lazy val now: LocalDateTime = nowZoned(MountainTime)
  def nowUTC = LocalDateTime.now()

  //def fixDateFormat = vastblue.time.Time.fixDateFormat _
  //def ageInMinutes  = vastblue.time.Time.ageInMinutes _
  def ageInMinutes(f:java.io.File):Double = {
    if( f.exists ){
      val diff = (now.getMillis() - f.lastModified) / (60 * 1000).toDouble
      diff
    } else {
      1e6 // missing files are VERY stale
    }
  }
  def ageInDays(f:java.io.File):Double = {
    ageInMinutes(f) / (24 * 60)
  }
  def ageInDays(fname:String):Double = {
    ageInDays(new java.io.File(fname))
  }

  def parse(str: String,format:String): LocalDateTime = {
//    if( timeDebug ) System.err.print("parse(str=[%s], format=[%s]\n".format(str,format))
    if( format.length <= "yyyy-mm-dd".length ){
      LocalDateTime.parse(str,dateTimeFormatPattern(format))
    } else {
      LocalDateTime.parse(str,dateTimeFormatPattern(format))
    }
  }
  /** The new parser does not depend on MDate */
  def parseDateNew(_datestr:String,format:String=""):LocalDateTime = {
    val datestr = _datestr.
      replaceAll("/","-"). // normalize field separator
      replaceAll("\"",""). // remove quotes
      replaceAll(""" (\d):"""," 0$1:"). // make sure all time fields are 2 digits (zero filled)
      replaceAll("\\s+"," ").trim // compress random whitespace to a single space, then trim

    val pattern = (format != "",datestr.contains(":"),datestr.matches(""".* (AM|PM)\b.*"""),datestr.contains(".")) match {
      case (true,_,_,_)     => format // user-specified format
      case (_,false,_,_)    => "yyyy-MM-dd"
      case (_,true,false,false) => "yyyy-MM-dd HH:mm:ss"
      case (_,true, true,false) => "yyyy-MM-dd hh:mm:ss aa"
      case (_,true,false, true) => "yyyy-MM-dd HH:mm:ss.SSS"
      case (_,true, true, true) => "yyyy-MM-dd hh:mm:ss aa.SSS"
    }
    try {
      parse(datestr,pattern)
    } catch {
    case e:IllegalArgumentException =>
      e.getMessage.contains("Illegal instant due to time zone offset") match {
      case true =>
        throw e
      case false =>
        parse(datestr,pattern)
      }
    }
  }

  def parseDateTime(str:String):LocalDateTime = parseDateStr(str)
  lazy val ThreeIntegerFields1 = """(\d{2,4})\D(\d{1,2})\D(\d{1,2})""".r
  lazy val ThreeIntegerFields3 = """(\d{1,2})\D(\d{1,2})\D(\d{2,4})""".r
  lazy val ThreeIntegerFields2 = """(\d{2,2})\D(\d{1,2})\D(\d{1,2})""".r
  def parseDateStr(_inputdatestr: String): LocalDateTime = { // , offset: Int=0):LocalDateTime = {
    val _datestr = _inputdatestr.trim.replaceAll("\"","")
    if( _datestr.isEmpty ){
      BadDate
    } else {
      _datestr match {
        case ThreeIntegerFields1(_y,_m,_d) =>
          if (_y.length > 2){
            val (y, m, d) = (_y.toInt, _m.toInt, _d.toInt)
            new RichString("%4d-%02d-%02d".format(y,m,d)).toDateTime
          } else {
            val nums = List(_y, _m, _d).map { _.toInt }
            val possibleDays = nums.zipWithIndex.filter { case (n, i) => n <= 31 }
            val possibleMonths = nums.zipWithIndex.filter { case (n, i) => n <= 12 }
            val (y, m, d) = possibleMonths match {
              case (n,0) :: list =>
                (nums(2), nums(0), nums(1)) // m/d/y
              case (n,1) :: list =>
                (nums(2), nums(1), nums(0)) // d/m/y
              case _ => possibleDays match {
                case (n,0) :: list => (nums(2), nums(1), nums(0)) // d/m/y
                case _ =>
                (nums(2), nums(0), nums(1))  // m/d/y
              }
            }
            val year = if (y >= 1000) y else y+2000
            new RichString("%4d-%02d-%02d".format(year,m,d)).toDateTime
          }
        case ThreeIntegerFields3(_m, _d, _y) =>
          val (y, m, d) = (_y.toInt, _m.toInt, _d.toInt)
          val year = if (y >= 1000) y else y+2000
          new RichString("%4d-%02d-%02d".format(year,m,d)).toDateTime
        case ThreeIntegerFields2(_x, _y, _z) =>
          val nums = List(_x, _y, _z).map { _.toInt }
       // val possibleDays = nums.zipWithIndex.filter { case (n,i) => n <= 31 }
       // val possibleMonths = nums.zipWithIndex.filter { case (n,i) => n <= 12 }
          val List(y, m, d) = nums
          val year = if (y >= 1000) y else y+2000
          new RichString("%4d-%02d-%02d".format(year,m,d)).toDateTime
        case _ =>
        // next, treat yyyyMMdd (8 digits, no field separators)
        if (_datestr.matches("""2\d{7}""")) {
          new RichString(_datestr.replaceAll("(....)(..)(..)", "$1-$2-$3")).toDateTime

        } else if (_datestr.matches("""\d{2}\D\d{2}\D\d{2}""")) {
          // MM-dd-yy
          val fixed = _datestr.split("\\D").toList match {
            case m :: d :: y :: Nil =>
              "%04d-%02d-%02d 00:00:00".format(2000 + y.toInt, m.toInt, d.toInt)
            case _ =>
              _datestr // no fix
          }
          //      printf("%s\n",datetimeFormatter.getClass)
          //      datetimeFormatter6.parse(fixed)
          LocalDateTime.parse(fixed, datetimeFormatter6)
        } else if (_datestr.matches("""2\d{3}\D\d{2}\D\d{2}\.\d{4}""")) {
          // yyyy-MM-dd.HHMM

          val fixed = _datestr.replaceAll("""(....)\D(..)\D(..)\.(\d\d)(\d\d)""", "$1-$2-$3 $4:$5:00")
          LocalDateTime.parse(fixed, datetimeFormatter6)
        } else {
          val datestr = _datestr.replaceAll("/", "-")
          parseDateString(datestr)
          /*
          try {
          } catch {
          case e: Exception =>
            if (vastblue.MDate.debug) System.err.printf("e[%s]\n",e.getMessage)
            val mdate = vastblue.MDate.parseDate(datestr) // .replaceAll("\\D+",""))
            //    val timestamp = new LocalDateTime(mdate.getEpoch)
            val standardFormat = mdate.toString(standardTimestampFormat)
            val timestamp = standardFormat.toDateTime
            val hour = timestamp.getHour // hourOfDay.get
            val extraHours = if (datestr.contains(" PM") && hour < 12) {
              12
            } else {
              0
            }
            val hours = (offset + extraHours).toLong
            timestamp.plusHours(hours)
          }
          */
        }
      }
    }
  }

  def standardTime(datestr: String): String = { // }, offset: Int=0): String = {
  //parseDateStr(datestr, offset).toString(standardTimestampFormat)
    parseDateStr(datestr).toString(standardTimestampFormat)
  }
  def parseDate(datestr: String): LocalDateTime = { // }, offset: Int=0): LocalDateTime = {
    parseDateStr(datestr) // , offset)
  }

  def getDaysElapsed(idate1:LocalDateTime, idate2:LocalDateTime): Long = {
    if( idate2.getMillis() < idate1.getMillis() ){
    //- (idate2 to idate1).getStandardDays
      - diffDays(idate2,idate1)
    } else {
    //(idate1 to idate2).getStandardDays
        diffDays(idate1,idate2)
    }
  }
  def getDaysElapsed(datestr1:String,datestr2:String):Long = {
    getDaysElapsed(parseDateStr(datestr1),parseDateStr(datestr2))
  }
  def selectZonedFormat(_datestr:String):java.time.format.DateTimeFormatter = {
    val datestr = _datestr.replaceAll("/","-")
    val numfields = datestr.split("\\D+")
    numfields.length <= 3 match {
      case true =>
        dateonlyFormatter
      case false =>
        datetimeFormatter6
    }
  }
  def ti(s:String): Int = {
    s match {
    case n if n.matches("0\\d+") =>
      n.replaceAll("0+(.)","$1").toInt
    case n =>
      n.toInt
    }
  }
  def numerifyNames(datestr:String) = {
    val noweekdayName = datestr.replaceAll("(Sun[day]*|Mon[day]*|Tue[sday]*|Wed[nesday]*|Thu[rsday]*|Fri[day]*|Sat[urday]*),? *","")
    noweekdayName match {
      case str if str.matches("(?i).*[JFMASOND][aerpuco][nbrylgptvc][a-z]*.*") =>
        var ff = str.split("[,\\s]+")
        if (ff(0).matches("\\d+")){
          // swap 1st and 2nd fields (e.g., convert "01 Jan" to "Jan 01")
          val tmp = ff(0)
          ff(0) = ff(1)
          ff(1) = tmp
        }
        val month = monthAbbrev2Number(ff.head.take(3))
        ff = ff.drop(1)
        val (day, year, timestr, tz) = ff.toList match {
          case d :: y :: Nil =>
            (d.toInt, y.toInt, "", "")
          case d :: y :: ts :: tz :: Nil if ts.contains(":") =>
            (d.toInt, y.toInt, " "+ts, " "+tz)
          case d :: ts :: y :: tail if ts.contains(":") =>
            (d.toInt, y.toInt, " "+ts, "")
          case d :: y :: ts :: tail =>
            (d.toInt, y.toInt, " "+ts, "")
          case other =>
            sys.error(s"bad date [$other]")
        }
        "%4d-%02d-%02d%s%s".format(year,month,day,timestr,tz)
      case str =>
        str
    }
  }
  def monthAbbrev2Number(name:String):Int = {
    name.toLowerCase.substring(0,3) match {
      case "jan" => 1
      case "feb" => 2
      case "mar" => 3
      case "apr" => 4
      case "may" => 5
      case "jun" => 6
      case "jul" => 7
      case "aug" => 8
      case "sep" => 9
      case "oct" => 10
      case "nov" => 11
      case "dec" => 12
    }
  }

  lazy val mmddyyyyPattern: Regex = """(\d{1,2})\D(\d{1,2})\D(\d{4})""".r
  lazy val mmddyyyyTimePattern: Regex = """(\d{1,2})\D(\d{1,2})\D(\d{4})(\D\d\d:\d\d(:\d\d)?)""".r
  lazy val mmddyyyyTimePattern2: Regex = """(\d{1,2})\D(\d{1,2})\D(\d{4})\D(\d\d):(\d\d)""".r
  lazy val mmddyyyyTimePattern3: Regex = """(\d{1,2})\D(\d{1,2})\D(\d{4})\D(\d\d):(\d\d):(\d\d)""".r
  lazy val mmddyyyyTimePattern3tz: Regex = """(\d{1,2})\D(\d{1,2})\D(\d{4})\D(\d\d):(\d\d):(\d\d)\D(-?[0-9]{4})""".r
  lazy val yyyymmddPattern: Regex = """(\d{4})\D(\d{1,2})\D(\d{1,2})""".r
  lazy val yyyymmddPatternWithTime: Regex = """(\d{4})\D(\d{1,2})\D(\d{1,2})(\D.+)""".r
  lazy val yyyymmddPatternWithTime2: Regex = """(\d{4})\D(\d{1,2})\D(\d{1,2}) +(\d{2}):(\d{2})""".r
  lazy val yyyymmddPatternWithTime3: Regex = """(\d{4})\D(\d{1,2})\D(\d{1,2})\D(\d{2}):(\d{2}):(\d{2})""".r
  lazy val validYearPattern = """(1|2)\d{3}""" // only consider years between 1000 and 2999
  def parseDateString(_datestr:String): LocalDateTime = {
    var datestr = _datestr.
      replaceAll("/","-").
      replaceAll("#","").
      replaceAll("-[0-9]+:[0-9]+$","").
      replaceAll("([0-9])T([0-9])","$1 $2").trim
    datestr = datestr match {
      case mmddyyyyPattern(m,d,y) =>
        "%s-%02d-%02d".format(y,ti(m),ti(d))
      case mmddyyyyTimePattern(m,d,y,t) =>
        "%s-%02d-%02d%s".format(y,ti(m),ti(d),t)
      case mmddyyyyTimePattern2(m,d,y,h,min) if y.matches(validYearPattern) =>
        "%s-%02d-%02d %02d:%02d".format(y,ti(m),h,min)
      case mmddyyyyTimePattern3(m, d, y, h, min, s) if y.matches(validYearPattern) =>
        "%s-%02d-%02d %02d:%02d:02d".format(y, ti(m), h, min, s)
      case mmddyyyyTimePattern3tz(m, d, y, h, min, s, tz) if y.matches(validYearPattern) =>
        "%s-%02d-%02d %02d:%02d:02d %s".format(y, ti(m), h, min, s)
      case yyyymmddPattern(y,m,d) if y.matches(validYearPattern) =>
        "%s-%02d-%02d".format(y,ti(m),ti(d))
      case yyyymmddPatternWithTime(y,m,d,t) if y.matches(validYearPattern) =>
        "%s-%02d-%02d%s".format(y,ti(m),ti(d),t)
      case yyyymmddPatternWithTime2(y,m,d,hr,min) if y.matches(validYearPattern) =>
        if (hr.toInt>12){
          "%s-%02d-%02d %s:%s".format(y,ti(m),ti(d),hr,min)
        } else {
          "%s-%02d-%02d %s:%s".format(y,ti(m),ti(d),hr,min)
        }
      case yyyymmddPatternWithTime3(y, m, d, hr, min, sec) if y.matches(validYearPattern) =>
        if (hr.toInt > 12) {
          "%s-%02d-%02d %s:%s:%s".format(y, ti(m), ti(d), hr, min, sec)
        } else {
          "%s-%02d-%02d %s:%s:%s".format(y, ti(m), ti(d), hr, min, sec)
        }
      case other =>
        val withNums = numerifyNames(other)
        withNums
    }

    val numfields = datestr.split("\\D+").map { _.trim }.filter { _.nonEmpty }.map { _.toInt}
    numfields.length match {
    case 1  =>

      val dstr = if (datestr.startsWith("2")) {
        // e.g., 20220330
        datestr.replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3")
      } else if (datestr.drop(4).startsWith("2")) {
        // e.g., 03302022
        datestr.replaceAll("(\\d{2})(\\d{2})(\\d{4})", "$3-$1-$2")
      } else {
        sys.error(s"bad date format [$datestr]")
      }
      val fmtr = datetimeFormatter6
      LocalDateTime.parse(s"${dstr} 00:00:00",fmtr)
    case 3 =>
      val fmtr = datetimeFormatter6
      LocalDateTime.parse(s"${datestr} 00:00:00",fmtr)
    case 5 =>
      if (numfields(3) <= 12) {
        LocalDateTime.parse(datestr, datetimeFormatter5)
      } else {
        LocalDateTime.parse(datestr, datetimeFormatter5b)
      }
    case 6 =>
      LocalDateTime.parse(datestr,datetimeFormatter6)
    case 7 =>
      LocalDateTime.parse(datestr,datetimeFormatter7)
    case _ =>
      // System.err.printf("%d datetime fields: [%s] [%s]\n".format(numfields.size,numfields.mkString("|"),datestr))
      LocalDateTime.parse(datestr,datetimeFormatter6)
    }
  }
  class RichString(val s: String) extends AnyVal {
    import java.time.*
    def str = s // .replaceAll(" ","T")
    def toDateTime: LocalDateTime                        = parseDateString(str) // LocalDateTime.parse(str,selectZonedFormat(str))
    def toInstant: Instant                         = Instant.parse(str)
 // def toLocalDate                       = LocalDate.parse(str)
//    def toDateTime:LocalDateTime               = toLocalDate.atStartOfDay
    def toDateTimeOption: Option[LocalDateTime]                  = toOption(toDateTime)
//    def toLocalDateOption                 = toOption(toLocalDate)
    def toDateTime(format: String): String        = dateTimeFormat(format)
    def toLocalDate(format: String): String       = localDateTimeFormat(format)
    def toDateTimeOption(format: String): Option[String]  = toOption(toDateTime(format))
    def toLocalDateOption(format: String): Option[String] = toOption(toLocalDate(format))

    private def toOption[A](f: => A): Option[A] = try {
      Some(f)
    } catch {
      case _: IllegalArgumentException => None
    }

    def dateTimeFormat(format: String): String      = s.format(DateTimeFormatter.ofPattern(format)) // .parseDateTime(s)
    def localDateTimeFormat(format: String): String = s.format(DateTimeFormatter.ofPattern(format)) // .parseLocalDate(s)
  }

  lazy val BadDate: LocalDateTime = parseDateStr("1900-01-01")
  lazy val EmptyDate: LocalDateTime = parseDateStr("1800-01-01")
  def date2string(d:LocalDateTime,fmt:String="yyyy-MM-dd"): String = d match {
    case EmptyDate => ""
    case other => other.toString(fmt)
  }
}
