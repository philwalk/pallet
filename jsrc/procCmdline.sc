#!/usr/bin/env -S scala @./atFile
package vastblue

import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  try {
    val proc = Paths.get("/proc")
    proc.lines.foreach { println }
  } catch {
    case t: Throwable =>
      showLimitedStack(t)
      sys.exit(1)
  }
}
