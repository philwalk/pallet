#!/usr/bin/env -S scala @./atFile

import vastblue.pallet._
import vastblue.file.MountMapper

def main(args: Array[String]): Unit =
  /*
  if (isDarwin || args.contains("-verbose")) {
    MountMap.main(args.filter { _ != "-verbose" })
  }
  */
  if (!isDarwin) {
    val cygdrivePrefix = MountMapper.reverseMountMap.get("cygdrive").getOrElse("not-found")
    printf("cygdrivePrefix: [%s]\n", cygdrivePrefix)
    for ((k, v) <- MountMapper.mountMap) {
      printf("%-22s: %s\n", k, v)
    }
  }
