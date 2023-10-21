#!/usr/bin/env -S scala @${HOME}/.scala3cp -explain

import vastblue.pathextend._
import vastblue.Platform._
import scala.sys.process._
import vastblue.PidCmd

object PidCommands {
  def main(args: Array[String]): Unit = {
    try {
      val verbose = args.contains("-v")
      for (pc <- PidCmd.procCmdlines){
        printf("%s\n", pc)
        for (arg <- pc.pidArgv){
          printf(" pidArg[%s]\n", arg)
        }
      }
      for (arg <- args){
        printf("args: [%s]\n", arg)
      }
      for (arg <- scriptArgv){
        printf("main: [%s]\n", arg)
      }
    } catch {
    case t: Throwable =>
      showLimitedStack(t)
      sys.exit(1)
    }
  }
}
