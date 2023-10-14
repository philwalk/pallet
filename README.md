# pallet
Library for Cross-Platform Development

<img alt="pallet image" width=200 src="images/wooden-pallet.jpg">

Provides support for various expressive idioms typical of scripting languages, with support across most platforms.

* No Dependencies

* Supported Scala Versions
  * `scala 3.x`
  * `scala 2.13`

* Tested Target environments
  * `Linux`
  * `Windows`
  * `Cygwin64`
  * `Msys64`
  * `Mingw64`
  * `Git-bash`
  * `WSL Linux`

* Occasionally Verified
  * `OSX`

### Usage

Add the following dependency to `build.sbt`
```sbt
  "org.vastblue" % "pallet" % "0.8.6"
```

### Example `scala-cli` script:
```scala
#!/usr/bin/env -S scala-cli shebang

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.8.6"

import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  // show system memory info
  for (line <- "/proc/meminfo".path.lines) {
    printf("%s\n", line)
  }
  // list child directories of "."
  val cwd: Path = Paths.get(".")
  for ( p: Path <- cwd.paths.filter { _.isDirectory }){
    printf("%s\n", p.norm)
  }
}
main(args)
```

### Example `scala3` script
In `scala 3.x`, the classpath may be defined with an `@atFile`.

Create an `@atFile` named `./atFile`, with these contents:
```
-cp /Users/username/.ivy2/local/org.vastblue/pallet_2.13/0.8.6/jars/pallet_3.jar
```

```scala
#!/usr/bin/env -S scala -cp @./atFile

import vastblue.pathextend.*

def main(args: Array[String]): Unit =
  // show system memory info
  val dirs = for
    fname <- args
    p = Paths.get(fname)
    if p.isFile
  yield p.norm

  printf("%s\n", dirs.toList.mkString("\n"))
```

### Using `SCALA_OPTS` environment variable

* Create a classpath `atFile` named `/Users/username/.scala3cp`: 
* define `SCALA_OPTS`
  * `SCALA_OPTS="@/Users/username/.scala3cp -save"`

(TO BE CONTINUED)
