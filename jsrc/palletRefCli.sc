#!/usr/bin/env -S scala-cli shebang

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.9.0"

import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  // show system memory info
  if (vastblue.Platform.osType != "darwin"){
    // osx doesn't have /proc/meminfo
    for (line <- "/proc/meminfo".path.lines) {
      printf("%s\n", line)
    }
  }
  // list child directories of "."
  val cwd: Path = ".".path
  for ((p: Path) <- cwd.paths.filter { _.isDirectory }) {
    printf("%s\n", p.norm)
  }
}
main(args)