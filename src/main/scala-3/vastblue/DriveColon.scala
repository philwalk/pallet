package vastblue

opaque type DriveColon = String

// DriveColon Strings can only match "" or "[A-Z]:"
object DriveColon {
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

  extension (dl: DriveColon){
    def letter: String = dl.substring(0,1).toLowerCase
    def string: String = dl
    def posix: String = s"/$letter"
    def isEmpty: Boolean = string.isEmpty
    def isDrive: Boolean = !string.isEmpty
  }
}

