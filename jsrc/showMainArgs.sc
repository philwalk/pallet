#!/usr/bin/env -S scala @./atFile
//package vastblue

import vastblue.pallet._

def main(args: Array[String]): Unit = {
  for (arg <- args) {
    printf("arg [%s]\n", arg)
  }
  val argv = prepArgv(args.toSeq)
  for ((arg, i) <- argv.zipWithIndex) {
    printf(" %2d: [%s]\n", i, arg)
  }
}
