//#!/usr/bin/env -S scala3
package vastblue

import vastblue.pallet._

object Args {
  def main(args: Array[String]): Unit = {
    val argv = MainArgs.prepArgv(args.toIndexedSeq)
    for ((a, i) <- argv.zipWithIndex) {
      printf(s"arg %2d:[%s]\n", i, a)
    }
  }
}
