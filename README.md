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
  "org.vastblue" % "pallet" % "0.9.2"
```

## TL;DR
Simplicity and Portability:
* If you would like to use `scala` instead of `bash` or `python` for writing portable scripts
* If you distribute example scala scripts that you want everyone to be successful running

In Windows, some features require a posix shell ([MSYS64](https://msys2.org), [CYGWIN64](https://www.cygwin.com), or `WSL`)
In `Darwin/OSX`, requires `homebrew` or similar.
A recent version of coreutils (e.g., `ubuntu`: 8.32-4.1ubuntu1, `osx`: stable 9.4)

### Concept
* Concise, expressive and readable scripting idioms
* correct portable handling of command line args
* provides a `java.nio.file.Paths` drop-in replacement that:
  * correctly handles mounted `posix` paths
  * returns ordinary `java.nio.file.Path` objects

Examples below illustrate some of the capabilities.

### Background
If you work in diverse environments, you generally must customize scripts for each environment:
 * in `Linux`, shell or python scripts
 * in `Darwin/Osx`, ...
 * in `Windows`, batch files, powershell scripts, or other Windows-specific tools

Although most platforms other than `Windows` are unix-like, the problems are:
 * how to support unix-like scripts in `Windows`
 * how to work around differing conventions and incompatibilities elsewhere:
   * Incompatible symantics of `/usr/bin/env`, etc.
   * Linux `/var/proc` not available in `Darwin/Osx`

In `Windows`, installing a posix shell (e.g., `git-bash`, `cygwin64`, `msys64`) provides some help,
but `jvm` languages don't support the posix filesystem abstractions.
This library provides the missing piece.

Choices to be made when using `scala` as a general purpose scripting language include:
 * how to manage the classpath
 * learning cross-platform coding techniques

### The Classpath
The various approaches to managing classpaths fall into two categories.
In addition to installing scripts, client systems must either:
  * install `scala-cli`
  * install required jars plus an associated `@atFile`

If `scala-cli` is installed, the classpath is fully managed for you.
If required jars plus associated `@atFile` are installed, your scripts must either:
  * reference `@<path-to-atFile>` in the `shebang` line
  * set environment variable `SCALA_OPTS=@<path-to-atFile>`

To support `Darwin/Osx`, an absolute path to an `@atFile` is required in the `shebang` line.
Example portable `shebang` line:
   `#!/usr/bin/env -S scala @/opt/atFiles/.scala3cp`

Alternatively, if `SCALA_OPTS` is defined:
   `#!/usr/bin/env -S scala`

### Setup for running the example scripts:
A good option for writing scala scripts is `scala-cli`, and some example scripts are written for it.
Each `scala-cli` script specifies required dependency internally, and the classpath is managed for you.

A `scala-cli` alternative is to create an `@atFile` containing the `-classpath` definition.

Some differences to be aware of between `scala-cli` scripts and conventional `scala` scripts:
  * a `scala-cli` script declares dependencies within the script via special comments 
  * if `main()` is defined, it must be explicitly called within a `scala-cli` script
  * startup times the two script types differ, even after the initial compile invocation.  On my Windows box:
    * 4 seconds for `scala-cli` before printing `hello world`
    * 2 seconds for scala scripts (`SCALA_CLI=-save @/Users/username/scala3cp`)

### Defining the classpath
For a per-user classpath `atFile`, define your classpath in a file named, e.g., `/Users/username/.scala3cp`.
To include the `scala3` version of this library, for example, the `@file` might contain:
```
-classpath /Users/username/.ivy2/local/org.vastblue/pallet_3/0.9.2/jars/pallet_3.jar
```
With this configuration, your scala 3 `shebang` line will look like this:
```scala
#!/usr/bin/env -S scala @${HOME}/scala3cp
```

In `Darwin/Osx` the `${HOME}` path must be explicit, due to `/usr/bin/env` semantics.
The alternative is to reference the `@atFile` via `SCALA_OPTS=@/Users/username/scala3cp` rather than in the `shebang` line.

Examples below assume classpath and other options are defined by the `SCALA_CLI` variable.

Note that if `classpath` is also defined in the `shebang` line, it will append to the classpath defined in `SCALA_CLI`.

### Example script: display the native path and the number of lines in `/etc/fstab`
This example might surprise developers working in a `Windows` posix shell, since `jvm` languages normally cannot see posix file paths that aren't also legal `Windows` paths.
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
//> using lib "org.vastblue::pallet::0.9.2"

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
### Output of the previous example scripts on various platforms:
```
Linux Mint # env: GNU/Linux | posixroot: /           | /etc/fstab            | 21 lines
Darwin     # env: Darwin    | posixroot: /           | /etc/fstab            | 0 lines
WSL Ubuntu # env: GNU/Linux | posixroot: /           | /etc/fstab            | 6 lines
Cygwin64   # env: Cygwin    | posixroot: C:/cygwin64 | C:/cygwin64/etc/fstab | 24 lines
Msys64     # env: Msys      | posixroot: C:/msys64/  | C:/msys64/etc/fstab   | 22 lines
```
Note that on Darwin, there is no `/etc/fstab` file, so the `Path#lines` extension returns `Nil`.

### Example `scala-cli` script:
```scala
#!/usr/bin/env -S scala-cli shebang

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.9.2"

import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  // list child directories of "."
  val cwd: Path = Paths.get(".")
  for ( p: Path <- cwd.paths.filter { _.isDirectory }) {
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
The Windows `jvm` will sometimes expand `glob` arguments, even if double-quoted.
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
Notice that `argv` has the script path in argv(0), similar to the standard in `C` 

### Using `SCALA_OPTS` environment variable
With `scala 3`, you can specify the `classpath` via an environment variable, permitting the use of a universal `shebang` line (a portability requirement).

* Create a classpath `atFile` named `${HOME}/scala3cp`: 
* define `SCALA_OPTS` (e.g., in ~/.bashrc):
  * `export SCALA_OPTS="@${HOME}/scala3cp"`

If you want to speed up subsequent calls to your scripts (after the initial compile-and-run invocation), you can add the `-save` option to your `SCALA_OPTS` variable:
  * `export SCALA_OPTS="@/${HOME}/scala3cp -save"`

The `-save` option saves the compiled script to a `jar` file in the script parent directory, speeding up subsequent calls, which are equivalent to `java -jar <jarfile>`.  The `jar` is self-contained, as it defines main class, classpath, etc. via the jar `manifest.mf` file.

### Setup
  * `Windows`: install one of the following:
    * [MSYS64](https://msys2.org)
    * [CYGWIN64](https://www.cygwin.com)
    * [WSL](https://learn.microsoft.com/en-us/windows/wsl/install)
  * `Linux`: required packages:
    * `sudo apt install coreutils`
  * `Darwin/OSX`:
    * `brew install coreutils`

### How to Write Portable Scala Scripts
Things that maximize the odds of your script running on another system:
  * use `scala 3`
  * use `posix` file paths by default
  * in `Windows`
    * represent paths internally with forward slashes and avoid drive letters
    * drive letter not needed for paths on the current working drive (often C:)
    * to access disks other than the working drive, mount them via `/etc/fstab`
    * `vastblue.Paths.get()` is can parse both `posix` and `Windows` filesystem paths
  * never use `java.nio.File.separator` for parsing input, only for output, as appropriate
  * never use `sys.props("line.separator")` for parsing input, only for output, as appropriate
  * split input into lines via regex `[\r\n]+` rather than `line.separator`
    * compatible with input files generated anywhere
  * create `java.nio.file.Path` objects in either of two ways:
    * `Paths.get("/etc/fstab")
    * `"/etc/fstab".path
  * if client needs glob expression command line arguments, `val argv = prepArgs(args.toSeq)`
    * this avoids exposure to the `Windows` jvm glob expansion bug, and
    * inserts `script` path or `main` method class as `argv(0)` (as in C/C++)
    * argv(0) script name available as input parameter affecting script behaviour

