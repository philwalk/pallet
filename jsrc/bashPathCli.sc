#!/usr/bin/env -S scala-cli shebang

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.8.7-SNAPSHOT"

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
    printf("realroot: %s\n", realroot)
    printf("sys root: %s\n", where("bash").norm.replaceAll("(/usr)?/bin/bash.*", ""))
  }
}
main(args)
