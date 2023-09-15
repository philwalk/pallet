package vastblue.file

import java.nio.file.Path

enum Slash(s: String) {
  case Unx extends Slash("/")
  case Win extends Slash("\\")
  override def toString = s
}
object Slash {
  def unx = '/'
  def win = '\\'
}

import Slash.*

trait EzPath(val initstring: String, val sl: Slash) {
  val p: Path = {
    def str: String = if notWindows then initstring.norm else initstring
    Paths.get(str)
  }
  def ab: Path    = p.toAbsolutePath.normalize
  def abs: String = ab.toString.slash(sl)
  def norm: String = {
    if (sl == Win) {
      initstring
    } else {
      initstring.norm
    }
  }
  def slash: String = {
    if (sl == Win) {
      initstring.replace('/', '\\')
    } else {
      initstring.norm
    }
  }
}
object EzPath {
  // val winu = EzPath("c:\\opt", Unx) // valid
  // val winw = EzPath("c:\\opt", Win) // valid
  def apply(p: Path, sl: Slash) = {
    val pstr: String = if notWindows then p.toString.norm else p.toString
    sl match {
      case Unx => new PathUnx(pstr)
      case Win => new PathWin(pstr)
    }
  }
  def apply(s: String, sl: Slash): EzPath = {
    def str: String = if notWindows then s.norm else s
    if (sl == Unx) {
      new PathUnx(str)
    } else {
      new PathWin(str)
    }
  }
  def apply(s: String): EzPath = {
    def str: String = if notWindows then s.norm else s
    if (notWindows) {
      new PathUnx(str)
    } else {
      new PathWin(str)
    }
  }
}

def notWindows   = java.io.File.separatorChar == '/'
def isWindows    = !notWindows
def defaultSlash = if (isWindows) Slash.Win else Slash.Unx

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

object PathUnx{
  def apply(s: String): PathUnx = new PathUnx(s)
}
class PathUnx(s: String) extends EzPath(s, Slash.Unx) {
  override def toString = abs
}

object PathWin{
  def apply(s: String): PathWin = new PathWin(s)
}
class PathWin(s: String) extends EzPath(s, Slash.Win) {
  override def toString = abs
}

extension (p: Path) {
  def slash(sl: Slash): String = {
    if (sl == Win) {
      p.toString.replace('/', '\\')
    } else {
      p.toString.replace('\\', '/')
    }
  }
}
extension (s: String) {
  def norm: String = {
    s.replace('\\', '/')
  }
  def slash(sl: Slash): String = {
    if (sl == Win) {
      s.replace('/', '\\')
    } else {
      s.norm
    }
  }
}
