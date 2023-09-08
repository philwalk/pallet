#!/usr/bin/env -S scala @classpathAtfileClassesDir

// hashbang line requires an classpath @file, containing:
// -cp target/scala-3.3.0/classes
// (assumes a successful compile)

import vastblue.Platform

def main(args: Array[String]): Unit =
  Platform.main(args)
  for ((k,v) <- Platform.mountMap){
    printf("%-22s: %s\n", k, v)
  }
