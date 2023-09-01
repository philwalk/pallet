#!/usr/bin/env -S scala -cp target/scala-3.3.0/classes
// the above hashbang line works after successful sbt compile

import vastblue.Platform

def main(args: Array[String]): Unit =
  Platform.main(args)
  for ((k,v) <- Platform.mountMap){
    printf("%-22s: %s\n", k, v)
  }
