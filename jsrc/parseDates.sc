#!/usr/bin/env -S scala-cli shebang -deprecation

//> using dep "org.vastblue:pallet_3:0.10.6"
//> using dep "org.vastblue:unifile_3:0.3.0"
//> using dep "org.simpleflatmapper:sfm-csv-jre6:8.2.3"
//> using dep "io.github.chronoscala::chronoscala::2.0.10"
//> using dep "com.github.sisyphsu:dateparser:1.0.11"

import vastblue.pallet.*
import vastblue.time.TimeDate.*
import vastblue.time.TimeParser
import com.github.sisyphsu.dateparser.*
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

ParseDates.main(args)
object ParseDates {
  var monthFirst = true // by default, prefer US format

  def usage(m: String=""): Nothing = {
    _usage(m, Seq(
      "[<inputFile>]   ; one datetime string per line",
      "[-test | -flds] ; verify testdate.csv conversions",
      "[-df]           ; prefer day-first format (non-US)",
      "by default, op == \"-flds\"",
    ))
  }
  var (op, verbose, infiles) = ("", false, Vector.empty[Path])

  def main(args: Array[String]): Unit = {
    parseArgs(args.toSeq)
    DateParserUtils.preferMonthFirst(monthFirst)
    try {
      op match {
      case "" =>
        verifyFields(testDataFile)
      case "-test" =>
        verifyConversions(testDataFile)
      case "-file" =>
        for (p <- infiles) {
          convertEntries(p)
        }
      }
    } catch {
    case t: Throwable =>
      showLimitedStack(t)
      sys.exit(3)
    }
  }

  def parseArgs(args: Seq[String]): Unit = {
    eachArg(args.toSeq, usage) {
    case f if f.path.isFile =>
      assert(op.isEmpty, s"op[$op] but also specified file [$f]")
      op = "-file"
      infiles :+= f.path
    case "-v" =>
      verbose = true
    case "-df" =>
      monthFirst = false
    case "-test" | "-flds" =>
      if (!testDataFile.isFile) {
        usage(s"not found: ${testDataFile.posx}")
      } else {
        op = thisArg
      }
    case arg =>
      usage(s"unrecognized arg [$arg]")
    }
  }
  def verifyFields(file: Path): Unit = {
    if (file.isFile) {
      val rows = file.csvRows.drop(1) // discard column names
      eprintf("%d rows\n", rows.size)
      for ((row, i) <- rows.zipWithIndex){
        printf("%04d : %s\n", i, row.mkString("|"))
        val Seq(expected, rawline) = row
        val format = DateFlds(rawline)
        printf("rawlin: [%s]\n", rawline)
        printf("   format: [%s]\n", format)
        printf("   expect: [%s]\n", expected)
      }
    }
  }
  def verifyConversions(file: Path): Unit = {
    if (file.isFile) {
      val rows = file.csvRows.drop(1) // discard column names
      for (row <- rows){
        val Seq(expected, rawline) = row
        val dateTime = newDateParser(rawline)
        val dtstr = dateTime.toString("yyyy-MM-dd HH:mm:ss")
        if (dtstr != expected) {
          printf("rawlin: [%s]\n", rawline)
          printf("   result: [%s]\n", dtstr)
          printf("   expect: [%s]\n", expected)
        }
        //printf("%s # [%s]\n", dtstr, rawline)
      }
    }
  }
  def convertEntries(file: Path): Unit = {
    if (file.isFile) {
      for (rawline <- file.lines) {
        val dateTime = newDateParser(rawline)
        val dtstr = dateTime.toString("yyyy-MM-dd HH:mm:ss")
        printf("%s # [%s]\n", dtstr, rawline)
      }
    }
  }
  lazy val testDataFile = "testdates.csv".path

  lazy val nowdate = now.toString("yyyy-MM-dd")

  def newDateParser(rawline: String): LocalDateTime = {
    try {
      sysiphus(rawline)
    } catch {
    case e: java.time.format.DateTimeParseException =>
      var line = rawline.replaceAll("/{2,}", "/").replaceAll("[\"\\\\]", "")
      parseDate(line)
    }
  }
  /*
  def fieldTypes(numstrings: List[String]): String = {
    val nums: Seq[Int] = numstrings.map { (s: String) => Fld(s) }
    val fields: Seq[(String, Int)] = numstrings.zipWithIndex.sortBy { case (str, i) => str }
    nums match {
    case a :: b :: c :: Nil if a > 1000 =>
      "yyyyMMdd"
    case a :: b :: c :: Nil if a > 12 && c > 1000 =>
      "ddMMyyyy"
    case a :: b :: c :: Nil if c > 1000 =>
      "MMddyyyy"
    case a :: b :: c :: Nil if a > 12 && c > 1000 =>
      "ddMMyyyy"
    } else if (num > 12) {
      "d" // day
    } else {
      "m" // resolve ambiguity in favor of month
    }
  }
  */
  def sysiphus(rawline: String): LocalDateTime = {
    var line = rawline
    val numstrings = line.split("\\D+").map { _.trim }.filter { _.nonEmpty }
    val nums = numstrings.map { _.toInt }
    if (verbose) {
      printf("%d number fields [%s]\n", numstrings.size, numstrings.mkString("|"))
    }
    if (numstrings.length == 3) {
      if (line.contains(":")) {
        // time fields only
        DateParserUtils.parseDateTime(s"$nowdate $line")
      } else {
        // date fields only
        val Seq(a, b, c) = nums.toSeq
        if (a < 1000) {
          // year-first
        } else if (a > 12) { 
          // day-first, swap first two fields
          line = "%02d-%02d-%04d".format(b, a, c)
        } else {
          // assume month-first, but is ambiguous
        }
        DateParserUtils.parseDateTime(s"$line 00:00:00")
      }
    } else {
      if (verbose) {
        eprintf("== [%s]\n", rawline)
      }
      DateParserUtils.parseDateTime(line)
    }
  }
 
  // derive parse format for dates with numeric fields
  // preparation must include converting names of day and month to numeric
  case class DateFlds(rawdatetime: String) {
    val datetime = numerifyNames(rawdatetime)
    val numstrings = datetime.replaceAll("\\D+", " ").trim.split(" ").toIndexedSeq
    val fields: Seq[(String, Int)] = numstrings.zipWithIndex
    def hasTime = datetime.contains(":")
    def hasDate = datetime.contains("-") || datetime.contains("/")

    var formats: Array[String] = fields.toArray.map { (s: String, i: Int) =>
      if (i < 3) {
        s.toInt match {
        case y if y > 31 || s.length == 4 =>
          s.replaceAll(".", "y")
        case d if d > 12 && s.length <= 2 =>
          s.replaceAll(".", "d")
        case _ => // can't resolve month without more context
          s
        }
      } else {
        i match {
        case 3 => s.replaceAll(".", "H")
        case 4 => s.replaceAll(".", "m")
        case 5 => s.replaceAll(".", "s")
        case 6 => s.replaceAll(".", "Z")
        case _ =>
          s // not expecting any more numeric fields
        }
      }
    }

    def is(s: String, v: String): Boolean = s.startsWith(v)

    val yidx = formats.indexWhere((s: String) => s.startsWith("y"))
    val didx = formats.indexWhere((s: String) => s.startsWith("d"))
    val midx = formats.indexWhere((s: String) => s.startsWith("M"))

    def hasY = yidx >= 0
    def hasM = midx >= 0
    def hasD = didx >= 0
    def needsY = yidx < 0
    def needsM = midx < 0
    def needsD = didx < 0

    var (yval, mval, dval) = (0, 0, 0)
    def replaceFirstNumericField(s: String): Unit = {
      val i = formats.indexWhere((s: String) => s.matches("[0-9]+"))
      assert(i < 3, s"internal error: $datetime [i: $i, s: $s]")
      s match {
      case "y" =>
        assert(yval < 1, s"yval: $yval")
        yval = formats(i).toInt
      case "M" =>
        assert(mval < 1, s"mval: $mval")
        mval = formats(i).toInt
      case "d" =>
        assert(dval < 1, s"dval: $dval")
        dval = formats(i).toInt
      case _ =>
        sys.error(s"internal error: bad format indicator [$s]")
      }
      formats(i) = formats(i).replaceAll("[0-9]", s)
    }

    (needsY, needsM, needsD) match {
    case (false, false, true) =>
      replaceFirstNumericField("d")
    case (false, true, false) =>
      replaceFirstNumericField("M")
    case (true, false, false) =>
      replaceFirstNumericField("y")

    case (false, true,  true) =>
      // has year, needs month and day
      yidx match {
      case 0 =>
        // y-M-d
        replaceFirstNumericField("M")
        replaceFirstNumericField("d")
      case 2 =>
        // d-M-y
        replaceFirstNumericField("d")
        replaceFirstNumericField("M")
      }
    case (true,  true, false) =>
      // has day, needs month and year
      didx match {
      case 0 =>
        // d-M-y
        replaceFirstNumericField("M")
        replaceFirstNumericField("d")
      case 2 =>
        // y-M-d
        replaceFirstNumericField("d")
        replaceFirstNumericField("M")
      case 1 =>
        // AMBIGUOUS ...
        if (monthFirst) {
          // M-d-y
          replaceFirstNumericField("d")
          replaceFirstNumericField("M")
        } else {
          // d-M-y
          replaceFirstNumericField("M")
          replaceFirstNumericField("d")
        }
      }
    case (true, true, true) =>
      // done with date fields
    case (yy, mm, dd) =>
      sys.error(s"yy[$yy], mm[$mm], dd[$dd] datetime[$datetime], formats[${formats.mkString("|")}]")
    }
    override def toString = "%s : %s".format(formats.mkString("|"), datetime)

    /*
    case (true, false, false) =>

    case (false, false, true) =>
      formats(i) = m.map { _ => "M" }
    case (m :: d :: y :: tail), i) if hasY && hasM =>
      formats(i) = d.map { _ => "d" }
    case (m :: d :: y :: tail, i) if hasY =>
      // by convention (in US):
      formats(0) = m.replaceAll(".", "M")
      formats(1) = d.replaceAll(".", "d")

    case m :: d :: y :: tail if !hasY =>
      formats(2) = m.map { _ => "M" }
    case m :: d :: y :: tail if hasY && hasM =>
      formats(1) = d.map { _ => "d" }
    case m :: d :: y :: tail if hasY =>
      // by convention (in US):
      formats(0) = m.replaceAll(".", "M")
      formats(1) = d.replaceAll(".", "d")

    case m :: d :: y if y.startsWith("y") :: tail =>
      formats(0) = m.replaceAll(".", "M")
      formats(1) = d.replaceAll(".", "M")
    */
    /*
    def hasy = fmts.contains("y")
    for ((str, idx) <- fields) {
      val width = str.length
      val valu = str.toInt
      (valu, idx) match {
      case (y, 0 | 2) if y > 31 || width == 4 =>
        fmts(idx) = "y" * width

      case (d, _) if d > 12 && width <= 2 =>
        fmts(idx) = "d" * width

      case (m, 0 | 1) if m < 31 && width <= 2 =>
        fmts(idx) = "M" * width

      case unk =>
        if (width >= 4) {
          "Z"
        } else {
          sys.error(s"datetime[$datetime] unknown field: [$unk]")
        }
      }
    }

    override def toString: String = {
      if (fmts.size <= 3) {
        val delim = if (hasTime) { ":" } else { "-" }
        fmts.mkString(delim)
      } else {
        val (dateff, timeff) = fmts.splitAt(3)
        val datefmt = dateff.mkString("-")
        val timefmt = timeff.mkString(":")
        s"$datefmt $timefmt"
      }
    }
    */
  }
}
