#!/usr/bin/env -S scala -cp target/scala-3.3.0/classes
import vastblue.pathextend._

object PathsDemo {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      printf("usage: %s <filepath-1> [<filepath2> ...]\n", scriptPath.path.name)
    } else {
      for (a <- args) {
        printf("========== arg[%s]\n", a)
        printf("stdpath   [%s]\n", Paths.get(a).stdpath)
        printf("normpath  [%s]\n", Paths.get(a).norm)
        printf("dospath   [%s]\n", Paths.get(a).dospath)
      }
    }
  }
}
