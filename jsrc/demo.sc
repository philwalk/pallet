#!/usr/bin/env -S scala @classpathAtfile
// hashbang line requires successful sbt compile
import vastblue.pathextend.*

def main(args: Array[String]): Unit = {
  // show system memory info
  for (line <- "/proc/meminfo".path.lines) {
    printf("%s\n", line)
  }
  // list child directories of the current working directory
  val cwd: Path = ".".path
  for ( (p: Path) <- cwd.paths.filter { _.isDirectory }){
    printf("%s\n", p.norm)
  }
}
