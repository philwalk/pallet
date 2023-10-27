#!/usr/bin/env -S scala @./atFile

import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  try {
    val proc = Paths.get("/proc")
    if (proc.exists){
      proc.paths.foreach { println }
    } else {
      printf("not found: [%s]\n", proc)
    }
  } catch {
    case t: Throwable =>
      showLimitedStack(t)
      sys.exit(1)
  }
}
