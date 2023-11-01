#!/usr/bin/env -S scala @./atFile

import vastblue.Platform
import vastblue.Platform._

def main(args: Array[String]): Unit =
  if (isDarwin || args.contains("-verbose")) {
    Platform.main(args.filter { _ != "-verbose" })
  }
  if (!isDarwin) {
    val cygdrivePrefix = Platform.reverseMountMap.get("cygdrive").getOrElse("not-found")
    printf("cygdrivePrefix: [%s]\n", cygdrivePrefix)
    for ((k, v) <- Platform.mountMap) {
      printf("%-22s: %s\n", k, v)
    }
  }
