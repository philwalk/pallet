# pallet
Platform Independent Tooling

<img alt="pallet image" width=200 src="images/wooden-pallet.png">

Provides support for various expressive idioms typical of scripting languages,
with a goal of supporting portable code runnable in many environments with little or no customization.

Target environments include Linux, OSX, Cygwin, Msys2, Mingw, WSL, Windows.

Example script:
```scala
#!/usr/bin/env -S scala -cp target/scala-3.3.0/classes
// hashbang above is sufficient after 'sbt compile'
import vastblue.pathextend.*

def main(args: Array[String]): Unit = {
  // show system memory info
  for (line <- "/proc/meminfo".path.lines) {
    printf("%s\n", line)
  }
  // list child directories of "."
  val cwd: Path = ".".path
  for ( (p: Path) <- cwd.paths.filter { _.isDirectory }){
    printf("%s\n", p.norm)
  }
}
```

