#!/usr/bin/env -S scala-cli shebang
//package vastblue

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.9.0"

import vastblue.pathextend._
import vastblue.Platform._

object Fstab {
  def main(args: Array[String]): Unit = {
    printf("posixroot: %s\n", posixroot)
    val p = Paths.get("/etc/fstab")
    printf("%s\n", p.norm)
    if (p.isFile){
      p.lines.foreach { println }
    }
  }
}
