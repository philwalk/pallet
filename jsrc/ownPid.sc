#!/usr/bin/env -S scala @${HOME}/.scala3cp
package vastblue

import vastblue.pathextend._
import vastblue.Platform._
import vastblue.MainArgs

object OwnPid {
  def main(args: Array[String]): Unit = {
    try {
      val argv = MainArgs.prepArgs(args)
      for ((arg, i) <- argv) {
        printf("%2d: [%s]\n", i, arg)
      }

    } catch {
      case e: Exception =>
        printf("%s\n", e.getMessage)
    }
  }
}
