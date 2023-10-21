#!/usr/bin/env -S scala @${HOME}/.scala3cp
package vastblue

import vastblue.pathextend._
import vastblue.Platform._
import vastblue.ProcInfo

object OwnPid {
  def main(args: Array[String]): Unit = {
    try {
      val procs = ProcInfo.pidCommandlines()
      for (proc <- procs){
        printf("[%s]\n", proc)
      }

    } catch {
      case ex: Exception =>
        printf("%s\n", ex.getMessage)
    }
  }
}
