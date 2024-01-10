#!/usr/bin/env -S scala -deprecation
package vastblue.time

import vastblue.pallet.*
import vastblue.time.TimeDate.*
import java.time.LocalDateTime

object ChronoParse {
  // by default, prefer US format, but swap month and day if unavoidable
  // (e.g., 24/12/2022 incompatible with US format, not with Int'l format
  var monthFirst = true

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
    try {
      op match {
      case "" | "-test" | "-flds" =>
        verifyFields(testDataFile)
      case "-file" =>
        convertTimestamps(infiles)
      case _ =>
        usage(s"op == $op")
      }
    } catch {
    case t: Throwable =>
      showLimitedStack(t)
      sys.exit(3)
    }
  }

  def verifyFields(p: Path): Unit = {
    if (p.isFile) {
      val rows = p.csvRows
      printf("%d rows\n", rows.size)
      hook += 1
      for ((row, i) <- rows.zipWithIndex) {
        if (i > 0) {
          // skip headings row, preserve file row numbers
          val rawline = row.toList match {
            case _ :: targetstr :: Nil =>
              targetstr
            case targetstr :: Nil =>
              targetstr
            case _ =>
              hook += 1
              ""
          }
          if (verbose) printf("%04d : %s\n", i, row.mkString("|"))

          val format = DateFlds(rawline)
          if (verbose) {
            printf("rawlin: [%s]\n", rawline)
            printf("   format: [%s]\n", format.toString)
          } else {
            printf("%-22s, \"%s\"\n", format.toString, rawline)
          }
        }
      }
    }
  }

  def convertTimestamps(files: Seq[Path]): Seq[DateFlds] = {
    for {
      p <- files
      if p.isFile
      (row, i) <- p.csvRows.zipWithIndex
      if i > 0 // skip headings row
      rawline = row.toList match {
        case targetstr :: Nil =>
          targetstr // if 1 column, convert it
        case _ :: targetstr :: Nil =>
          targetstr // if 2 columns, convert 2nd
        case _ =>
          ""
      }
      if rawline.nonEmpty
      dateflds = DateFlds(rawline)
      if dateflds.valid
    } yield dateflds
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

  lazy val testDataFile = "testdates.csv".path
  lazy val nowdate = now.toString("yyyy-MM-dd")
}

// derive parse format for dates with numeric fields
// preparation must include converting names of day and month to numeric
object DateFlds {
  lazy val TimeZoneSplitter  = "(.*:.*) ?([-+][0-9]{1,2}:00)$".r
  lazy val BadDate: DateTime = dateParser("1900-01-01")

  def apply(rawdatetime: String): DateFlds = {
    var valid = true
    val numerified = numerifyNames(rawdatetime) // toss weekday name, convert month name to number
 
    // TODO: split into time and date near the outset, handle each separately
    var (datetime, timezone) = numerified match {
    case TimeZoneSplitter(time, zone) =>
      (time, zone)
    case str =>
      (str, "")
    }
    // TODO: use timezone info, including CST, etc

    /*
    val timezone = datetime.replaceAll(".*:.* ?([-+][0-9]{1,2}:00)$", "$1").trim
    if (timezone.length < datetime.length) {
      datetime = datetime.stripSuffix(timezone).trim
    }
    */
    var numstrings = datetime.replaceAll("\\D+", " ").trim.split(" ").toIndexedSeq
    val widenums = numstrings.filter { _.length >= 4 }
    widenums.toList match {
    case Nil => // no wide num fields
    case year :: _ if year.length == 4 =>
      val i = numstrings.indexOf(year)
      if (i > 3) {
        val (left, rite) = numstrings.splitAt(i)
        val newnumstrings = Seq(year) ++ left ++ rite.drop(1)
        numstrings = newnumstrings.toIndexedSeq
      }
      hook += 1
    case ymd :: _ =>
      hook += 1       // maybe 20240213 or similar
      var (y, m, d) = ("", "", "")
      if (ymd.startsWith("2") && ymd.length == 8) {
        // assume yyyy/mm/dd
        y = ymd.take(4)
        m = ymd.drop(4).take(2)
        d = ymd.drop(6)
      } else if (ymd.drop(4).matches("2[0-9]{3}") ){
        if (monthFirst) {
          // assume mm/dd/yyyy
          m = ymd.take(2)
          d = ymd.drop(2).take(2)
          y = ymd.drop(4)
        } else {
          // assume dd/mm/yyyy
          d = ymd.take(2)
          m = ymd.drop(2).take(2)
          y = ymd.drop(4)
        }
      }
      val newymd = Seq(y, m, d)
      val newnumstrings: Seq[String] = {
        val head: String = numstrings.head
        if (head == ymd) {
          val rite: Seq[String] = numstrings.tail
          val (mid: Seq[String], tail: Seq[String]) = rite.splitAt(1)
          val hrmin: String = mid.mkString
          if (hrmin.matches("[0-9]{3,4}")) {
            val (hr: String, min: String) = hrmin.splitAt(hrmin.length-2)
            val hour = if (hr.length == 1) {
              s"0$hr"
            } else {
              hr
            }
            val lef: Seq[String] = newymd
            val mid: Seq[String] = Seq(hour, min)
            newymd ++ mid ++ tail
          } else {
            newymd ++ rite
          }
        } else {
          val i = numstrings.indexOf(ymd)
          val (left, rite) = numstrings.splitAt(i)
          val newhrmin = rite.drop(1)
          left ++ newymd ++ newhrmin
        }
      }
      numstrings = newnumstrings.toIndexedSeq
    }
    
    var nums = numstrings.map { ti(_) }
    val timeOnly: Boolean = numstrings.size <= 4 && rawdatetime.matches("[0-9]{2}:[0-9]{2}.*")
    if ( !timeOnly ) {
      def adjustYear(year: Int): Unit = {
        nums = nums.take(2) ++ Seq(year) ++ nums.drop(3)
        numstrings = nums.map {
          _.toString
        }
      }
      val dateFields = nums.take(3)
      dateFields match {
      case Seq(a, b, c) if a > 31 || b > 31 || c > 31 =>
        hook += 1 // the typical case where 4-digit year is provided
      case Seq(a, b) =>
        // the problem case; assume no year provided
        adjustYear(now.getYear) // no year provided, use current year
      case Seq(mOrD, dOrM, relyear) =>
        // the problem case; assume M/d/y or d/M/y format
        val y = now.getYear
        val century = y - y % 100
        adjustYear(century + relyear)
      case _ =>
        hook += 1 // huh?
      }
    }
    
    val fields: Seq[(String, Int)] = numstrings.zipWithIndex
    var (yval, mval, dval) = (0, 0, 0)
    val farr = fields.toArray
    var formats: Array[String] = farr.map { (s: String, i: Int) =>
      if (i < 3 && !timeOnly) {
        ti(s) match {
        case y if y > 31 || s.length == 4 =>
          yval = y
          s.replaceAll(".", "y")
        case d if d > 12 && s.length <= 2 =>
          dval = d
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
    def indexOf(s: String): Int = {
      formats.indexWhere((fld: String) =>
        fld.startsWith(s)
      )
    }
    def numIndex: Int = {
      formats.indexWhere((s: String) => s.matches("[0-9]+"))
    }
    def setFirstNum(s: String): Int = {
      val i = numIndex
      val numval = formats(i)
      val numfmt = numval.replaceAll("[0-9]", s)
      formats(i) = numfmt
      ti(numval)
    }
    // if two yyyy-MM-dd fields already fixed, the third is implied
    formats.take(3).map { _.distinct }.sorted match {
      case Array(_, "M", "y") => dval = setFirstNum("d")
      case Array(_, "d", "y") => mval = setFirstNum("M")
      case Array(_, "M", "d") => yval = setFirstNum("y")
      case _arr =>
        hook += 1 // more than one numeric fields, so not ready to resolve
    }
    hook += 1
    def is(s: String, v: String): Boolean = s.startsWith(v)

    val yidx = indexOf("y")
    val didx = indexOf("d")
    val midx = indexOf("M")

    def hasY = yidx >= 0
    def hasM = midx >= 0
    def hasD = didx >= 0
    def needsY = yidx < 0
    def needsM = midx < 0
    def needsD = didx < 0

    def replaceFirstNumericField(s: String): Unit = {
      val i = numIndex
      if (i < 0) {
        hook += 1 // no numerics found
      } else {
        assert(i >= 0 && i < 3, s"internal error: $datetime [i: $i, s: $s]")
        s match {
          case "y" =>
            assert(yval == 0, s"yval: $yval")
            yval = ti(formats(i))
          case "M" =>
            assert(mval == 0, s"mval: $mval")
            mval = ti(formats(i))
          case "d" =>
            if (dval > 0) {
              hook += 1
            }
            assert(dval == 0, s"dval: $dval")
            dval = ti(formats(i))
          case _ =>
            sys.error(s"internal error: bad format indicator [$s]")
        }
        setFirstNum(s)
      }
    }

    val needs = Seq(needsY, needsM, needsD)
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
      case 1 =>
        // might as well support bizarre formats (M-y-d or d-M-y)
        if (monthFirst) {
          replaceFirstNumericField("M")
          replaceFirstNumericField("d")
        } else {
          replaceFirstNumericField("d")
          replaceFirstNumericField("M")
        }
      case 0 | 2 =>
        // y-M-d
        if (monthFirst) {
          replaceFirstNumericField("M")
          replaceFirstNumericField("d")
        } else {
          replaceFirstNumericField("d")
          replaceFirstNumericField("M")
        }

      }
    case (true,  true, false) =>
      // has day, needs month and year
      didx match {
      case 0 =>
        // d-M-y
        replaceFirstNumericField("M")
        replaceFirstNumericField("y")
      case 2 =>
        // y-M-d
        replaceFirstNumericField("y")
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
    case (false, false, false) =>
      hook += 1 // done
    case (true, true, true) if timeOnly =>
      hook += 1 // done
    case (yy, mm, dd) =>
      formats.toList match {
      case a :: b :: Nil =>
        val (ta, tb) = (ti(a), ti(b))
        // interpret as missing day or missing year
        // missing day if either field is > 31
        if (monthFirst && ta <= 12) {
          mval = ta
          dval = tb
        } else {
          mval = tb
          dval = tb
        }
        if (mval > 31) {
          // assume day is missing
          yval = mval
          mval = dval
          dval = 1 // convention
        } else if (dval > 31) {
          // assume day is missing
          yval = dval
          dval = 1 // convention
        } else {
          if (mval > 12) {
            // the above swap might make this superfluous
            // swap month and day
            val temp = mval
            mval = dval
            dval = temp
          }
          yval = now.getYear // supply missing year
        }
        // TODO: reorder based on legal field values, if appropriate
        formats = Array("yyyy", "MM", "dd")
        numstrings = IndexedSeq(yval, mval, dval).map { _.toString }
      case _ =>
        sys.error(s"yy[$yy], mm[$mm], dd[$dd] datetime[$datetime], formats[${formats.mkString("|")}]")
      }
    }
    if (numstrings.endsWith("2019") ){
      hook += 1
    }

    def fromStandardOrder(so: List[Int]): LocalDateTime = {
      so match {
      case yr :: mo :: dy :: hr :: mn :: sc :: nano :: Nil =>
        LocalDateTime.of(yr, mo, dy, hr, mn, sc, nano)
      case yr :: mo :: dy :: hr :: mn :: sc :: Nil =>
        if (sc > 59 || mn > 59 || hr > 59) {
          hook += 1
        }
        LocalDateTime.of(yr, mo, dy, hr, mn, sc)
      case yr :: mo :: dy :: hr :: mn :: Nil =>
        LocalDateTime.of(yr, mo, dy, hr, mn, 0)
      case yr :: mo :: dy :: hr :: Nil =>
        LocalDateTime.of(yr, mo, dy, hr, 0, 0)
      case yr :: mo :: dy :: Nil =>
        if (mo > 12) {
          hook += 1
        }
        LocalDateTime.of(yr, mo, dy, 0, 0, 0)
      case other =>
        sys.error(s"not enough date-time fields: [${so.mkString("|")}]")
      }
    }

    val bareformats = formats.map { _.distinct }.toList
    nums = numstrings.map { ti(_) }.toIndexedSeq
    def ymd(iy: Int, im: Int, id: Int, tail: List[String]): LocalDateTime = {
      if (iy <0 || im <0 || id <0) {
        hook += 1
      } else if (nums.size < 3) {
        hook += 1
      }
      val standardOrder = List(nums(iy), nums(im), nums(id)) ++ nums.drop(3)
      fromStandardOrder(standardOrder)
    }
    val dateTime: LocalDateTime = bareformats match {
      case "d" :: "M" :: "y" :: tail => ymd(2,1,0, tail)
      case "M" :: "d" :: "y" :: tail => ymd(2,0,1, tail)
      case "d" :: "y" :: "M" :: tail => ymd(1,2,0, tail)
      case "M" :: "y" :: "d" :: tail => ymd(1,0,2, tail)
      case "y" :: "d" :: "M" :: tail => ymd(0,2,1, tail)
      case "y" :: "M" :: "d" :: tail => ymd(0,1,2, tail)
      case other =>
        valid = false
        BadDate
    }
    new DateFlds(dateTime, rawdatetime, numerified, timezone, formats, valid)
  }
}

case class DateFlds(dateTime: LocalDateTime, rawdatetime: String, numerified: String, timezone: String, formats: Seq[String], valid: Boolean) {
  override def toString: String = dateTime.toString("yyyy-MM-dd HH:mm:ss")
}
