package vastblue.time

/**
 * This is useful for converting between a wide variety
 * of Date and Time Strings and the ParsDate class.
 */
import vastblue.pallet.*
import java.io.{File => JFile}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Calendar, Date, Locale}

//import vastblue.time.TimeDate.{parseDateTime => coreParseDate}
import vastblue.time.TimeDate.*

import scala.collection.immutable.*
import scala.util.matching.Regex
import scala.util.control.Breaks.*

object ParsDate {
  var verbose        = ".verbose".path.isFile
  var debug: Boolean = ".debug".path.isFile
  var yearFirstFlag  = true
  var gcal           = new java.util.GregorianCalendar()

  def reset(): Unit = {
    // format: off
    verbose       = ".verbose".path.isFile
    debug         = ".debug".path.isFile
    yearFirstFlag = true
    gcal          = new java.util.GregorianCalendar()
    currentFormat = null // reserved for most recent runtime pattern
    newFormats    = scala.collection.immutable.Set[String]()
    outfmt        = dateTimeFormat // by default, show both date and time
    // format: on
  }

  lazy val MDY: Regex              = """.*(\d{1,2})\D(\d{1,2})\D(\d{4}).*""".r
  lazy val DayMonthYr: Regex       = """(?i)(\d+)[^\d\w]+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[^\d\w](\d{4})""".r
  lazy val PackedIsoPattern: Regex = """.*\b(19\d{2}|20\d{2})(\d{2})(\d{2}).*""".r

  // initialize date patterns
  lazy val yy: String  = """(20\d{2})"""
  lazy val nn: String  = """\d{1,2}"""
  lazy val dd: String  = s"($nn)"
  lazy val mm: String  = s"(${firstThreeLettersPatternBuilder(monthNames)}|$nn)"
  lazy val div: String = """[-\s/,]+"""

  lazy val YYMMddPtrn: Regex = ("""(?i).*""" + yy + div + mm + div + dd + """.*""").r // yyyy mm dd
  lazy val MMDDyyPtrn: Regex = ("""(?i).*""" + mm + div + dd + div + yy + """.*""").r // mm dd yyyy
  lazy val DDMMyyPtrn: Regex = ("""(?i).*""" + dd + div + mm + div + yy + """.*""").r // dd mm yyyy

  def isLeapYear(year: Int): Boolean = gcal.isLeapYear(year)

  lazy val YearPattern  = yy.r
  lazy val NumFieldPtrn = nn.r
  lazy val DayPtrn      = dd.r
  lazy val MonthPattern = mm.r

  var currentFormat: SimpleDateFormat = null // reserved for most recent runtime pattern

  val dateOnlyFormat: DateFormat   = new SimpleDateFormat("yyyy/MM/dd", Locale.US)          // preferred output format 1:
  val dateTimeFormat: DateFormat   = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss", Locale.US) // preferred output format 2:
  val dateTimeMsFormat: DateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a", Locale.US)
  var newFormats: Set[String]      = scala.collection.immutable.Set[String]()

  val monthMap: Map[String, String] = Map(
    "jan" -> "01",
    "feb" -> "02",
    "mar" -> "03",
    "apr" -> "04",
    "may" -> "05",
    "jun" -> "06",
    "jul" -> "07",
    "aug" -> "08",
    "sep" -> "09",
    "oct" -> "10",
    "nov" -> "11",
    "dec" -> "12",
  )

  var outfmt: DateFormat = dateTimeFormat // by default, show both date and time

  def dateAndTime(): Unit = { outfmt = dateTimeFormat }
  def dateOnly(): Unit    = { outfmt = dateOnlyFormat }

  def setFormat(fmt: DateFormat): Unit = { outfmt = fmt }
  def setFormat(fmt: String): Unit     = { outfmt = new SimpleDateFormat(fmt) }

  def extractDateFromText(rawline: String): Option[ParsDate] = {
    // debug: test before toLowerCase
    val text = rawline.replaceAll("""[^-a-zA-Z:/_0-9\s]+""", " ").replaceAll("""\s+""", " ").trim
    if (debug) {
      text match {
      case YYMMddPtrn(yy, mm, dd) if okYMD(yy, mm, dd) =>
        if (verbose) printf("ymd: [%s] [%s] [%s] [%s]\n", yy, mm, dd, text)
      case MMDDyyPtrn(mm, dd, yy) if okYMD(yy, mm, dd) =>
        if (verbose) printf("mdy: [%s] [%s] [%s] [%s]\n", yy, mm, dd, text)
      case DDMMyyPtrn(dd, mm, yy) if okYMD(yy, mm, dd) =>
        if (verbose) printf("dmy: [%s] [%s] [%s] [%s]\n", yy, mm, dd, text)
      case _ =>
        printf("4-text[%s]\n", text)
      }
    }
    def lc = text.toLowerCase.replaceAll("""\s+""", " ").replaceAll("""(\d)(st|nd|rd|th)\b""", "$1")
    val result = if (lc.contains("@")) {
      // ignore email
      if (verbose) printf("ignore email[%s]\n", text)
      None // Some(ParsDate("2019:01:01"))
    } else if (lc.matches(""".*\bapprov.*""")) {
      if (verbose) printf("ignore approval of minutes [%s]\n", text)
      None
    } else if (lc.matches(""".*\bletter.*""")) {
      if (verbose) printf("ignore reference to letter [%s]\n", text)
      None
    } else {
      lc.replaceAll("""\.""", "/") match {
      case PackedIsoPattern(yy, mm, dd) if okYMD(yy, mm, dd) =>
        if (verbose) printf("iso: [%s] [%s] [%s] [%s]\n", yy, mm, dd, text)
        Some(normalizedMdate(yy, mm, dd))
      case YYMMddPtrn(yy, mm, dd) if okYMD(yy, mm, dd) =>
        if (verbose) printf("ymd: [%s] [%s] [%s] [%s]\n", yy, mm, dd, text)
        Some(normalizedMdate(yy, mm, dd))
      case MMDDyyPtrn(mm, dd, yy) if okYMD(yy, mm, dd) =>
        if (verbose) printf("mdy: [%s] [%s] [%s] [%s]\n", yy, mm, dd, text)
        Some(normalizedMdate(yy, mm, dd))
      case DDMMyyPtrn(dd, mm, yy) if okYMD(yy, mm, dd) =>
        if (verbose) printf("dmy: [%s] [%s] [%s] [%s]\n", yy, mm, dd, text)
        Some(normalizedMdate(yy, mm, dd))
      case YYMMddDensePattern(yy, mm, dd) if okYMD(yy, mm, dd) =>
        Some(normalizedMdate(yy, mm, dd))
      case _ =>
        None // Some(ParsDate("2019:02:02"))
      }
    }
    result
  }
  def validMonth(mm: String): Boolean = {
    val (_, valid) = monthToInt(mm)
    valid
  }
  def validDay(dd: String): Boolean = {
    val dnum = numStringToInt(dd)
    dnum >= 1 && dnum <= 31
  }
  def okYMD(yy: String, mm: String, dd: String): Boolean = {
    validMonth(mm) && validDay(dd) && yy.matches("\\d{4}")
  }
  def monthToInt(mm: String): (Int, Boolean) = {
    val num = mm match {
    case DayPtrn(nn)        => numStringToInt(nn)
    case LcMonthPattern(mm) => monthName2Number(mm)
    case _                  => -1
    }
    val valid = (num >= 1 && num <= 12)
    (num, valid)
  }
  def normalizedMdate(yy: String, mm: String, dd: String): ParsDate = {
    val (y, m, d)  = numericFields(yy, mm, dd)
    val normalized = "%04d/%02d/%02d".format(y, m, d)
    if (verbose) printf("normalized: [%s]\n", normalized)
    ParsDate(normalized)
  }
  lazy val YYMMddDensePattern: Regex = """.*(\b2[01]\d{2})(\d{2})(\d{2})\b.*""".r
  lazy val LcMonthPattern: Regex     = (s"(?i)${MonthPattern.toString}").r

  def numericFields(yy: String, mm: String, dd: String): (Int, Int, Int) = {
    // val y = numStringToInt(yy)
    // val d = numStringToInt(dd)
    val monthNum = mm match {
    case DayPtrn(nn)        => numStringToInt(nn)
    case LcMonthPattern(mm) => monthName2Number(mm)

    case _ =>
      val msg = s"error: month string [$mm] not matched:\nDayPtrn[%s]\nLcMonthPattern[%s]\n".format(DayPtrn, LcMonthPattern)
      sys.error(msg)
    }
    val (year, month, day) = (numStringToInt(yy), monthNum, numStringToInt(dd))
    if (month < 1 || month > 12) eprintf("month[%s] converted to bogus month number [%d]\n".format(mm, month))
    assert(month >= 1 && month <= 12)
    if (day < 1 || day > 31) eprintf("day[%s] converted to bogus day number [%d]\n".format(dd, day))
    assert(day >= 1 && day <= 31)
    (year, month, day)
  }
  def numStringToInt(nn: String): Int = {
    val withoutLeadingZeros = nn.replaceFirst("""^0*([1-9]\d+)""", """$1""")
    if (verbose && nn.length != withoutLeadingZeros.length) {
      printf("nn[%s]\nwithoutZeros[%s]\n", nn, withoutLeadingZeros)
    }
    withoutLeadingZeros.toInt // remove leading zeros before calling .toInt
  }
  lazy val monthNames: List[String] = List(
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  )
  lazy val weekdayNames: List[String] = List(
    "Sunday",
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
  )
  lazy val monthAbbreviationsLowerCase: List[String] = monthNames.map { _.toLowerCase.substring(0, 3) }
  def indexedLetters(idx: Int, list: List[String]): String = {
    new String({
      var uniqChars = List[Char]()
      for (cc <- list.map { _.charAt(idx) }.toArray) {
        if (!uniqChars.contains(cc)) {
          uniqChars ::= cc
        }
      }
      uniqChars.reverse.toArray
    })
  }
  def firstThreeLettersPatternBuilder(list: List[String]): String = {
    val cc0 = indexedLetters(0, list)
    val cc1 = indexedLetters(1, list)
    val cc2 = indexedLetters(2, list)
    s"[$cc0][$cc1][$cc2][\\.\\w]*"
  }

  def apply(): ParsDate = {
    ParsDate(new Date)
  }

  def apply(time: Long): ParsDate = {
    new ParsDate(time)
  }

  def apply(date: Date): ParsDate = {
    ParsDate(date.getTime)
  }

  def apply(tupleDate: ((Int, Int, Int), (Int, Int, Int))): ParsDate = {
    val (date, time)  = tupleDate
    val (yy, mm, day) = date
    val (hr, mn, sec) = time
    apply("%04d/%02d/%02d %02d:%02d:%02d".format(yy, mm, day, hr, mn, sec))
  }

  lazy val BadParsDate = apply(-1L)

  def apply(datestr: String): ParsDate = {
    parseDate(datestr).getOrElse(BadParsDate)
  }

  def apply(yy: Int, mm: Int, dd: Int): ParsDate = {
    // Calendar month is zero-based
    val cal = java.util.Calendar.getInstance
    cal.set(yy, mm - 1, dd)
    apply(cal.getTime)
  }
  def apply(date: Any): ParsDate = {
    date match {
    case tt: Long   => apply(tt)
    case tt: Date   => apply(tt)
    case tt: String => apply(tt)

    case (yy: Int, mm: Int, dd: Int) => apply(yy, mm, dd)

    case _ =>
      eprintf("arg.class == [%s]\n".format(date.getClass.getName))
      sys.error(s"bad date constructor arg[$date]")
    }
  }
  def adjustHour(twoDigitNumber: String, amflag: Boolean, pmflag: Boolean): String = {
    var number: Int = if (twoDigitNumber.startsWith("0")) {
      twoDigitNumber.substring(1).toInt
    } else {
      twoDigitNumber.toInt
    }
    if (amflag) {
      if (number == 12) {
        number = 0
      }
    } else if (pmflag && number < 12) {
      number += 12
    }
    s"$number"
  }

  lazy val MonthNamePattern: Regex = """(?i)(.*)\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\b(.*)""".r

  def monthAbbrev2Number(name: String): Int = {
    name.toLowerCase.substring(0, 3) match {
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
  def monthName2Number(rawname: String): Int = {
    val name = rawname.replaceAll("""(\d+)[a-zA-Z]{2}""", "$1")
    if (name.length < 3) { printf("illegal month name parameter: name[%s] rawname[%s]", name, rawname) }
    assert(name.length >= 3, s"illegal month name parameter: [$name]")
    val abbr   = name.toLowerCase.replaceAll("""[^a-zA-Z]+""", "").substring(0, 3)
    val result = monthAbbrev2Number(abbr)
    assert(result >= 1 && result <= 12)
    result
  }

  lazy val QuadrixBackIssuesFilenameFormat: Regex = """(?i)([jfmasond][aepuco][nbrylgptvc])\D?(\d{1,2})\D(\d{1,4})""".r
  def toNumericFormat(dateStrRaw: String): String = {
    // "Wed Apr 08 18:17:08 2009"
    var dateStr = dateStrRaw.replaceAll("""\b(Sun|Mon|Tue|Wed|Thu|Fri|Sat)[a-z]*\b""", "").replaceAll("""\s*, \s*""", "").trim
    dateStr = dateStr match {
    case MDY(m, d, y) =>
      val (mo, da, yr) = (m.toInt, d.toInt, y.toInt)
      "%4d-%02d-%02d".format(yr, mo, da)
    case DayMonthYr(d, monthname, y) =>
      val monthNumber  = monthName2Number(monthname)
      val (yr, mo, da) = (y.toInt, monthNumber, d.toInt)
      "%4d-%02d-%02d".format(yr, mo, da)
    case MonthNamePattern(pre, _mid, _post) =>
      val mid  = _mid.replaceAll("\\s+", "-").trim
      val post = _post.trim
      if (pre.isEmpty) {
        val mon = wordMap(mid).trim
        (s"$mon-$post").replaceAll("[-]+", "-").trim
      } else if (post.isEmpty) {
        s"${pre.trim}-${wordMap(mid.trim)}"
      } else {
        s"${pre.trim}-${wordMap(mid.trim)}-${post.trim}"
      }
    case QuadrixBackIssuesFilenameFormat(monthName, day, year2digit) =>
      val mo = "%02d".format(monthName2Number(monthName))
      val dy = "%02d".format(day.toInt)
      val yr = s"20$year2digit"
      s"${yr}-${mo}-${dy}"

    case _ =>
      sys.error(s"unsupported format[$dateStr")
    }
    dateStr.trim
  }
  def wordMap(_str: String): String = {
    val str = _str.toLowerCase.trim
    str match {
    case "jan" | "feb" | "mar" | "apr" | "may" | "jun" | "jul" | "aug" | "sep" | "oct" | "nov" | "dec" =>
      monthMap(str)
    case other if other.matches("""\d+""") => // numeric
      other
    case _ =>
      "" // junk
    }
  }
  def reorderYearFirst(ff: Array[String]): Array[String] = {
    if (ff(2).length == 4) {
      // reorder first 3 from mm/dd/yyyy to yyyy/mm/dd
      def zpad(s: String): String = {
        if (s.length == 1) {
          s"0$s"
        } else {
          s
        }
      }
      val Array(f0, f1, f2) = ff.take(3)
      ff(0) = f2
      ff(2) = zpad(f1)
      ff(1) = zpad(f0)
    } else if (ff.head.length == 4) {
      // nothing to do
    } else {
      throw new RuntimeException("unknown format [%s]".format(ff.mkString(",")))
    }
    ff
  }
  def to24hourFormat(dateStrRaw: String): (String, String) = {
    var dateStr = dateStrRaw
    // standardize delimiters
    if (dateStr.trim.isEmpty || dateStr == "null") {
      throw new IllegalArgumentException("null dateStr argument")
    }
    val TZPattern = """(.*)\s([A-Z]{3})$""".r
    val zone = dateStr.trim match {
    case TZPattern(tstamp, zone) =>
      dateStr = tstamp
      zone
    case _ =>
      ""
    }
    val amflag = dateStr.endsWith(" AM")
    val pmflag = dateStr.endsWith(" PM")
    val ff     = reorderYearFirst(dateStr.trim.split("""\D+"""))
    ff.size match {
    case 3 =>
      assert(!pmflag, "unexpected pmflag==true!")
      assert(!amflag, "unexpected amflag==true!")
      dateStr = ff.mkString("/")
    case 5 =>
      ff(3) = adjustHour(ff(3), amflag, pmflag)
      dateStr = s"${ff.slice(0, 3).mkString("/")} ${ff.slice(3, 5).mkString(":")}"
    case 6 =>
      ff(3) = adjustHour(ff(3), amflag, pmflag)
      dateStr = s"${ff.slice(0, 3).mkString("/")} ${ff.slice(3, 6).mkString(":")}"
    case 7 =>
      ff(3) = adjustHour(ff(3), amflag, pmflag)
      dateStr = s"${ff.slice(0, 3).mkString("/")} ${ff.slice(3, 6).mkString(":")}" // + "."+ff(6)
    case _ =>
      if (ff.nonEmpty) {
        eprintf("ff.size==%d\n".format(ff.size))
        ff.foreach { it => printf("[%s]\n", it) }
      }
      sys.error(s"dateStr[$dateStr]")
    }
    (dateStr, zone)
  }

  def parseDate(date: Date): ParsDate     = ParsDate(date)
  def parseDate(date: ParsDate): ParsDate = ParsDate(date.getTime)

  def prepDateString(str: String): (Boolean, String, String) = {
    if (str.contains(":") && str.matches(".* 2[0-9]{3}")) {
      hook += 1
    }
    var yearFirst = false
    // format: off
    var dateStr = str.
      replaceAll("\"", "").trim. // remove quotes
      replaceAll(",", "/").
      replaceAll("/", "-"). // cut number of formats in half
      replaceFirst("(?i)^([a-z]{3,})(\\d.*)", "$1-$2") // separate apr12-14 into apr-12-14
    // format: on

    if (str.endsWith("PM")) {
      hook += 1
    }
    if (str.startsWith("08/04/")) {
      hook += 1
    }

    if ("""(?i)\b[JFMASOND][aepuco][nbrylgptvc]""".r.findFirstIn(dateStr) != None) {
      // assume year is either first or last (never in the middle).
      // if Month is first, year must be last
      val monthFirst = """^(?i)[JFMASOND][aepuco][nbrylgptvc]""".r.findFirstIn(dateStr) != None
      yearFirst = !monthFirst
      // must repair 2-digit year before throwing away monthFirst information
      if (monthFirst && !dateStr.matches(".*\\d{4}")) {
        val testYear = dateStr.replaceAll(".*\\D", "")
        // warning: this code will expire in 2050! (bad workaround for a bad problem)
        val yearString = testYear match {
        case yy if yy > "50" => s"19${yy}"
        case it              => s"20${it}"
        }
        dateStr = dateStr.replaceAll("\\d+$", yearString)
      }
      dateStr = toNumericFormat(dateStr)
    }
    var zone = ""

    yearFirst = if (dateStr.contains(":")) { // dateStr.contains("AM") || dateStr.contains("PM") || dateStr.matches(".* [-+][0-9]{4}$")) {
      val (dstr, zoneStr) = to24hourFormat(dateStr)
      dateStr = dstr
      zone = zoneStr
      true // year is now first
    } else {
      // assume year first if year is not last
      ("""\b[12]\d{3}$|\b[12]\d{3}\s""".r.findFirstIn(dateStr) == None)
    }
    if (debug) printf("yearFirst: %s\n", yearFirst)
    (yearFirst, dateStr, zone)
  }

  /**
  * Test whether a String is a Date string.
  */
  def isDate(text: String): Boolean = {
    try {
      if (text.matches(""".*\d\d.*""")) {
        ParsDate(text)
        true
      } else {
        // a date string requires at least 2 consecutive digits (the year)
        // e.g.: Thu, Dec 8, 49
        false
      }
    } catch {
      case _: Exception =>
        false
    }
  }

  lazy val legalCharacters: Set[Char] = "0123456789-:/ abcdefghijklmnopqrstuvwxyz.,+()".toSet // time zones can be in parentheses

  def tryFormat(dateStr: String, fmt: SimpleDateFormat): Option[ParsDate] = {
    try {
      val dt    = fmt.parse(dateStr)
      val pdate = ParsDate(dt)
      Some(pdate)
    } catch {
      case ee: Exception =>
        None
    }
  }

  /**
  * Parse date String.
  */
  def parseDate(rawdate: String): Option[ParsDate] = {
    if (rawdate.startsWith("08/04/")) {
      hook += 1
    }
    // do a case-insensitive test for legal characters
    rawdate.find { cc => !legalCharacters.contains(cc.toLower) }.foreach { cc =>
      sys.error(s"illegal character [$cc] in date string[$rawdate]")
    }
    if (!rawdate.matches("""^(?i)[-:/\d\s,\.JanFebMrApyulgSOctNvDi]+$""")) {
      throw new IllegalArgumentException(s"bogus date string[$rawdate]")
    }
    // standardize format to use hyphenated y-m-d rather than y/m/d
    val (yearFirst, dateStr, zone) = prepDateString(rawdate)

    var parsOpt: Option[ParsDate] = None

    val dateFormats = relevantFormats(dateStr, yearFirst)
    dateFormats.find { testfmt =>
      tryFormat(dateStr, testfmt).foreach { (pd: ParsDate) =>
        parsOpt = Some(pd)
        // save most recent successful format (try it on first attempt next time)
        currentFormat = testfmt
        yearFirstFlag = yearFirst
        if (debug) printf("<<< [%s], [%s] [%s]\n", dateStr, currentFormat.toPattern, parsOpt)
      }
      parsOpt.nonEmpty
    }
    if (debug) printf("date: [%s], yearFirst: %s, currentFormat: [%s]\n", rawdate, yearFirst, currentFormat)
    if (parsOpt.isEmpty) {
      parsOpt = Option(guessFormat(dateStr))
    }
    parsOpt.foreach { mdate =>
      mdate.zone = zone
    }
    parsOpt
  }

  // format: off
  def selfFormat(str: String): String = {
    str.
      replaceAll("""[-+]\d\d\d\d\b""",    "Z").
      replaceAll("""\b(\d\d\d\d)\b""",    "yyyy").
      replaceAll(""" \d\d:""",            " hh:").
      replaceAll(""" \d:""",              " hh:").
      replaceAll("""h:\d\d\b""",          "h:mm").
      replaceAll("""h:\d\b""",            "h:mm").
      replaceAll("""m:\d\d\b""",          "m:ss").
      replaceAll("""m:\d\b""",            "m:ss").
      replaceAll("""mm(\d\d?:\d\d?)\b""", " hh:mm").
      replaceAll("""y([-/])\d\d\b""",     "y$1MM").
      replaceAll("""y([-/])\d\b""",       "y$1MM").
      replaceAll("""M([-/])\d\d\b""",     "M$1dd").
      replaceAll("""M([-/])\d\b""",       "M$1dd").
      replaceAll("""^\d\d([-/])""",       "MM$1").
      replaceAll("""^\d([-/])""",         "M$1").
      replaceAll("""M([-/])\d\d\b""",     "M$1dd").
      replaceAll("""M([-/])\d\b""",       "M$1d").
      replaceAll("""s\.\d+\b""",          "s.S").
      replaceAll("""\b[SMTWF][uoera][neduit][a-z]*\b""",  "EEE"). // day-of-week
      replaceAll("""^\s*,\s*""",         "").
      replaceAll("""\b(Jan[uary]*|Feb[uary]*|Mar[ch]*|Apr[ril]*|May|June?|July?|Aug[ust]*|Sep[tembr]*|Oct[tober]*Nov[embr]*|Dec[embr]*)\b""",         "MMM").
      replaceAll("""\b\d\d? M""",         "dd M").
      replaceAll("""M \d\d?\b""",         "M dd").
      replaceAll(""" \(?[A-Z][A-Z]+T\)?""", " ").trim
  }
  // format: on

  /**
  * Guess date format.
  * TODO: currently unable to parse "January 12, 1972" !!!!
  */
  def guessFormat(date: String): ParsDate = {
    val dateStr = date
    val sf      = selfFormat(date)
    val fmt     = simpleFormat(sf)
    try {
      val dt = fmt.parse(date)
      ParsDate(dt)
    } catch {
      case _: Exception =>
        if (debug) eprintf("failed self-format: [%s] : [%s]\n", date, fmt)
        var mdate: ParsDate = null

        // year is easy
        var onthefly = date.replaceFirst("""\d\d\d\d""", "yyyy")

        // possible variants of month first
        if ("""^\d\D""".r.findFirstIn(onthefly) != None) {
          onthefly = onthefly.replaceFirst("""\d""", "M")
        } else if ("""^\d\d""".r.findFirstIn(onthefly) != None) {
          onthefly = onthefly.replaceFirst("""\d\d""", "MM")
        } else {
          // month following yyyy
          if ("""\D\d\D""".r.findFirstIn(onthefly) != None) {
            onthefly = onthefly.replaceFirst("""\d""", "M")
          } else if ("""\D\d\d""".r.findFirstIn(onthefly) != None) {
            onthefly = onthefly.replaceFirst("""\d\d""", "MM")
          }
        }

        // day
        onthefly = if ("""^M+\D\d\D""".r.findFirstIn(onthefly) != None) {
          onthefly.replaceFirst("""(M\D)\d""", "$1d")
        } else {
          onthefly.replaceFirst("""(M+\D)\d\d""", "$1dd")
        }
        // before continuing, make sure we got years, months and days
        if ("""^\S*\d\S""".r.findFirstIn(onthefly) != None) {
          sys.error(s"problem: onthefly[$onthefly] ($dateStr)")
        }
        onthefly = if ("""\s\d\d""".r.findFirstIn(onthefly) != None) {
          onthefly.replaceFirst("""\d\d""", "hh")
        } else {
          onthefly.replaceFirst("""\d""", "h")
        }
        onthefly = onthefly.replaceFirst("""\d\d""", "mm")
        onthefly = onthefly.replaceFirst("""\d\d""", "ss")
        if ("""\.\d+$""".r.findFirstIn(onthefly) != None) {
          onthefly = onthefly.replaceFirst("""\d+$""", "S")
        }
        if ("""\s[AP]M$""".r.findFirstIn(onthefly) != None) {
          onthefly = onthefly.replaceFirst("""[AP]M$""", "a")
        }
        try {
          val testFormat = new SimpleDateFormat(onthefly)
          mdate = ParsDate(testFormat.parse(dateStr))
          currentFormat = testFormat // successful
          yearFirstFlag = onthefly.startsWith("yyyy")
          if (!newFormats.contains(onthefly)) {
            newFormats += onthefly
            if (verbose) eprintf("onthefly[%s] (%s)\n", onthefly, dateStr)
          }
        } catch {
          case _: Exception =>
          // ee.printStackTrace()
          // fall thru if on-the-fly failed
        }
        mdate
    }
  }

  def barePunctuation(fmt: String): String = fmt.replaceAll("[a-zA-Z0-9]+", "").replaceAll("/", "-")

  def relevantFormats(literalDate: String, yearFirst: Boolean): List[SimpleDateFormat] = {
    val punct = barePunctuation(literalDate)
    val list = if (punctuationMap.contains(punct)) {
      punctuationMap(punct).sortBy { sdf => -sdf.toPattern.length } // longest patterns first
    } else {
      if (ParsDate.debug || ParsDate.verbose) {
        eprintf("no date format for punct[%s], literalDate[%s]\n", punct, literalDate)
      }
      List[SimpleDateFormat]()
    }
    list.filter { yearFirst == _.toPattern.startsWith("y") }
  }

  lazy val punctuationMap: Map[String, List[SimpleDateFormat]] = {
    var uniqMap = Map[String, List[SimpleDateFormat]]()
    dateFormatStrings.map { str =>
      val punct = barePunctuation(str)
      val sfmt  = simpleFormat(str)

      val list = if (uniqMap.contains(punct)) {
        sfmt :: uniqMap(punct)
      } else {
        List(sfmt)
      }
      uniqMap += (punct -> list)
    }
    uniqMap
  }

  def listVariations(orig: String): List[String] = {
    val list1 = if (orig.indexOf(":s") >= 0) {
      val hh  = orig.replaceAll("HH", "hh")
      val hha = hh.replaceFirst("(:s+)", "$1 a")
      val hhb = hh.replaceFirst("(:s+)", "$1.S")
      val hhc = hh.replaceFirst("(:s+)", "$1.S a")

      val origS = orig.replaceAll(":ss", "")
      val hhS   = origS.replaceAll("HH", "hh")
      val hhaS  = hhS + " a"

      List(orig, hh, hha, hhb, hhc, origS, hhS, hhaS)
    } else {
      val hh  = orig.replaceAll("HH", "hh")
      val hha = hh + " a"
      List(orig, hh, hha)
    }
    val list2 = list1.map { _.replaceAll("dd", "d") }
    val list3 = list1.map { _.replaceAll("MM", "M") }
    val list4 = list2.map { _.replaceAll("MM", "M") }

    var bass       = list1 ::: list2 ::: list3 ::: list4
    val shortHours = bass.map { _.replaceAll("HH", "H").replaceAll("hh", "h") }
    bass = bass ::: shortHours
    val list   = bass
    val notime = list.map { _.replaceAll(""" [Hh]\S*""", " ").trim }
    val all    = (list ::: notime).filter(_.nonEmpty)
    val big    = all ::: all.map { _ + " Z" }
    val accum  = big.map { _.replaceAll(" +", " ").trim }.toSet.toList // remove duplicate
    accum.sortWith { (a, b) => (a.length > b.length) }
  }

  object sysTimer {
    var begin         = System.currentTimeMillis
    def reset(): Unit = { begin = System.currentTimeMillis }
    def elapsed: Long = System.currentTimeMillis - begin
    def elapsedMillis = elapsed

    def elapsedSeconds: Double = elapsed.toDouble / 1000.0
    def elapsedMinutes: Double = elapsed.toDouble / (60.0 * 1000.0)
    def elapsedHours: Double   = elapsed.toDouble / (60.0 * 60.0 * 1000.0)
    def elapsedDays: Double    = elapsed.toDouble / (24.0 * 60.0 * 60.0 * 1000.0)
  }

  /**
  * NOTE: only HH variant should appear in this list.
  */
  lazy val baseFormats: List[String] = List(
    // supported input formats, as of 2012-10-04
    "yyyyMMdd",
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd kk:mm:ss", // 24-hour format
    "MM-dd-yyyy HH:mm:ss",
    "MM-dd HH:mm:ss yyyy"
  )

  def dateFormatStrings: List[String] = baseFormats.map { listVariations }.flatten

  def simpleFormat(fmt: String): SimpleDateFormat = new SimpleDateFormat(fmt, Locale.US)

  /// ============================================================== former object Main
  def parse(line: String, zeroTime: Boolean = false): ParsDate = {
    val simplified = line.replaceAll("""[\s\(\),]+""", " ").trim

    simplified match {
    case DateRegex_01(dystr, moName, yr, time, tz @ _) =>
      val (yyyy, mm, dd) = getNumbers(yr, moName, dystr)
      val tm             = if (zeroTime) "00:00:00" else time
      val stdfmt         = "%4d/%02d/%02d %s".format(yyyy, mm, dd, tm)
      // eprintf("dystr:[%s], moName:[%s], yr:[%s], time:[%s], tz:[%s], stdfmt[%s]".format(dystr, moName, yr, time, tz, stdfmt))
      ParsDate(stdfmt)

    case DateRegex_02(dummy @ _, dystr, moName, yr, time, tz @ _) =>
      val (yyyy, mm, dd) = getNumbers(yr, moName, dystr)
      val tm             = if (zeroTime) "00:00:00" else time
      val stdfmt         = "%4d/%02d/%02d %s".format(yyyy, mm, dd, tm)
      // eprintf("dystr:[%s], moName:[%s], yr:[%s], time:[%s], tz:[%s], stdfmt[%s] (%s)".format(dystr, moName, yr, time, tz, stdfmt, dummy))
      ParsDate(stdfmt)

    case other =>
      sys.error(s"unparseable date:[$other]")
    }
  }
  def getNumbers(yr: String, moName: String, dy: String): (Int, Int, Int) = {
    def prep(str: String): Int = str.stripPrefix("0").toInt
    (prep(yr), monthNumber(moName), prep(dy))
  }
  // ==================================
  def monthNumber(monthName: String): Int = {
    monthName.trim.toLowerCase.substring(0, 3) match {
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
  lazy val DayNames   = """([smtwf][uoehra][neduit])"""
  lazy val MonthNames = """([jfmasond][aepuco][nbrynlgptvc])"""
  lazy val ZoneHours  = """([-+]\d{4})"""
  lazy val TimeRegex  = """(\d{2}:\d{2}:\d{2})"""
  lazy val YearRegex  = """([12][01]\d{2})"""
  lazy val DayNumber  = """(\d{1,2})"""
  lazy val Spc        = """[\s+]"""

  // format: off
  lazy val DateRegex_01: Regex = ("(?i).*"+              DayNumber+Spc+MonthNames+Spc+YearRegex+Spc+TimeRegex+Spc+ZoneHours+".*").r
  lazy val DateRegex_02: Regex = ("(?i).*"+ DayNames+Spc+DayNumber+Spc+MonthNames+Spc+YearRegex+Spc+TimeRegex+Spc+ZoneHours+".*").r
  // format: on

  // import java.time.LocalDateTime
  // type DateTime = java.time.LocalDateTime
  def quikDate(yyyyMMdd: String): DateTime = {
    assert(yyyyMMdd.matches("2[0-9]{7}"), s"bad date [$yyyyMMdd], expecting yyyyMMdd")
    val y = yyyyMMdd.take(4).toInt
    val m = yyyyMMdd.drop(4).take(2).toInt
    val d = yyyyMMdd.drop(6).take(2).toInt
    java.time.LocalDateTime.of(y, m, d, 0, 0, 0)
  }
  def ymdDate: String => DateTime = quikDate // alias
}

class ParsDate(msec: Long) extends Ordered[ParsDate] {
  import ParsDate.*

  var zone: String       = ""
  var outfmt: DateFormat = ParsDate.outfmt // inherit the current default

  // return self, to permit this usage:  date.dateOnly.toString
  def dateAndTime: ParsDate = {
    outfmt = dateTimeFormat
    this
  }

  // TODO: this sets global mode for default printing of date format
  def dateOnly: ParsDate = {
    outfmt = dateOnlyFormat
    this
  }

  private val cal = java.util.Calendar.getInstance
  cal.setTimeInMillis(msec)
  private val date = cal.getTime

  val stringValue: String = dateTimeFormat.format(date).replaceAll("""\s+00:00:00""", "")

  def toTuple: ((Int, Int, Int), (Int, Int, Int)) = {
    val date = (getYear, getMonth, getDay)
    val time = (getHour, getMinute, getSecond)
    (date, time)
  }

  def getTime: Long = date.getTime

  // alias
  def getEpoch: Long = date.getTime

  def toDate: Date = new Date(getEpoch)

  def copyCalendar(): Calendar = {
    val tcal = java.util.Calendar.getInstance
    tcal.setTimeInMillis(getEpoch)
    tcal
  }
//  def compareTo(that: ParsDate): Int = {
//    if (this < that) -1
//    else if (this > that) 1
//    else 0
//  }
//  def compare(x: ParsDate, y: ParsDate) = x compareTo y
  def compare(that: ParsDate): Int = this compareTo that

  def isLeapYear: Boolean = gcal.isLeapYear(getYear)

//  def < (other: ParsDate): Boolean = { getTime <  other.getTime }
//  def <= (other: ParsDate): Boolean = { getTime <= other.getTime }
//  def > (other: ParsDate): Boolean = { getTime >  other.getTime }
//  def >= (other: ParsDate): Boolean = { getTime >= other.getTime }

  override def toString: String = {
    outfmt.format(date).replaceAll("""\s+00:00:00""", "")
  }
  def toString(fmt: String, locale: Locale = Locale.US): String = {
    val fixfmt         = fmt.replace("T", " ")
    val df: DateFormat = new SimpleDateFormat(fixfmt, locale)
    df.format(date)
  }

  def nextDay: ParsDate = addDays(1)

  def addMilliseconds(milliseconds: Int): ParsDate = ParsDate(getEpoch + milliseconds)

  def addSeconds(seconds: Int): ParsDate = addMilliseconds(seconds * 1000)
  def addMinutes(minutes: Int): ParsDate = addSeconds(minutes * 60)
  def addHours(hours: Int): ParsDate     = addMinutes(hours * 60)

  def between(a: ParsDate, b: ParsDate): Boolean = {
    assert(a <= b)
    this >= a && this <= b
  }

  def addDays(days: Int): ParsDate = {
    val tcal = copyCalendar()
    tcal.add(Calendar.DAY_OF_YEAR, days)
    ParsDate(tcal.getTime)
  }
  // time elapsed since previousTime
  def elapsedMilliSeconds(previousTime: ParsDate): Long = {
    val t0: Long = previousTime.getTime
    val t1: Long = this.getTime
    if (t0 > t1) {
      t0 - t1
    } else {
      t1 - t0
    }
  }
  def elapsedSeconds(previousTime: ParsDate): Long = {
    elapsedMilliSeconds(previousTime) / 1000
  }
  def elapsedMinutes(previousTime: ParsDate): BigDecimal = {
    BigDecimal(elapsedSeconds(previousTime) / 60.0)
  }
  def elapsedHours(previousTime: ParsDate): BigDecimal = {
    elapsedMinutes(previousTime) / 60.0
  }
  def elapsedDays(previousTime: ParsDate): BigDecimal = {
    elapsedHours(previousTime) / 24.0
  }

  // duration methods that return approximate answers
  lazy val averageMonthSize: Double = (31 + 28.25 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30 + 31) / 12.0
  def elapsedMonths(previousTime: ParsDate): BigDecimal = {
    elapsedDays(previousTime) / averageMonthSize
  }
  def elapsedYears(previousTime: ParsDate): BigDecimal = {
    elapsedDays(previousTime) / 365.25
  }

  def toLongString: String = {
    dateTimeFormat.format(this)
  }
  // this would be an override, if extending Date
  def getYear: Int = {
    cal.get(Calendar.YEAR)
  }

  // @return 1-based month rather than zero based.
  // this would be an override, if extending Date
  def getMonth: Int = {
    cal.get(Calendar.MONTH) + 1 // compatible with java.util.Date
  }
  def getDayOfMonth: Int = getDay

  // this would be an override, if extending Date
  def getDay: Int = {
    cal.get(Calendar.DAY_OF_MONTH) // compatible with java.util.Date
  }
  def dayOfWeek: Int = {
    cal.get(Calendar.DAY_OF_WEEK)
  }
  def dayOfWeekName: String = {
    cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
  }

  def getHour: Int = {
    cal.get(Calendar.HOUR_OF_DAY)
  }
  def getMinute: Int = {
    cal.get(Calendar.MINUTE)
  }
  def getSecond: Int = {
    cal.get(Calendar.SECOND)
  }
}

object LongIso {
  // Print date in standard sortable long-iso format.
  def main(args: Array[String]): Unit = {
    try {
      for (arg <- args) {
        printf("%s\n", ParsDate(arg))
      }
    } catch {
      case ee: Exception =>
        showLimitedStack(ee)
    }
  }
}
