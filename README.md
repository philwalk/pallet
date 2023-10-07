# pallet
Library for Cross-Platform Development

<img alt="pallet image" width=200 src="images/wooden-pallet.png">

Provides support for various expressive idioms typical of scripting languages, with a goal of supporting portable code runnable across platforms with minimal customization.

* No Dependencies

* Tested on Scala Versions
  * `scala 3.3.1`
  * `scala 2.13`

* Tested Target environments
  * `Linux`
  * `Windows`
  * `Cygwin64`
  * `Msys64`
  * `Mingw64`
  * `Git-bash`
  * `WSL Linux`

* Soon-to-be Tested
  * `OSX`

### Usage

Add the following dependency to `build.sbt`
```sbt
  "org.vastblue" % "pallet" % "0.8.12-SNAPSHOT"
```

### Example script:
```scala
#!/usr/bin/env -S scala-cli.bat shebang

//> using scala "2.13.12"
//> using lib "org.vastblue::pallet::0.8.4-SNAPSHOT"

import vastblue.pathextend._

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
main(args)
```

The classpath may also be defined with an `@atFile`

Example `@atFile` 
```
-cp /Users/username/.ivy2/local/org.vastblue/pallet_2.13/0.8.4-SNAPSHOT/jars/pallet_2.13.jar
```

```scala
#!/usr/bin/env -S scala -cp @~/.scala3cp
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

