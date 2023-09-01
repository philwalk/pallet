#!/usr/bin/env -S scala -cp target/scala-3.3.0/classes
import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  ".".path.paths.filter { _.isDirectory }.foreach { (p: Path) => printf("%s\n", p.norm) }
}
