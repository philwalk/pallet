#!/usr/bin/env -S scala
//package vastblue.examples

import vastblue.pallet.*

object IsWinJunctionPoint {
  def main(args: Array[String]): Unit =
    for (arg <- args){
      val (flag, target) = vastblue.file.Util.isWindowsJunction(arg)
      if (flag) {
        printf("points to [%s]\n", target)
      } else {
        printf("not a junction point\n")
      }
    }
}
