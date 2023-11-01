# pallet
Library for Cross-Platform Development

<img alt="pallet image" width=200 src="images/wooden-pallet.png">

Provides support for expressive idioms typical of scripting languages, for writing portable code that runs everywhere.

* Supported Scala Versions
  * `scala 3.x`
  * `scala 2.13`

* Tested Target environments
  * `Linux`
  * `Darwin/OSX`
  * `Windows`
    * `Cygwin64`
    * `Msys64`
    * `Mingw64`
    * `Git-bash`
    * `WSL Linux`

### Usage

Add the following dependency to `build.sbt`
```sbt
  "org.vastblue" % "pallet" % "0.9.0"
```

## TL;DR
Replace `bash` and `python` scripts with `scala`.  
In Windows, some features require installation of a posix shell (such as [MSYS64](https://msys2.org)), and in `Darwin/OSX`, `homebrew`.

### Concept
* Concise, expressive and readable scripting idioms
* provides a `java.nio.file.Paths` drop-in replacement that:
  * correctly handles mounted `posix` paths
  * returns ordinary `java.nio.file.Path` objects

Examples below illustrate some of the capabilities.

### Background
If your work spans multiple environments, you generally must use different tools for each environment:
 * batch files, powershell scripts, or other Windows-specific tools in `Windows`
 * bash files everywhere else

Most platforms other than `Windows` are unix-like, so the problem is how to support unix-like scripts in `Windows`.
Installing `bash` shell environments (e.g., `git-bash`, `cygwin64`, `msys64`) is a partial solution.
However, the `jvm` does not recognize the filesystem abstractions.  This library provides the missing piece.

Choices to be made when using `scala` as a general purpose scripting language include:
 * how to manage the classpath
 * learning cross-platform coding techniques

### Setup for running the example scripts:
A good option for writing scala scripts is `scala-cli`, and some example scripts are written for it.
Each `scala-cli` scripts specifies required dependency internally, and the classpath is managed for you.

Alternatively, in `scala 3.x`, a classpath may be defined with an `atFile`, for both simple and complex `classpath` definitions.

Some differences to be aware of between `scala-cli` scripts and ordinary `scala` scripts:
  * dependencies must be declared in special comment lines at the top of the script
  * the `main(args)` call must be explicitly specified in the script file
  * classpath management tends to require less fuss
  * startup times the two script types differ, even after the initial compile invocation.  On my Windows box:
    * 4 seconds for `scala-cli` before printing `hello world`
    * 2 seconds for scala scripts (`SCALA_CLI=-save @/Users/username/scala3cp`)

### Defining the classpath
For a per-user classpath `atFile`, define your classpath in a file named, e.g., `$HOME/scala3cp`.
To include the `scala3` version of this library, for example, the @file might contain:
```
-cp ${HOME}/.ivy2/local/org.vastblue/pallet_3/0.9.0/jars/pallet_3.jar
```
With this configuration, your scala 3 `shebang` line will look like this:
```scala
#!/usr/bin/env -S scala @${HOME}/scala3cp
```
This uses an absolute path: `${HOME}/scala3cp`, so you might need to define `HOME` on some platforms.
Unfortunately, the `Darwin` version of `/usr/bin/env` doesn't seem to expand the $HOME environment variable in my experiments, so you might want to use an explicit path.  For a fully portable alternative, you can reference your `@atFile` argument by defining environment variable, e.g., `SCALA_OPTS=@/Users/username/scala3cp`, eliminating the need to add it to the `shebang` line.  See the section below.

Examples below assume classpath and other options are defined by the `SCALA_CLI` variable.  Note that when the classpath is also defined in the `shebang` line, it supplements (appends) rather than overrides classpath defined in `SCALA_CLI`.

### Example script: display the native path and the number of lines in `/etc/fstab`
```scala
#!/usr/bin/env -S scala

import vastblue.pathextend._
import vastblue.Platform._

object Fstab {
  def main(args: Array[String]): Unit = {
    // `posixroot` is the native path corresponding to "/"
    // display the native path and lines.size of /etc/fstab
    val p = Paths.get("/etc/fstab")
    printf("env: %-10s| posixroot: %-12s| %-22s| %d lines\n",
      uname("-o"), posixroot, p.norm, p.lines.size)
  }
}
```
### Equivalent Scala-cli version of the same script:
```scala
#!/usr/bin/env -S scala-cli shebang

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.9.0"

import vastblue.pathextend._
import vastblue.Platform._

object FstabCli {
  def main(args: Array[String]): Unit = {
    // `posixroot` is the native path corresponding to "/"
    // display the native path and lines.size of /etc/fstab
    val p = Paths.get("/etc/fstab")
    printf("env: %-10s| posixroot: %-12s| %-22s| %d lines\n",
      uname("-o"), posixroot, p.norm, p.lines.size)
  }
}
FstabCli.main(args)
```
### Output on various platforms:
```
WSL Ubuntu # env: GNU/Linux | posixroot: /           | /etc/fstab            | 6 lines
Linux Mint # env: GNU/Linux | posixroot: /           | /etc/fstab            | 21 lines
Cygwin64   # env: Cygwin    | posixroot: C:/cygwin64 | C:/cygwin64/etc/fstab | 24 lines
Msys64     # env: Msys      | posixroot: C:/msys64/  | C:/msys64/etc/fstab   | 22 lines
Darwin     # env: Darwin    | posixroot: /           | /etc/fstab            | 0 lines
```
Note that on Darwin, there is no `/etc/fstab` file, so the `Path#lines` extension returns `Nil`.

### Example `scala-cli` script:
```scala
#!/usr/bin/env -S scala-cli shebang

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.9.0"

import vastblue.pathextend._

def main(args: Array[String]): Unit = {
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

### How to consistently access comand line arguments
The Windows `jvm` sometimes will expand `glob` arguments, even if double-quoted.
 * https://stackoverflow.com/questions/37037375/trailing-asterisks-on-windows-jvm-command-line-args-are-globbed-in-cygwin-bash-s/37081167#37081167:~:text=This%20problem%20is%20caused,net/browse/JDK%2D8131329
This script demonstrates a consistent, portable way to get command line arguments.
```scala
#!/usr/bin/env -S scala
package vastblue

import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  // display default args
  for (arg <- args) {
    printf("arg [%s]\n", arg)
  }
  // display extended and repaired args
  val argv = prepArgs(args.toSeq)
  for ((arg, i) <- argv.zipWithIndex) {
    printf(" %2d: [%s]\n", i, arg)
  }
}
```
Pass arguments with embedded spaces and glob expressions to see the difference between `args` and `argv`.
Notice that `argv` has added the script path as argv(0), similar to the standard in `C` 

### Using `SCALA_OPTS` environment variable
With `scala 3`, you can specify the `classpath` via an environment variable, permitting the use of a universal `shebang` line (a portability requirement).

* Create a classpath `atFile` named `${HOME}/scala3cp`: 
* define `SCALA_OPTS` (e.g., in ~/.bashrc):
  * `export SCALA_OPTS="@${HOME}/scala3cp"`

If you want to speed up subsequent calls to your scripts (after the initial compile-and-run invocation), you can add the `-save` option to your `SCALA_OPTS` variable:
  * `export SCALA_OPTS="@/${HOME}/scala3cp -save"`

The `-save` option saves the compiled script to a `jar` file in the script parent directory, speeding up subsequent calls, which are equivalent to `java -jar <jarfile>`.  The `jar` is self-contained, as it defines main class, classpath, etc. via the jar `manifest.mf` file.

### How to Write Portable Scala Scripts
Things that maximize the odds of your script running on another system:
  * use `scala 3`
  * always call `val argv = prepArgs(args.toSeq)`
  * use `posix` file paths by default
  * only use `File.separator` for output, never for parsing input
  * avoid depending on `System.lineSeparator` (or property `line.separator`)
  * when parsing input, split line endings with regex `[\r\n]+` rather than `line.separator` property or equivalent.
  * use forward slash, avoid drive letters
    * a drive letter is not needed for paths on the current working drive (often C:)
  * in `Windows`, install a posix shell (such as [MSYS64](https://msys2.org) or [CYGWIN64](https://www.cygwin.com))
  * in `Darwin/OSX`, install `homebrew`
  * to access disks other than the working drive, mount the drive via `/etc/fstab`
  * create `java.nio.file.Path` objects from `Strings` by the `String#path` extension
    * `Path` objects created this way can see mounted posix files, e.g,. `/etc/fstab`.

