#!/usr/bin/env -S scala

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.9.0"

import vastblue.pathextend.*
import vastblue.Platform.*

object BashPath {
  lazy val bashPath = where("bash").path
  def main(args: Array[String]): Unit = {
    printf("userhome: [%s]\n", userhome)
    import scala.sys.process.*
    val whereBash = Seq("where.exe", "bash").lazyLines_!.take(1).mkString
    printf("first bash in path:\n%s\n", whereBash)
    printf("%s\n", bashPath)
    printf("%s\n", bashPath.realpath)
    printf("%s\n", bashPath.toRealPath())
    printf("posixroot: %s\n", posixroot)
    printf("sys root:  %s\n", where("bash").norm.replaceAll("(/usr)?/bin/bash.*", ""))
  }
}
