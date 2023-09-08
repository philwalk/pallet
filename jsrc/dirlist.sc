#!/usr/bin/env -S scala @classpathAtfile
// hashbang line requires successful sbt compile
import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  ".".path.paths.filter { _.isDirectory }.foreach { (p: Path) => printf("%s\n", p.norm) }
}
