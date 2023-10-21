#!/usr/bin/env -S scala
package vastblue

import vastblue.pathextend._
import vastblue.Platform._

object CmdInfo {
  def main(args: Array[String]): Unit = {
    val mainArgv = vastblue.script.scriptArgv
    printf("mainArgv           [%s]\n", mainArgv.take(1).mkString)

    printf("scriptPathProperty [%s]\n", scriptPathProperty)
    printf("scriptPath         [%s]\n", scriptPath.norm)
    printf("scriptPath.name    [%s]\n", scriptPath.name)
    printf("scriptName         [%s]\n", scriptName)
    printf("progName           [%s]\n", progName)
    for ((arg, i) <- mainArgv.zipWithIndex) {
      printf("  args(%d) == [%s]\n", i, arg)
    }
  }
}
