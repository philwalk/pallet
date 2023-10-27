#!/usr/bin/env -S scala @./.scala3cp -deprecation
package vastblue

import vastblue.pathextend._
import vastblue.Platform._
import vastblue.MainArgs

object OwnPid {
  def main(args: Array[String]): Unit = {
    try {
      val argv = MainArgs.prepArgs(args.toSeq)
      for ((arg, i) <- argv.zipWithIndex) {
        printf("%2d: [%s]\n", i, arg)
      }

    } catch {
      case e: Exception =>
        printf("%s\n", e.getMessage)
    }
  }
}
