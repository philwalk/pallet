#!/usr/bin/env -S scala
def main(args: Array[String]): Unit = {
  import scala.sys.process._
  val name = if (vastblue.Platform.isWindows){
    "where.exe"
  } else {
    "which"
  }
  val whereExe = Seq(name, name).lazyLines_!.take(1).toList.mkString("").replace('\\', '/')
  printf("path of [%s] is [%s]\n", name, whereExe)
}
