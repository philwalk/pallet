# pallet
Library for Cross-Platform Development

<img alt="pallet image" width=200 src="images/wooden-pallet.png">

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
  "org.vastblue" % "pallet" % "0.9.0"
```

## TL;DR
Write scripts in `scala` instead of `bash` or `python` that "do the same thing" on all platforms, including `Windows`.
A complete solution requires a posix shell, but even without it this library provides an expressive scripting solution.

### Concept
* Concise, powerful and readable scripting idioms
* extended filenames via a drop-in replacement for `java.nio.file.Paths` that:
 * returns an ordinary `java.nio.file.Path` object
 * recognizes `posix` paths in `Windows` shell environments

Example below illustrate the capabilities.

### Background
If your work spans multiple environments, you generally must use different tools for each environment:
 * batch files or powershell scripts in `Windows`
 * bash files everywhere else

Most platforms other than `Windows` are unix-like, so the problem is how to support unix-like scripts in `Windows`.
Installing `bash` shell environments (e.g., `git-bash`, `cygwin64`, `msys64`) is a partial solution.
However, the `jvm` does not recognize the filesystem abstractions.  This library provides the missing piece.

Choices to be made when using `scala` as a general purpose scripting language include:
 * how to manage the classpath
 * learning cross-platform coding techniques

### Setup for running the example scripts:
If you have `scala-cli` installed, it's a good option, and some of the example scripts are written for it.

In `scala 3.x`, the classpath may be defined with an `atFile`, a very simple way to control your `classpath`.

Create a file in the current directory named `./scala3cp`, with your classpath, for example:
```
-cp /Users/username/.ivy2/local/org.vastblue/pallet_3/0.9.0/jars/pallet_3.jar
```
With this configuration, your scala 3 `shebang` line will look like this:
```scala
#!/usr/bin/env -S scala @./scala3cp
```
This works as long as you're in the same directory as the `@atFile`.

A more permanent approach uses an absolute path, .e.g., `/home/username/.scala3cp`, although you might need a slightly different `shebang` line on some platforms.


### Example script: display the native path and number of lines in `/etc/fstab`
```scala
#!/usr/bin/env -S scala
package vastblue

import vastblue.pathextend._
import vastblue.Platform._

object Fstab {
  def main(args: Array[String]): Unit = {
    // display the native path corresponding to "/"
    printf("posixroot: %s\n", posixroot)

    // print the native path and the contents of "/etc/fstab", if present
    val p = Paths.get("/etc/fstab")
    printf("/etc/fstab => %s\n", p.norm)
    printf("size: %s lines\n", p.lines.size)
  }
}
```
### Output on various platforms:
```
# WSL Ubuntu | env: GNU/Linux | posixroot: /           | /etc/fstab            | 6 lines
# Linux Mint | env: GNU/Linux | posixroot: /           | /etc/fstab            | 21 lines
# Cygwin64   | env: Cygwin    | posixroot: C:/cygwin64 | C:/cygwin64/etc/fstab | 24 lines
# Msys64     | env: Msys      | posixroot: C:/msys64/  | C:/msys64/etc/fstab   | 22 lines
```


### Example `scala-cli` script:
This script requires `scala-cli` to be in your path.  It is compatible with all supported environments, although in Windows, to read `/proc/meminfo` requires there to be a posix Shell environment, such as `WSL`, `cygwin64`, `msys64`, `git-bash`, etc.

```scala
#!/usr/bin/env -S scala-cli shebang

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.9.0"

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
```scala
#!/usr/bin/env -S scala -cp @./atFile

import vastblue.pathextend.*

def main(args: Array[String]): Unit =
  // display native path of command-line provided filenames
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
