package vastblue.util

import vastblue.pallet.*
import vastblue.time.TimeDate.*
import scala.util.matching.Regex
import scala.math.BigDecimal
import java.math.MathContext

object DataTypes {
  type Big = BigDecimal
  var abbrevNums = false
  def Big(bd: java.math.BigDecimal):Big = BigDecimal(bd)
  def Big(x: scala.math.BigInt):Big = BigDecimal(x)
  def Big(x: String):BigDecimal = BigDecimal(x)
  def Big(x: Array[Char]):Big = BigDecimal(x)
  def Big(d: Double):Big = BigDecimal(d)
  def Big(l: Long):Big = BigDecimal(l)
  def Big(i: Int):Big = BigDecimal(i)
  lazy val BadNum:Big = Big("-0.0000000123456789") // sentinel to represent "N/A"
  lazy val BigZero:Big = Big(0)
  lazy val BigOne:Big = Big(1)
  private var hook = 0
  private def eprintf(fmt: String, xs: Any*):Unit = {
    System.err.print(fmt.format(xs*))
  }
  private lazy val zebug = System.getenv("DEBUG") != null //|| ueExists(".debug")

  lazy val NumPattern1: Regex = """(?i)([-\(]\s*)?(\d[\.\d,]+)[%\)]?[%KMB]?""".r
  lazy val NumPattern2: Regex = """(-?\s*[\.\d,]+)(E-?\d+)([%\)]?)([%KMB]?)""".r
  lazy val NumPattern3: Regex = """-?(\d+)([%KMB]?)""".r
  lazy val NumPattern4: Regex = """-?(\d+)(.?[0-9]*E[-+][0-9]+)?""".r

  def str2num(_rtext:String):Big = {
    if ( !_rtext.trim.forall((c: Char) => validNumchar(c)) ){
      BadNum
    } else {
      val rtext = _rtext.replaceAll("^[^-\\.\\d]+","").replaceAll("[$,]","")
      val rawtext = rtext.startsWith(".") match {
      case true => "0"+rtext
      case false => rtext
      }
      val num = {
        var bignum = BadNum
        def numlike = rawtext.filter { (c: Char) => validNumchar(c) }
        if (rawtext.nonEmpty && numlike.length == rawtext.length) {
          val nopct = rawtext.replaceAll("%","")
          bignum = try {
            val bnum = Big(nopct)
            if (nopct != rawtext){
              bnum / Hundred
            } else {
              bnum
            }
          } catch {
          case _:NumberFormatException =>
            BadNum
          }
          // getMostSpecificType(rawtext)
        } else {
          rawtext
        }
        bignum
      }
      num match {
      case b:Big =>
        b
      }
    }
  }
  lazy val Hundred = Big(100)
  def validNumchar(c: Char): Boolean = {
    (c>='0' && c<='9') || c=='.' || c == '-' || c == 'E' || c == 'e' || c == '+' || c == '%' || c == '$' || c==','
  }
  def isNumeric(col:String): Boolean = {
    val s = col.trim
    try {
      s.nonEmpty && {
        val numsAndSuch = s.filter{ (c: Char) => validNumchar(c) }
        s.length == numsAndSuch.length &&
        numsAndSuch.filter{(c: Char) => c=='-'||c=='/'}.size <= 1 && {
          s.toDouble
          true
        }
      }
    } catch {
    case e:Exception =>
      s match {
      case NumPattern1(_,_) =>
        true
      case NumPattern2(_,_,_,_) =>
        true
      case NumPattern3(_,_) =>
        true
      case NumPattern4(_,_) =>
        true
      case s =>
        false
      }
    }
  }

  def getMostSpecificType[T <: Ordered[T]](rawcol: String): String | Big | DateTime = {
    var col = rawcol.replaceAll("""[\$]""","").trim
    if( zebug )
      printf("# rawcol[%s]\n",rawcol)
    val valu:AnyRef = if (col.trim.isEmpty) col else if( isNumeric(col) ){
      if( zebug ) printf("Numeric match\n")

      var negative = false
      var percent = false
      var factor = 1

      col = col.replaceAll(",","") // remove commas
      if( col.contains("%") ){
        percent = true
        col = col.replaceAll("%","")
      }
      def consumeFactor(multiplier:Int):Unit = {
        col = col.substring(0,col.length-1) // consume suffix
        factor = multiplier
      }
      col.toLowerCase.charAt(col.length-1) match {
      case 'k' => consumeFactor(1000)
      case 'm' => consumeFactor(1000000)
      case 'b' => consumeFactor(1000000000)
      case _ => // no multiplier
      }
      if( col.startsWith("(") && col.endsWith(")") ){
        negative = true
        col = col.substring(1,col.length-1).trim
      } else if( col.startsWith("-") ){
        negative = true
        col = col.substring(1).trim
      }
      var numval = try {
        Big(col)
      } catch {
        case nfe:NumberFormatException =>
          //eprintf(s"bad number string: [${col}]")
          //throw nfe
          BadNum // bad format equates to BadNum
      }
      if( negative ) numval *= Big("-1")
      if( percent ) {
        numval /= Big("100")
      }
      numval *= factor
      numval // Big
    } else if (col.length < 7) {
      col // not a date
    } else {
      try {
        dateParser(col)
      } catch {
      case _ : Exception =>
        col // String
      }
    }
    valu match {
      case num: Big => num
      case str: String => str
      case date: DateTime =>
        date
      case _ =>
        hook += 1
        valu.toString
    }
  }

  def big(str:String): Big = str2num(str)

  def big(it:Any):Big = it match {
  case bd:BigDecimal =>
    bd
  case s:String =>
    str2num(s)
  case d:Double =>
    Big(d)
  case f:Float =>
    Big(f.toDouble)
  case i:Int =>
    Big(i)
  case l:Long   =>
    Big(l)
  case _ =>
    BadNum
  }

  def numStr(xx:Big,colwidth:Int=9,dec:Int=2,factor:Double=1.0):String = {
    val fmtm1 = "%%%d.%df".format(colwidth-1,dec)
    val fmt   = "%%%d.%df".format(colwidth,  dec)
    val str = if      (xx == BadNum)
      " " * (colwidth-3) + "N/A"
    else if (abbrevNums && xx >= 1e9)     
      fmtm1.format(xx/1e9)+"B"
    else if (abbrevNums && xx >= 1e6)     
      fmtm1.format(xx/1e6)+"M"
    else if (abbrevNums && factor != 1.0) 
      fmtm1.format(xx * factor)+"M"
    else                    
      fmt.format(xx)
    str.trim match {
      case "-0.00" => str.replaceAll("-"," ")
      case _ => str
    }
  }

  /*
  * Integer percent, e.g., 8%
  */
  def v2sp(xx:Big,colwidth:Int=3,dec:Int=0,factor:Double=100.0):String =
    numStrPct(xx,colwidth,dec,factor) // alias

  def v2s(num:Big):String = num2string(num.toDouble)

  def numStrPct(xx:Big,colwidth:Int=9,dec:Int=2,factor:Double=100.0):String = {
 // val fmtm0 = "%%%ds".format(colwidth)
    val fmtm1 = "%%%d.%df".format(colwidth-1,dec)
    val fmtm2 = "%%%d.%df".format(colwidth-2,dec)
    val result = (xx * factor) match {
    case BadNum               => " " * (colwidth-3) + "N/A"
    case num if num >= 1e9    => fmtm2.format(num)+"B%"
    case num if num >= 1e6    => fmtm2.format(num)+"M%"
    case num                  => fmtm1.format(num)+"%"
    }
    result
  }
  def num2string(xx:Big,dec:Int=2,factor:Double=1.0):String = {
    numStr(xx,9,dec,factor)
  }
  def num2string(xx:Int):String = {
    numStr(xx,9)
  }

  def num2str[T:Numeric](n:T,colwidth:Int=9):String = {
    val fmtm1 = "%%%dd".format(colwidth-1)
    val fmt   = "%%%dd".format(colwidth)
    val xx = n match {
    case f:Float => f.toDouble
    case d:Double => d
    }
    if      (xx >= 1e9)   fmtm1.format(xx/1e9)+"B"
    else if (xx >= 1e6)   fmtm1.format(xx/1e6)+"M"
    else                    fmt.format(xx)
  }

  def big2double(xx:Big):Double = {
    xx match {
    case BadNum => Double.NaN
    case _ => xx.toDouble
    }
  }
  def bigDecimal2num(num:Big):String = num2string(num)
  def num2str(num:Big):String = num2string(num)
  def v2sPct(num:Big):String = numStrPct(num)

}
import DataTypes.*

object NumLike {
  import scala.language.implicitConversions

  implicit def num2bigDecimal(num:NumLike):Big = num.xx
  def apply(str:String):NumLike = {
    new NumLike(str,str2num(str))
  }

  // this will have to do until after study of scala Numeric design
  def sum(list:List[NumLike]):Big = {
    var tot = Big(0)
    list.foreach { num => tot += num.xx }
    tot
  }
}

class NumLike(val str:String, val xx:Big,val mc:MathContext) {
  import scala.math.BigDecimal.RoundingMode.*
  
  def toBig:Big = xx
  def this(str:String,xx:Big) = this(str,xx,BigDecimal.defaultMathContext)
  def this(xx:Big,mc:MathContext) = this("",xx,mc)
  def this(xx:Big) = this("",xx,BigDecimal.defaultMathContext)

  override def toString (): String = num2string(xx) // Returns the decimal String representation of this Big.
  // val mc = xx.mc

  // method to prevent BadNum from being treated as a numeric
  private def ck(num:Big, alt:Big):Big = if( num == BadNum ) num else alt

  def % (that: Big): Big = xx % that // Remainder after dividing this by that.
  def * (that: Big): Big = xx * that // Multiplication of Bigs
  def + (that: Big): Big = xx + that // Addition of Bigs
  def - (that: Big): Big = xx - that // Subtraction of Bigs
  def / (that: Big): Big = xx / that // Division of Bigs
  def /% (that: Big): (Big, Big) = xx /% that // Division and Remainder - returns tuple containing the result of divideToIntegralValue and the remainder.

  def < (that: Big): Boolean   = xx < that // Less-than of Bigs
  def < (that: NumLike): Boolean   = xx < that.xx // Less-than of NumLike
  def <= (that: Big): Boolean  = xx <= that // Less-than-or-equals comparison of Bigs
  def <= (that: NumLike): Boolean  = xx <= that.xx // Less-than-or-equals comparison of NumLikes
  def > (that: Big): Boolean   = xx > that // Greater-than comparison of Bigs
  def > (that: NumLike): Boolean   = xx > that.xx // Greater-than comparison of NumLikes
  def >= (that: Big): Boolean  = xx >= that // Greater-than-or-equals comparison of Bigs
  def >= (that: NumLike): Boolean  = xx >= that.xx // Greater-than-or-equals comparison of NumLikes

  def abs : Big                = xx.abs // Returns the absolute xx of this Big
  def apply (mc: MathContext): NumLike = new NumLike("",ck(xx,xx.apply(mc))) // Returns a new NumLike based on the supplied MathContext.
  def byteValue (): Byte = xx.byteValue // Converts this Big to a Byte.
  def charValue : Char  = xx.charValue // Converts this Big to a Char.
  def compare (that: Big): Int = xx compare that // Compares this Big with the specified Big
  def doubleValue (): Double = xx.doubleValue // Converts this Big to a Double.
  def equals (that: Big): Boolean = xx equals that // Compares this Big with the specified Big for equality.

  import scala.language.postfixOps

  def floatValue (): Float = xx floatValue// Converts this Big to a Float.
  override def equals (that: Any): Boolean = xx == that // Compares this Big with the specified xx for equality.
  override def hashCode (): Int = xx.hashCode // Returns the hash code for this Big.
  def intValue (): Int = xx.intValue // Converts this Big to an Int.
  def isValidByte : Boolean = xx.isValidByte
  def isValidChar : Boolean = xx.isValidChar
  def isValidInt : Boolean = xx.isValidInt
  def isValidShort : Boolean = xx.isValidShort
  def longValue (): Long = xx.longValue // Converts this Big to a Long.
  def max (that: Big): Big = xx max that // Returns the maximum of this and that
  def min (that: Big): Big = xx min that // Returns the minimum of this and that
  def pow (n: Int): Big = xx pow(n) // Returns a Big whose xx is this ** n.
  def precision : Int = xx.precision // Returns the precision of this Big.
  def quot (that: Big): Big = xx quot that // Divide to Integer xx.
  def remainder (that: Big): Big = xx remainder that // Remainder after dividing this by that.
  def round (mc: MathContext): Big = xx round(mc) // Returns a Big rounded according to the MathContext settings.

  import scala.language.postfixOps

  def scale : Int = xx scale // Returns the scale of this Big.
  def setScale (scale: Int, mode: RoundingMode): NumLike = new NumLike(ck(xx,xx.setScale(scale,mode)))
  def setScale (scale: Int): NumLike = new NumLike(ck(xx,xx.setScale(scale))) // Returns a Big whose scale is the specified xx, and whose xx is numerically equal to this Big's.
  def shortValue (): Short = xx.shortValue // Converts this Big to a Short.
  def signum : Int = xx.signum // Returns the sign of this Big, i.
  def toBigInt (): BigInt = xx.toBigInt // Converts this Big to a scala.
  def toBigIntExact (): Option[BigInt] = xx.toBigIntExact // Converts this Big to a scala.
  def toByte : Byte = xx.toByte
  def toByteExact : Byte = xx.toByteExact // This Big as an exact xx.
  def toChar : Char = xx.toChar
  def toDouble : Double = xx.toDouble
  def toFloat : Float = xx.toFloat
  def toInt : Int = xx.toInt
  def toIntExact : Int = xx.toIntExact
  def toLong : Long = xx.toLong
  def toLongExact : Long = xx.toLongExact
  def toShort : Short = xx.toShort
  def toShortExact : Short = xx.toShortExact
  def ulp : Big = xx.ulp // Returns the size of an ulp, a unit in the last place, of this Big.
  def unary_- : Big = ck(xx,-xx)// Returns a Big whose xx is the negation of this Big
  def underlying (): Big = ck(xx,Big(xx.underlying))
}
