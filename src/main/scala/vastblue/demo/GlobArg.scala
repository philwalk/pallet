//#!/usr/bin/env -S scala @./atFile -deprecation
package vastblue.demo

import vastblue.pallet.*

object GlobArg {
  // if glob args are passed, they should be preserved in argv
  def main(args: Array[String]): Unit = {
    for ((a, i) <- args.zipWithIndex) {
      printf(" %2d: [%s]\n", i, a)
    }
    val argv = prepArgs(args.toSeq)
    for ((a, i) <- argv.zipWithIndex) {
      printf(" %2d: [%s]\n", i, a)
    }
  }
}
