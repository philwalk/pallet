//#!/usr/bin/env -S scala
package vastblue

import vastblue.pallet.*

object CmdInfo {
  def main(args: Array[String]): Unit = {
    printf("# [%s]\n", args.toList)
    for ((arg, i) <- args.zipWithIndex) {
      printf("A:  args(%d) == [%s]\n", i, arg)
    }
    val argv = prepArgv(args.toSeq)
    for ((arg, i) <- argv.zipWithIndex) {
      printf("B:  argv(%d) == [%s]\n", i, arg)
    }
  }
}
