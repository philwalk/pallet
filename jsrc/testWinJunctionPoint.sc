#!/usr/bin/env -S scala
//package vastblue.examples

import vastblue.pallet.*

object TestWinJunctionPoint {
  def main(args: Array[String]): Unit = {
    val (opts, filenames) = args.partition { (s: String) => s.startsWith("-") }
    val paths = if (filenames.isEmpty) {
      defaultTargets
    } else {
      filenames
    }.map { _.path }.filter { _.exists }

    for (arg <- args){
      val (flag, target) = vastblue.file.Util.isWindowsJunction(arg)
      if (flag) {
        printf("points to [%s]\n", target)
      } else {
        printf("not a junction point\n")
      }
    }
  }
  val defaultTargets = Seq(
    "C:/Documents and Settings",
    "C:/var",
    "C:/binn",
    "C:/home",
    "C:/usr",
    "C:/d",
    "C:/fscan",
    "C:/cscan",
    "C:/Pictures",
    "C:/share",
    "C:/ProgramFilesx86",
  )
}
