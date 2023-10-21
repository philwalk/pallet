//#!/usr/bin/env -S scala @${HOME}/.scala3cp -explain

import vastblue.pathextend._
import scala.sys.process._
import vastblue.Platform._
import vastblue.PidCmd._

object BashProcs {
  def main(args: Array[String]): Unit = {
    for (pc <- procCmdlines){
      if (pc.isJava){
        printf("%s\n", pc.args.mkString("|"))
      }
    }
  }
}
