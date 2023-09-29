#!/usr/bin/env -S scala
def main(args: Array[String]): Unit = {
  import scala.sys.process._
  val whereExe = Seq("where.exe", "where").lazyLines_!.take(1).toList.mkString("").replace('\\', '/')
  printf("whereExe[%s]\n", whereExe)
}
