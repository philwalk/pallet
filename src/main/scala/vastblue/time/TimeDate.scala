package vastblue.time

import vastblue.pallet.*
//import vastblue.time.TimeParser
import vastblue.time.ChronoParse

import java.time.ZoneId
import java.time.format.*
import io.github.chronoscala.Imports.*

import java.time.temporal.{ChronoField, TemporalAdjuster, TemporalAdjusters}
import scala.util.matching.Regex

object TimeDate extends vastblue.time.TimeExtensions {
  private[vastblue] def zoneid     = ZoneId.systemDefault
  private[vastblue] def zoneOffset = zoneid.getRules().getStandardOffset(now.toInstant())

  type DateTimeZone = java.time.ZoneId
  type DateTime     = LocalDateTime
  val DateTime = LocalDateTime

  private[vastblue] def parseLocalDate(_datestr: String, offset: Int = 0): DateTime = {
    dateParser(_datestr, offset) // .toLocalDate
  }

  lazy val timeDebug: Boolean = Option(System.getenv("TIME_DEBUG")) match {
  case None => false
  case _    => true
  }
  lazy val NullDate: LocalDateTime = DateTime.parse("0000-01-01T00:00:00") // .ofInstant(Instant.ofEpochMilli(0))

  // Patterns permit but don't require time fields
  // Used to parse both date and time from column 1.
  // Permits but does not require column to be double-quoted.
  lazy val YMDColumnPattern: Regex = """[^#\d]?(2\d{3})[-/](\d{1,2})[-/](\d{1,2})(.*)""".r
  lazy val MDYColumnPattern: Regex = """[^#\d]?(\d{1,2})[-/](\d{1,2})[-/](2\d{3})(.*)""".r

  lazy val standardTimestampFormat = datetimeFmt6

  lazy val datetimeFmt9  = "yyyy-MM-dd HH:mm:ss [-+][0-9]{4}"
  lazy val datetimeFmt8  = "yyyy-MM-dd HH:mm:ss-ss:S"
  lazy val datetimeFmt7  = "yyyy-MM-dd HH:mm:ss.S"
  lazy val datetimeFmt6  = "yyyy-MM-dd HH:mm:ss" // date-time-format
  lazy val datetimeFmt6B = "dd-MM-yyyy HH:mm:ss" // day first!
  lazy val datetimeFmt6C = "MM-dd-yyyy HH:mm:ss" // month first
  lazy val datetimeFmt6D = "M-dd-yyyy HH:mm:ss"  // month first
  lazy val datetimeFmt5  = "yyyy-MM-dd HH:mm"    // 12-hour format
  lazy val datetimeFmt5b = "yyyy-MM-dd kk:mm"    // 24-hour format
  lazy val dateonlyFmt   = "yyyy-MM-dd"          // date-only-format
  lazy val dateonlyFmtB  = "MM-dd-yyyy"           // month-first date-only-format

  lazy val datetimeFormatter9: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt9)
  lazy val datetimeFormatter8: DateTimeFormatter  = dateTimeFormatPattern(datetimeFmt8)
  lazy val datetimeFormatter7: DateTimeFormatter  = dateTimeFormatPattern(datetimeFmt7)
  lazy val datetimeFormatter6: DateTimeFormatter  = dateTimeFormatPattern(datetimeFmt6)
  lazy val datetimeFormatter6B: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt6B)
  lazy val datetimeFormatter6C: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt6C)
  lazy val datetimeFormatter6D: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt6D)
  lazy val datetimeFormatter5: DateTimeFormatter  = dateTimeFormatPattern(datetimeFmt5)
  lazy val datetimeFormatter5b: DateTimeFormatter = dateTimeFormatPattern(datetimeFmt5b)
  lazy val dateonlyFormatter: DateTimeFormatter   = dateTimeFormatPattern(dateonlyFmt)
  lazy val dateonlyFormatterB: DateTimeFormatter = dateTimeFormatPattern(dateonlyFmtB)

  lazy val EasternTime: ZoneId  = java.time.ZoneId.of("America/New_York")
  lazy val MountainTime: ZoneId = java.time.ZoneId.of("America/Denver")
  lazy val UTC: ZoneId          = java.time.ZoneId.of("UTC")

  private[vastblue] def LastDayAdjuster: TemporalAdjuster = TemporalAdjusters.lastDayOfMonth()

  // ==============================

  private[vastblue] def dateTimeFormatPattern(fmt: String, zone: ZoneId = ZoneId.systemDefault()): DateTimeFormatter = {
    val dtf1 = DateTimeFormatter.ofPattern(fmt).withZone(zone)
    val dtf = if (fmt.length <= "yyyy-mm-dd".length) {
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

  // type DateTime = DateTime
  // signed number of days between specified dates.
  // if date1 > date2, a negative number of days is returned.
  def daysBetween(idate1: DateTime, idate2: DateTime): Long = {
    assert(idate1 != null, "idate1 is null")
    assert(idate2 != null, "idate2 is null")
    val elapsedDays: Long = if (idate1.getMillis() < idate2.getMillis()) {
      between(idate1, idate2).getStandardDays
    } else {
      -(between(idate2, idate1).getStandardDays)
    }
    elapsedDays
  }
  // private var hook = 0
  def secondsBetween(idate1: DateTime, idate2: DateTime): Long = {
    // val d2d = idate1 to idate2 // new RichDuration(duration)
    // val d2d = new RichDuration(between(idate1, idate2))
    val d2d     = between(idate1, idate2) // .getStandardDays
    def seconds = d2d.getStandardSeconds.toLong
    val elapsedSeconds: Long = if (idate1.getMillis() <= idate2.getMillis()) {
      seconds
    } else {
      -seconds // negative number
    }
    elapsedSeconds
  }
  def secondsSince(date1: DateTime): Long = secondsBetween(date1, now)

  def endOfMonth(d: DateTime): DateTime = {
    val month: java.time.YearMonth = { java.time.YearMonth.from(d) }
    month.atEndOfMonth.atStartOfDay
  }

  def minutesBetween(date1: DateTime, date2: DateTime): Double = {
    secondsBetween(date1, date2).toDouble / 60.0
  }
  def minutesSince(date1: DateTime): Double = minutesBetween(date1, now)

  def hoursBetween(date1: DateTime, date2: DateTime): Double = {
    minutesBetween(date1, date2) / 60.0
  }
  def hoursSince(date1: DateTime): Double = hoursBetween(date1, now)

  def whenModified(f: java.io.File): DateTime = {
    val lastmod = f.toPath match {
    case p if java.nio.file.Files.exists(p) =>
      f.lastModified
    case _ =>
      -1
    }
    epoch2DateTime(lastmod, MountainTime)
  }

  def epoch2DateTime(epoch: Long, timezone: java.time.ZoneId = UTC): DateTime = {
    val instant = java.time.Instant.ofEpochMilli(epoch)
    java.time.LocalDateTime.ofInstant(instant, timezone)
  }

  /**
  * Returns days, hours, minutes, seconds between timestamps.
  */
  def getDuration(date1: DateTime, date2: DateTime): (Long, Long, Long, Long) = {
    val reverse = date1.getMillis() > date2.getMillis()
    val (d1, d2) = reverse match {
    case true => (date2, date1)
    case _    => (date1, date2)
    }
    val duration = between(d1, d2) // .toDuration
    val days     = duration.getStandardDays
    var (hours: Long, minutes: Long, seconds: Long) = (
      duration.getStandardHours,
      duration.getStandardMinutes,
      duration.getStandardSeconds
    )
    if (minutes > 0) {
      seconds -= minutes * 60
    }
    if (hours > 0) {
      minutes -= hours * 60
    }
    if (days > 0) {
      hours -= days * 24
    }
    (days, hours, minutes, seconds)
  }

  def nowZoned(zone: ZoneId = MountainTime): LocalDateTime = DateTime.now(zone)

  def now: LocalDateTime = nowZoned(MountainTime)
  def nowUTC: DateTime   = DateTime.now()

  // def fixDateFormat = vastblue.time.TimeDate.fixDateFormat _
  // def ageInMinutes  = vastblue.time.TimeDate.ageInMinutes _
  def ageInMinutes(f: java.io.File): Double = {
    if (f.exists) {
      val diff = (now.getMillis() - f.lastModified) / (60 * 1000).toDouble
      diff
    } else {
      1e6 // missing files are VERY stale
    }
  }
  def ageInDays(f: java.io.File): Double = {
    ageInMinutes(f) / (24 * 60)
  }
  def ageInDays(fname: String): Double = {
    ageInDays(new java.io.File(fname))
  }

  private[vastblue] def parse(str: String, format: String): DateTime = {
    if (timeDebug) System.err.print("parse(str=[%s], format=[%s]\n".format(str, format))
    if (format.length <= "yyyy-mm-dd".length) {
      DateTime.parse(str, dateTimeFormatPattern(format))
    } else {
      DateTime.parse(str, dateTimeFormatPattern(format))
    }
  }

  /** The new parser does not depend on TimeParser */
  def parseDateNew(_datestr: String, format: String = ""): DateTime = {
    // format: off
    val datestr = _datestr.
      replaceAll("/", "-"). // normalize field separator
      replaceAll("\"", ""). // remove quotes
      replaceAll(""" (\d): """, " 0$1:"). // make sure all time fields are 2 digits (zero filled)
      replaceAll("\\s+", " ").trim // compress random whitespace to a single space, then trim

    val pattern = (format != "", datestr.contains(":"), datestr.matches(""".* (AM|PM)\b.*"""), datestr.contains(".")) match {
      case (true, _, _, _)         => format // user-specified format
      case (_, false, _, _)        => "yyyy-MM-dd"
      case (_, true, false, false) => "yyyy-MM-dd HH:mm:ss"
      case (_,true, true,false)    => "yyyy-MM-dd hh:mm:ss aa"
      case (_,true,false, true)    => "yyyy-MM-dd HH:mm:ss.SSS"
      case (_,true, true, true)    => "yyyy-MM-dd hh:mm:ss aa.SSS"
    }
    try {
      parse(datestr, pattern)
    } catch {
      case e: IllegalArgumentException =>
        e.getMessage.contains("Illegal instant due to time zone offset") match {
        case true =>
          throw e
        case false =>
          parse(datestr, pattern)
        }
    }
    // format: on
  }

  def parseDateTime(str: String): DateTime = dateParser(str)

  lazy val ThreeIntegerFields1 = """(\d{2,4})\D(\d{1,2})\D(\d{1,2})""".r
  lazy val ThreeIntegerFields3 = """(\d{1,2})\D(\d{1,2})\D(\d{2,4})""".r
  lazy val ThreeIntegerFields2 = """(\d{2,2})\D(\d{1,2})\D(\d{1,2})""".r

  def dateParser(inpDateStr: String, offset: Int = 0): DateTime = {
    if (inpDateStr.trim.isEmpty) {
      BadDate
    } else {
      def isDigit(c: Char): Boolean = c >= '0' && c <= '9'
      val digitcount = inpDateStr.filter { (c: Char) => isDigit(c) }.size
      if (digitcount < 3 || digitcount > 19) {
        BadDate
      } else {
        val flds = vastblue.time.ChronoParse(inpDateStr)
        flds.dateTime // might be BadDate!
      }
    }
  }
  private[vastblue] def _dateParser(inpDateStr: String, offset: Int = 0): DateTime = {
    if (inpDateStr.startsWith("31/05/2009")) {
      hook += 1
    }
    if (inpDateStr.contains("-07")) {
      hook += 1
    }
    val _datestr = inpDateStr.trim.replaceAll("\"", "").replaceAll(" [-+][0-9]{4}$", "")
    val zonestr: String = inpDateStr.drop(_datestr.length)
    if (_datestr.isEmpty) {
      BadDate
    } else {
      // val ff = _datestr.split("\\D+")
      // first, deal with things like "12/31/21" and "22/5/5" (year has 2 digits)
      _datestr match {
      case ThreeIntegerFields1(_y, _m, _d) =>
        if (_y.length > 2) {
          val (y, m, d) = (_y.toInt, _m.toInt, _d.toInt)
          new RichString("%4d-%02d-%02d".format(y, m, d)).toDateTime
        } else {
          val nums           = List(_y, _m, _d).map { _.toInt }
          val possibleDays   = nums.zipWithIndex.filter { case (n, i) => n <= 31 }
          val possibleMonths = nums.zipWithIndex.filter { case (n, i) => n <= 12 }
          val (y, m, d) = possibleMonths match {
          case (n, 0) :: list => (nums(2), nums(0), nums(1)) // m/d/y
          case (n, 1) :: list => (nums(2), nums(1), nums(0)) // d/m/y
          case _ =>
            possibleDays match {
            case (n, 0) :: list => (nums(2), nums(1), nums(0)) // d/m/y
            case _              => (nums(2), nums(0), nums(1)) // m/d/y
            }
          }
          val year = if (y >= 1000) y else y + 2000
          new RichString("%4d-%02d-%02d".format(year, m, d)).toDateTime
        }
      case ThreeIntegerFields3(_m, _d, _y) =>
        val (y, m, d) = (_y.toInt, _m.toInt, _d.toInt)
        val year      = if (y >= 1000) y else y + 2000
        new RichString("%4d-%02d-%02d".format(year, m, d)).toDateTime
      case ThreeIntegerFields2(_x, _y, _z) =>
        val nums           = List(_x, _y, _z).map { _.toInt }
        val possibleDays   = nums.zipWithIndex.filter { case (n, i) => n <= 31 }
        val possibleMonths = nums.zipWithIndex.filter { case (n, i) => n <= 12 }
        val List(y, m, d)  = nums
        val year           = if (y >= 1000) y else y + 2000
        new RichString("%4d-%02d-%02d".format(year, m, d)).toDateTime
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
          //      printf("%s\n", datetimeFormatter.getClass)
          //      datetimeFormatter6.parse(fixed)
          DateTime.parse(fixed, datetimeFormatter6)
        } else if (_datestr.matches("""2\d{3}\D\d{2}\D\d{2}\.\d{4}""")) {
          // yyyy-MM-dd.HHMM
          val fixed = _datestr.replaceAll("""(....)\D(..)\D(..)\.(\d\d)(\d\d)""", "$1-$2-$3 $4:$5:00")
          DateTime.parse(fixed, datetimeFormatter6)
        } else {
          val datestr = _datestr.replaceAll("/", "-")
          try {
            val fixed = _datestr.
              replaceAll(" [-+][0-9]{4}$", "").
              replaceAll("([0-9])([A-Z])", "$1 $2").
              replaceAll("([a-z])([0-9])", "$1 $2")
            parseDateString(fixed)
          } catch {
            case r: RuntimeException if r.getMessage.toLowerCase.contains("bad date format") =>
//              if (TimeParser.debug) System.err.printf("e[%s]\n", r.getMessage)
              BadDate
            case p: DateTimeParseException =>
//              if (TimeParser.debug) System.err.printf("e[%s]\n", p.getMessage)
              BadDate
            case e: Exception =>
//              if (TimeParser.debug) System.err.printf("e[%s]\n", e.getMessage)
              BadDate
/*
              val mdate: TimeParser = TimeParser.parseDate(datestr).getOrElse(TimeParser.BadParsDate)
              // val timestamp = new DateTime(mdate.getEpoch)
              val standardFormat = mdate.toString(standardTimestampFormat)
              val timestamp      = standardFormat.toDateTime
              val hour           = timestamp.getHour // hourOfDay.get
              // format: off
              val extraHours = if (datestr.contains(" PM") && hour < 12) { 12 } else { 0 }
              val hours      = (offset + extraHours).toLong
              timestamp.plusHours(hours)
              // format: on
 */
          }
        }
      }
    }
  }

  def standardTime(datestr: String, offset: Int = 0): String = {
    dateParser(datestr, offset).toString(standardTimestampFormat)
  }
  def parseDate(datestr: String, offset: Int = 0): DateTime = {
    dateParser(datestr, offset)
  }

  def getDaysElapsed(idate1: DateTime, idate2: DateTime): Long = {
    if (idate2.getMillis() < idate1.getMillis()) {
      -between(idate2, idate1).getStandardDays
    } else {
      between(idate1, idate2).getStandardDays
    }
  }
  def getDaysElapsed(datestr1: String, datestr2: String): Long = {
    getDaysElapsed(dateParser(datestr1), dateParser(datestr2))
  }
  private[vastblue] def selectZonedFormat(_datestr: String): java.time.format.DateTimeFormatter = {
    val datestr   = _datestr.replaceAll("/", "-")
    val numfields = datestr.split("\\D+")
    numfields.length <= 3 match {
    case true  => dateonlyFormatter
    case false => datetimeFormatter6
    }
  }
  private[vastblue] def ti(s: String): Int = {
    s match {
    case n if n.matches("0\\d+") =>
      n.replaceAll("0+(.)", "$1").toInt
    case n =>
      n.toInt
    }
  }
  def numerifyNames(datestr: String) = {
    val noweekdayName = datestr.replaceAll("(?i)(Sun[day]*|Mon[day]*|Tue[sday]*|Wed[nesday]*|Thu[rsday]*|Fri[day]*|Sat[urday]*),? *", "")
//    val nomonthName = datestr.replaceAll("(?i)(Jan[ury]*|Feb[ruay]*|Mar[ch]*|Apr[il]*|May|Jun[e]*|Jul[y]*|Aug[st]*|Sep[tmbr]*|Oct[ober]*|Nov[embr]*|Dec[mbr]*),? *", "")
//    if (noweekdayName != datestr || nomonthName != datestr){
//      hook += 1
//    }
    noweekdayName match {
    case str if str.matches("(?i).*[JFMASOND][aerpuco][nbrylgptvc][a-z]*.*") =>
      var ff = str.replaceFirst("([a-zA-Z])([0-9])", "$1 $2").split("[-/,\\s]+")
      val monthIndex = ff.indexWhere {(s: String) => s.matches("(?i).*[JFMASOND][aerpuco][nbrylgptvc][a-z]*.*")}
      if (monthIndex >= 0){
        val monthName = ff(monthIndex)
        val month: Int = ChronoParse.monthAbbrev2Number(ff(monthIndex))
        val nwn = noweekdayName.replaceAll(monthName, "%02d ".format(month))
        nwn
      } else {
        // format: off
        if (ff(0).matches("\\d+")) {
          // swap 1st and 2nd fields (e.g., convert "01 Jan" to "Jan 01")
          val tmp = ff(0)
          ff(0) = ff(1)
          ff(1) = tmp
        }
        val mstr = ff.head.take(3)
        if (!mstr.toLowerCase.matches("[a-z]{3}")) {
          hook += 1
        }
        val month = ChronoParse.monthAbbrev2Number(mstr)
        ff = ff.drop(1)
        // format: off
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
        // format: on
        "%4d-%02d-%02d%s%s".format(year, month, day, timestr, tz)
      }
    case str =>
      str
    }
  }

  lazy val mmddyyyyPattern: Regex          = """(\d{1,2})\D(\d{1,2})\D(\d{4})""".r
  lazy val mmddyyyyTimePattern: Regex      = """(\d{1,2})\D(\d{1,2})\D(\d{4})(\D\d\d:\d\d(:\d\d)?)""".r
  lazy val mmddyyyyTimePattern2: Regex     = """(\d{1,2})\D(\d{1,2})\D(\d{4})\D(\d\d):(\d\d)""".r
  lazy val mmddyyyyTimePattern3: Regex     = """(\d{1,2})\D(\d{1,2})\D(\d{4})\D(\d\d):(\d\d):(\d\d)""".r
  lazy val mmddyyyyTimePattern3tz: Regex   = """(\d{1,2})\D(\d{1,2})\D(\d{4})\D(\d\d):(\d\d):(\d\d)\D(-?[0-9]{4})""".r
  lazy val yyyymmddPattern: Regex          = """(\d{4})\D(\d{1,2})\D(\d{1,2})""".r
  lazy val yyyymmddPatternWithTime: Regex  = """(\d{4})\D(\d{1,2})\D(\d{1,2})(\D.+)""".r
  lazy val yyyymmddPatternWithTime2: Regex = """(\d{4})\D(\d{1,2})\D(\d{1,2})\D+(\d{2}):(\d{2})""".r
  lazy val yyyymmddPatternWithTime3: Regex = """(\d{4})\D(\d{1,2})\D(\d{1,2})\D+(\d{2}):(\d{2}):(\d{2})""".r
  lazy val mmddyyyyPatternWithTime3: Regex = """(\d{1,2})\D(\d{1,2})\D(\d{4})\D+(\d{2}):(\d{2}):(\d{2})""".r

  lazy val validYearPattern = """(1|2)\d{3}""" // only consider years between 1000 and 2999

  // format: off
  private[vastblue] def parseDateString(_datestr: String): LocalDateTime = {
    if (_datestr.startsWith("31")) {
      hook += 1
    }
    var datestr = _datestr.
      replaceAll("/", "-").
      replaceAll("#", "").
      replaceAll("-[0-9]+:[0-9]+$", "").
      replaceAll("([0-9])T([0-9])", "$1 $2").trim
    datestr = datestr match {
    case mmddyyyyPattern(m, d, y) =>
      "%s-%02d-%02d".format(y, ti(m), ti(d))

    case mmddyyyyTimePattern(m, d, y, t) =>
      "%s-%02d-%02d%s".format(y, ti(m), ti(d), t)

    case mmddyyyyTimePattern2(m, d, y, h, min) if y.matches(validYearPattern) =>
      "%s-%02d-%02d %02d:%02d".format(y, ti(m), h, min)

    case mmddyyyyTimePattern3(m, d, y, h, min, s) if m.toInt <= 12 && y.matches(validYearPattern) =>
      "%s-%02d-%02d %02d:%02d:02d".format(y, ti(m), ti(d), h, min, s)

    case mmddyyyyTimePattern3tz(m, d, y, h, min, s, tz) if y.matches(validYearPattern) =>
      "%s-%02d-%02d %02d:%02d:02d %s".format(y, ti(m), ti(d), h, min, s)

    case mmddyyyyPatternWithTime3(dm, md, y, h, min, s) if y.matches(validYearPattern) =>
      val Seq(tyr, tmd, tdm, th, tmin, ts) = Seq(y, md, dm, h, min, s).map ( ti(_) )
      val dstr = if (tdm > 12) {
        // dm is day, md is month
        "%04d-%02d-%04d %02d:%02d:%02d %d".format(tyr, tmd, tdm, th, tmin, ts)
      } else {
        // md is day, dm is month
        "%04d-%02d-%04d %02d:%02d:%02d %d".format(tyr, tdm, tmd, th, tmin, ts)
      }
      dstr

    case yyyymmddPattern(y, m, d) if y.matches(validYearPattern) =>
      "%s-%02d-%02d".format(y, ti(m), ti(d))

    case yyyymmddPatternWithTime(y, m, d, t) if y.matches(validYearPattern) =>
      "%s-%02d-%02d%s".format(y, ti(m), ti(d), t)

    case yyyymmddPatternWithTime2(y, m, d, hr, min) if y.matches(validYearPattern) =>
      if (hr.toInt>12) {
        "%s-%02d-%02d %s:%s".format(y, ti(m), ti(d), hr, min)
      } else {
        "%s-%02d-%02d %s:%s".format(y, ti(m), ti(d), hr, min)
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
    val numstrings = datestr.split("\\D+").map { _.trim }.filter { _.nonEmpty }
    val numfields = numstrings.map { _.toInt}
    val numWidths = numstrings.map { _.length }
    numfields.length match {
      case 1 =>
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
        DateTime.parse(s"${dstr} 00:00:00", fmtr)
      case 3 =>
        datestr = datestr.replaceAll("\\D+", "-")
        val fmtr = if (numWidths.mkString == "224") {
          datetimeFormatter6B
        } else {
          datetimeFormatter6
        }
        DateTime.parse(s"$datestr 00:00:00", fmtr)
      case 5 =>
        if (numfields(3) <= 12) {
          DateTime.parse(datestr, datetimeFormatter5)
        } else {
          DateTime.parse(datestr, datetimeFormatter5b)
        }
      case 6 =>
        if (numfields(2) > 1000) {
          // partial ambiguity elimination
          if (numfields(0) > 12) {
            DateTime.parse(datestr, datetimeFormatter6B)
          } else {
            if (numWidths(0) > 1) {
              DateTime.parse(datestr, datetimeFormatter6C)
            } else {
              DateTime.parse(datestr, datetimeFormatter6D)
            }
          }
        } else {
          DateTime.parse(datestr, datetimeFormatter6)
        }
      case 7 =>
        DateTime.parse(datestr, datetimeFormatter7)
      case _ =>
        // System.err.printf("%d datetime fields: [%s] [%s]\n".format(numfields.size, numfields.mkString("|"), datestr))
        DateTime.parse(datestr, datetimeFormatter6)
    }
  }
  // format: on

  class RichString(val s: String) extends AnyVal {
    def str                                     = s
    def toDateTime: LocalDateTime               = parseDateString(str) // DateTime.parse(str, selectZonedFormat(str))
    def toInstant: Instant                      = Instant.parse(str)
    def toDateTimeOption: Option[LocalDateTime] = toOption(toDateTime)
    def toDateTime(format: String): String      = dateTimeFormat(format)
    def toLocalDate(format: String): String     = localDateTimeFormat(format)

    def toDateTimeOption(format: String): Option[String]  = toOption(toDateTime(format))
    def toLocalDateOption(format: String): Option[String] = toOption(toLocalDate(format))

    private def toOption[A](f: => A): Option[A] =
      try {
        Some(f)
      } catch {
        case _: IllegalArgumentException => None
      }

    def dateTimeFormat(format: String): String      = s.format(DateTimeFormatter.ofPattern(format)) // .parseDateTime(s)
    def localDateTimeFormat(format: String): String = s.format(DateTimeFormatter.ofPattern(format)) // .parseLocalDate(s)
  }

  lazy val BadDate: DateTime   = dateParser("1900-01-01")
  lazy val EmptyDate: DateTime = dateParser("1800-01-01")

  def date2string(d: DateTime, fmt: String = "yyyy-MM-dd"): String = d match {
  case EmptyDate => ""
  case other     => other.toString(fmt)
  }
}
