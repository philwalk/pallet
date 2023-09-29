#!/usr/bin/env -S scala @classpathAtfile

// hashbang line requires an classpath @file, containing:
// -cp target/scala-3.3.0/classes
// (assumes a successful compile)

import vastblue.Platform

def main(args: Array[String]): Unit =
  if (args.contains("-verbose")){
    Platform.main(args.filter { _ != "-verbose" })
  }
  val cygdrivePrefix = Platform.reverseMountMap.get("cygdrive").getOrElse("not-found")
  printf("cygdrivePrefix: [%s]\n", cygdrivePrefix)
  for ((k,v) <- Platform.mountMap){
    printf("%-22s: %s\n", k, v)
  }
