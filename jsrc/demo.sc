#!/usr/bin/env -S scala -cp target/scala-3.3.0/classes
// the above hashbang line works after successful sbt compile
import vastblue.pathextend._

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
