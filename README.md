# pallet
Platform Independent Tooling

<img alt="pallet image" width=200 src="images/wooden-pallet.png">

Provides support for various expressive idioms typical of scripting languages, with a goal of supporting portable code runnable across platforms with minimal or no customization.

* No Dependencies

* Target environments
  * Linux
  * OSX
  * Windows 10+
  * Cygwin64
  * Msys64
  * Mingw64
  * Git-bash
  * WSL Linux

#### Usage

Add the following to `build.sbt`
```sbt
  libraryDependencies += "com.palletops" % "pallet" % "0.8.12"
```

#### Example script:
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

The classpath may be defined with an `@atFile`

Example `@atFile` 
```
-cp /Users/username/.ivy2/local/org.vastblue/pallet_2.13/0.8.4-SNAPSHOT/jars/pallet_2.13.jar
```


