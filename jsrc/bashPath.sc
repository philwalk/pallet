#!/usr/bin/env -S scala
//package vastblue.demo

//> using scala "3.4.3"
//> using dep "org.vastblue::pallet::0.11.0"

import vastblue.pallet.*

object BashPath {
  val bashPath = where("bash").path

  def main(args: Array[String]): Unit = {
    printf("userhome: [%s]\n", userhome)
    import scala.sys.process.*
    val progname = if (isWindows) {
      "where.exe"
    } else {
      "which"
    }
    val whereBash = Seq(progname, "bash").lazyLines_!.take(1).mkString
    printf("first bash in path:\n%s\n", whereBash)
    printf("%s\n", bashPath)
    printf("%s\n", bashPath.realpath)
    printf("%s\n", bashPath.toRealPath())
    printf("shellRoot: %s\n", shellRoot)
    printf("sys root:  %s\n", where("bash").posx.replaceAll("(/usr)?/bin/bash.*", ""))
  }
}
