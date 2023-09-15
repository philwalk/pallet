package vastblue

import vastblue.DriveColon._


object DriveColon {
  type DriveColon = String

  // empty string or uppercase "[A-Z]:"
  def apply(s: String): DriveColon = {
    require(s.length <= 2, s"bad DriveColon String [$s]")
    val str: String = s match {
    case dl if dl.matches("^[a-zA-Z]:") => dl.toUpperCase
    case dl if dl.matches("^[a-zA-Z]") => s"$dl:".toUpperCase
    case _ => ""
    }
    str
  }

  implicit class DriveColonExtend(dl: DriveColon) {
    def letter: String = dl.substring(0, 1).toLowerCase
    def string: String = dl
    def posix: String = s"/$letter"
    def isEmpty: Boolean = dl.string.isEmpty
    def isDrive: Boolean = !dl.isEmpty
  }
}


