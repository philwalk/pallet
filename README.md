# pallet
Platform Independent Tooling

Provides support for various expressive idioms typical of scripting languages,
with a goal of supporting portable code runnable in many environments with little or no customization.

Target environments include Linux, OSX, Cygwin, Msys2, Mingw, WSL, Windows.

Example script:
```scala
#!/usr/bin/env -S scala -cp target/scala-3.3.0/classes
// hashbang line above is sufficient after 'sbt compile'
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
```
