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

To use `pallet` in an `SBT` project, add this dependency to `build.sbt`
```sbt
  "org.vastblue" % "pallet" % "0.10.0"
```
For `scala` or `scala-cli` scripts, see examples below.

## TL;DR
Simplicity and Universal Portability:
* Use `scala` instead of `bash` or `python` for portable general purpose scripting
* Publish universal scala scripts, rather than multiple OS-customized versions
* Script as though you're running in on Linux environment.
* Convenient runtime branching based on runtime environment:
```scala
#!/usr/bin/env -S scala-cli shebang

//> using lib "org.vastblue::pallet::0.10.0"
import vastblue.pallet._

  printf("uname / osType / osName:\n%s\n", s"platform info: ${unameLong} / ${osType} / ${osName}")
  if (isLinux) {
    // uname is "Linux"
    printf("hello Linux\n")
  } else if (isDarwin) {
    // uname is "Darwin*"
    printf("hello Mac\n")
  } else if (isWinshell) {
    // isWinshell: Boolean = isMsys | isCygwin | isMingw | isGitSdk | isGitbash
    printf("hello %s\n", unameShort)
  } else if (envOrEmpty("MSYSTEM").nonEmpty) {
    printf("hello %s\n", envOrEmpty("MSYSTEM"))
  } else {
    assert(isWindows, s"unknown environment: ${unameLong} / ${osType} / ${osName}")
    printf("hello Windows\n")
  }
```
* extends the range of scala scripting:
Example: read process command lines from `/proc/$PID/cmdline` files
```scala
#!/usr/bin/env -S scala -deprecation -cp target/scala-3.3.1/classes

import vastblue.pallet._
import vastblue.file.ProcfsPaths.cmdlines

var verbose = false
def main(args: Array[String]): Unit = {
  for (arg <- args) {
    arg match {
    case "-v" =>
      verbose = true
    }
  }
  if (isLinux || isWinshell) {
    printf("script name: %s\n\n", scriptName)
    // find /proc/[0-9]+/cmdline files
    for ((procfile, cmdline) <- cmdlines) {
      if (verbose || cmdline.contains(scriptName)) {
        printf("%s\n", procfile)
        printf("%s\n\n", cmdline)
      }
    }
  } else {
    printf("procfs filesystem not supported in os [%s]\n", osType)
  }
}
```
```bash
$ jsrc/procCmdline.sc
```
output when run from a Windows `Msys64` bash session:
```scala
script name: jsrc/procCmdline.sc

/proc/32314/cmdline
'C:\opt\jdk\bin\java.exe' '-Dscala.home=C:/opt/scala' '-classpath' 'C:/opt/scala/lib/scala-library-2.13.10.jar;C:/opt/scala/lib/scala3-library_3-3.3.1.jar;C:/opt/scala/lib/scala-asm-9.5.0-scala-1.jar;C:/opt/scala/lib/compiler-interface-1.3.5.jar;C:/opt/scala/lib/scala3-interfaces-3.3.1.jar;C:/opt/scala/lib/scala3-compiler_3-3.3.1.jar;C:/opt/scala/lib/tasty-core_3-3.3.1.jar;C:/opt/scala/lib/scala3-staging_3-3.3.1.jar;C:/opt/scala/lib/scala3-tasty-inspector_3-3.3.1.jar;C:/opt/scala/lib/jline-reader-3.19.0.jar;C:/opt/scala/lib/jline-terminal-3.19.0.jar;C:/opt/scala/lib/jline-terminal-jna-3.19.0.jar;C:/opt/scala/lib/jna-5.3.1.jar;;' 'dotty.tools.MainGenericRunner' '-classpath' 'C:/opt/scala/lib/scala-library-2.13.10.jar;C:/opt/scala/lib/scala3-library_3-3.3.1.jar;C:/opt/scala/lib/scala-asm-9.5.0-scala-1.jar;C:/opt/scala/lib/compiler-interface-1.3.5.jar;C:/opt/scala/lib/scala3-interfaces-3.3.1.jar;C:/opt/scala/lib/scala3-compiler_3-3.3.1.jar;C:/opt/scala/lib/tasty-core_3-3.3.1.jar;C:/opt/scala/lib/scala3-staging_3-3.3.1.jar;C:/opt/scala/lib/scala3-tasty-inspector_3-3.3.1.jar;C:/opt/scala/lib/jline-reader-3.19.0.jar;C:/opt/scala/lib/jline-terminal-3.19.0.jar;C:/opt/scala/lib/jline-terminal-jna-3.19.0.jar;C:/opt/scala/lib/jna-5.3.1.jar;;' '-deprecation' '-cp' 'target/scala-3.3.1/classes' './procCmdline.sc'

/proc/32274/cmdline
'bash' '/c/opt/scala/bin/scala' '-deprecation' '-cp' 'target/scala-3.3.1/classes' './procCmdline.sc'
```
Example #2: write and read `.csv` files:
```scala
#!/usr/bin/env -S scala -cp target/scala-3.3.1/classes
//package vastblue

import vastblue.pallet.*

object CsvWriteRead {
  def main(args: Array[String]): Unit = {
    val testFiles = Seq("tabTest.csv", "commaTest.csv")
    for (filename <- testFiles){
      val testFile: Path = filename.toPath

      if (!testFile.exists) {
        // create tab-delimited and comma-delimited test files
        val delim: String = if filename.startsWith("tab") then "\t" else ","
        testFile.withWriter() { w =>
          w.printf(s"1st${delim}2nd${delim}3rd\n")
          w.printf(s"A${delim}B${delim}C\n")
        }
      }

      assert(testFile.isFile)
      printf("\n# filename: %s\n", testFile.norm)
      // display file text lines
      for ((line: String, i: Int) <- testFile.lines.zipWithIndex){
        printf("%d: %s\n", i, line)
      }
      // display file csv rows
      for (row: Seq[String] <- testFile.csvRows){
        printf("%s\n", row.mkString("|"))
      }
    }
  }
}
```
```bash
$ time jsrc/csvWriteRead.sc
```
Output:
```bash
# filename: C:/Users/philwalk/workspace/pallet/tabTest.csv
0: 1st  2nd     3rd
1: A    B       C
1st|2nd|3rd
A|B|C

# filename: C:/Users/philwalk/workspace/pallet/commaTest.csv
0: 1st,2nd,3rd
1: A,B,C
1st|2nd|3rd
A|B|C

real    0m4.269s
user    0m0.135s
sys     0m0.411s
```
## Requirements
In Windows, requires a posix shell:
  ([MSYS64](https://msys2.org), [CYGWIN64](https://www.cygwin.com), or `WSL`)

In `Darwin/OSX`, requires `homebrew` or similar.

Best with a recent version of coreutils:
  (e.g., `ubuntu`: 8.32-4.1ubuntu1, `osx`: stable 9.4)

### Concept
* Concise, expressive and readable scripting idioms
* correct portable handling of command line args
* `vastblue.file.Paths` is a `java.nio.file.Paths` drop-in replacement that:
  * correctly handles mounted `posix` paths
  * returns ordinary `java.nio.file.Path` objects

Examples below illustrate some of the capabilities.

### Background
If you work in diverse environments, you generally must customize scripts for each environment:
 * in `Linux`, `Darwin/Osx`, shell or python scripts
 * in `Windows`, batch files, powershell scripts, or other Windows-specific tools

Hard to make scala scripts portable across `Linux`, `Osx`, `Windows`, because
the jvm doesn't support filesystem abstractions of `cygwin64`, `msys64`, etc.

Most platforms other than `Windows` are unix-like, but:
 * differing conventions and incompatibilities:
   * Linux / OSX symantics of `/usr/bin/env`, etc.

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
-classpath /Users/username/.ivy2/local/org.vastblue/pallet_3/0.10.0/jars/pallet_3.jar
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
#!/ usr / bin / env -S scala

import vastblue.pallet._
import vastblue.Platform._

object Fstab {
  def main(args: Array[String]): Unit = {
    // `posixroot` is the native path corresponding to "/"
    // display the native path and lines.size of /etc/fstab
    val p = Paths.get("/etc/fstab")
    printf("env: %-10s| posixroot: %-12s| %-22s| %d lines\n",
      _uname("-o"), posixroot, p.norm, p.lines.size)
  }
}
```
### Equivalent Scala-cli version of the same script:

```scala
#!/ usr / bin / env -S scala -cli shebang

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.10.0"

import vastblue.pallet._
import vastblue.Platform._

object FstabCli {
  def main(args: Array[String]): Unit = {
    // `posixroot` is the native path corresponding to "/"
    // display the native path and lines.size of /etc/fstab
    val p = Paths.get("/etc/fstab")
    printf("env: %-10s| posixroot: %-12s| %-22s| %d lines\n",
      _uname("-o"), posixroot, p.norm, p.lines.size)
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
//> using lib "org.vastblue::pallet::0.10.0"

import vastblue.pallet._

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

import vastblue.pallet.*

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

import vastblue.pallet._

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
  * don't assume path strings use `java.nio.File.separator` or `sys.props("line.separator")`
  * use them to format output, as appropriate, never to parse path strings
  * split strings with `"(\r)?\n"` rather than `line.separator`
    * `split("\n")` can leave carriage-return debris lines ends
  * create `java.nio.file.Path` objects in either of two ways:
    * `vastblue.file.Paths.get("/etc/fstab")
    * `"/etc/fstab".path       // guaranteed to use `vastblue.file.Paths.get()`
  * if client needs glob expression command line arguments, `val argv = prepArgs(args.toSeq)`
    * this avoids exposure to the `Windows` jvm glob expansion bug, and
    * inserts `script` path or `main` method class as `argv(0)` (as in C/C++)
    * argv(0) script name available as input parameter affecting script behaviour

