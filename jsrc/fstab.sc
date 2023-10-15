#!/usr/bin/env -S scala @./atFile
//package vastblue

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.9.0"

import vastblue.pathextend._
import vastblue.Platform._

object biz {
  def main(args: Array[String]): Unit = {
    // display native path corresponding to "/", and the native path and lines.size of /etc/fstab
    val p = Paths.get("/etc/fstab")
    printf("env: %-10s| posixroot: %-12s| %-22s| %d lines\n",
      uname("-o"), posixroot, p.norm, p.lines.size)
  }
}
