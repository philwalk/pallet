#!/usr/bin/env -S scala @${HOME}/.scala3cp
package vastblue

import vastblue.pathextend._
import vastblue.Platform._
//import vastblue.ProcInfo._
import scala.jdk.CollectionConverters._

object MainName {
  def main(args: Array[String]): Unit = {
    val mainName = Thread.currentThread().getStackTrace().last.getClassName()
    printf("#1: %s\n", mainName)
    printf("scriptName: [%s]\n", scriptName)
    printf("scriptArgs: [%s]\n", scriptArgs.mkString("|"))
    printf("%s\n", thisProc)
  }
}
