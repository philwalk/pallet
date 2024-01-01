package vastblue.file

//import vastblue.file.EzPath.notWindows
//import vastblue.pallet.ExtendString

import java.nio.file.Path

sealed trait SlashType
class Slash(s: String)

object Slash {
  def unx = '/'
  def win = '\\'

  object Unx extends Slash(unx.toString) with SlashType
  object Win extends Slash(win.toString) with SlashType
}
import Slash._
import EzPath._

class EzPath(val initstring: String, val sl: Slash) {
  val p: Path = {
    def str: String = if (notWindows) initstring.replace('\\', '/') else initstring
    Paths.get(str)
  }
  def ab: Path    = p.toAbsolutePath.normalize
  def abs: String = ab.slash(sl)
  def posx: String = {
    if (sl == Win) {
      initstring
    } else {
      initstring.posx
    }
  }
  def slash: String = {
    if (sl == Win) {
      initstring.replace('/', '\\')
    } else {
      initstring.replace('\\', '/')
    }
  }
}
object EzPath {
  implicit class StExtend(s: String) {
    def slash: String = s
  }
  // val winu = EzPath("c:\\opt", Unx) // valid
  // val winw = EzPath("c:\\opt", Win) // valid
  def apply(p: Path, sl: Slash) = {
    val pstr: String = if (notWindows) p.toString.replace('\\', '/') else p.toString
    sl match {
    case Unx => new PathUnx(pstr)
    case Win => new PathWin(pstr)
    }
  }

  def apply(s: String, sl: Slash): EzPath = {
    def str: String = if (notWindows) s.replace('\\', '/') else s

    if (sl == Unx) {
      new PathUnx(str)
    } else {
      new PathWin(str)
    }
  }

  def apply(s: String): EzPath = {
    def str: String = if (notWindows) s.replace('\\', '/') else s

    if (notWindows) {
      new PathUnx(str)
    } else {
      new PathWin(str)
    }
  }

  def defaultSlash = if (isWindows) Slash.Win else Slash.Unx

  def notWindows = java.io.File.separatorChar == '/'

  def isWindows = !notWindows

  def platformPrefix: String = Paths.get(".").toAbsolutePath.getRoot.toString match {
  case "/" => ""
  case s   => s.take(2)
  }

  def winlikePathstr(s: String): Boolean = {
    s.contains(':') || s.contains('\\')
  }

  def defaultSlash(s: String): Slash = {
    if (winlikePathstr(s)) Slash.Win else Slash.Unx
  }

  object PathUnx {
    def apply(s: String): PathUnx = new PathUnx(s)
  }
  class PathUnx(s: String) extends EzPath(s, Slash.Unx) {
    override def toString = abs
  }

  object PathWin {
    def apply(s: String): PathWin = new PathWin(s)
  }
  class PathWin(s: String) extends EzPath(s, Slash.Win) {
    override def toString = abs
  }

  implicit class PathExt(p: Path) {
    def slash(sl: Slash): String = {
      if (sl == Win) {
        p.toString.replace('/', '\\')
      } else {
        p.toString.replace('\\', '/')
      }
    }
  }
  implicit class StrExt(s: String) {
    def posx: String = {
      s.replace('\\', '/')
    }

    def slash(sl: Slash): String = {
      if (sl == Win) {
        s.replace('/', '\\')
      } else {
        s.posx
      }
    }
  }
}
