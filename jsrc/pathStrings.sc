#!/usr/bin/env -S scala

import vastblue.pathextend.*

object PathStrings {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      printf("usage: %s <filepath-1> [<filepath2> ...]\n", scriptPath.path.name)
    } else {
      val argv = prepArgs(args)
      for (a <- argv) {
        printf("========== arg[%s]\n", a)
        printf("stdpath   [%s]\n", Paths.get(a).stdpath)
        printf("normpath  [%s]\n", Paths.get(a).norm)
        printf("dospath   [%s]\n", Paths.get(a).dospath)
      }
    }
  }
}
