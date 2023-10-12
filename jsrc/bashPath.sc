#!/usr/bin/env -S scala-cli shebang
//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.8.6"

//import vastblue.extensions.*
import vastblue.pathextend.*
import vastblue.Platform.*

lazy val bashPath = where("bash").path
def main(args: Array[String]): Unit = {
  printf("%s\n", bashPath)
  printf("%s\n", bashPath.realpath)
  printf("%s\n", bashPath.toRealPath())
  printf("%s\n", bashPath.realpathLs)
  printf("realroot: %s\n", realroot)
  printf("sys root: %s\n", where("bash").norm.replaceAll("(/usr)?/bin/bash.*", ""))
}
main(args)
