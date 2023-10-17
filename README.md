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
With `scala 3`, you can specify the `classpath` via an environment variable, permitting the use of a universal `shebang` line (a portability requirement).

* Create a classpath `atFile` named `/Users/username/.scala3cp`: 
* define `SCALA_OPTS` (e.g., in ~/.bashrc):
  * `export SCALA_OPTS="@/Users/username/.scala3cp"`

If you want to speed up subsequent calls to your scripts (after the initial compile-and-run invocation), you can add the `-save` option to your `SCALA_OPTS` variable:
  * `export SCALA_OPTS="@/Users/username/.scala3cp -save"`

The `-save` option saves the compiled script to a `jar` file in the script parent directory, speeding up subsequent calls, which are equivalent to `java -jar <jarfile>`.  The `jar` is self-contained, as it defines main class, classpath, etc. via the jar `manifest.mf` file.

### Fix for inconsistent treatment of glob arguments by the `jvm`
The Windows `jvm` sometimes will expand `glob` arguments, even if double-quoted.
 * https://bugs.openjdk.org/browse/JDK-8131329
 * https://bugs.openjdk.org/browse/JDK-8131680
 * https://bugs.openjdk.org/browse/JDK-8158359
 * https://stackoverflow.com/questions/37037375/trailing-asterisks-on-windows-jvm-command-line-args-are-globbed-in-cygwin-bash-s/37081167#37081167:~:text=This%20problem%20is%20caused,net/browse/JDK%2D8131329

Actions that only occur in some environments are a problem for portable code.
This partial implementation of `/usr/bin/find` avoids the problem:

```scala
#!/usr/bin/env -S scala @./atFile
package vastblue

import vastblue.pathextend._

object Find {
  def main(args: Array[String]): Unit = {
    try {
      val parms = parseMainArgv(mainArgv)

      for (dir <- parms.paths) {
        for (f <- walkTree(dir.toFile, maxdepth = parms.maxdepth)) {
          val p = f.toPath
          if (parms.matches(p)) {
            printf("%s\n", p.relpath.posixpath)
          }
        }
      }
    } catch {
      case t: Throwable =>
        showLimitedStack(t)
        sys.exit(1)
    }
  }

  def usage(m: String = ""): Nothing = {
    if (m.nonEmpty) {
      printf("%s\n", m)
    }
    printf("usage: %s [options]\n", scriptName)
    def usageText = Seq(
      "<dir1> [<dir2> ...]",
      " [-maxdepth <N>]",
      " -type [fdl]",
      " [-name | -iname] <filename-glob>",
    )
    for (str <- usageText) {
      printf("  %s\n", str)
    }
    sys.exit(1)
  }

  /**
   * Parse vastblue.script.mainArgv, equivalent to C language main arguments vector.
   *
   * jvm main#args and script.mainArgv.tail identical if `glob` args are not passed.
   *
   * mainArgv always delivers unexpanded glob arguments.
   */
  def parseMainArgv(args: Seq[String]): CmdParams = {
    var cmdParms = new CmdParams()
    parse(mainArgv.tail) // args is (usually) identical to mainArgv.tail

    def parse(args: Seq[String]): Unit = {
      if (args.nonEmpty) {
        var tailargs = List.empty[String]
        args match {
        case Nil =>
          usage()
        case "-v" :: tail =>
          tailargs = tail
          cmdParms.verbose = true
        case "-maxdepth" :: dep :: tail =>
          tailargs = tail
          if (dep.matches("[0-9]+")) {
            cmdParms.maxdepth = dep.toInt
          } else {
            usage(s"-maxdepth followed by a non-integer: [$dep]")
          }

        case "-type" :: typ :: tail =>
          tailargs = tail
          typ match {
          case "f" | "d" | "l" =>
            cmdParms.ftype = typ
          case _ =>
            usage(s"-type [$typ] not supported")
          }

        case "-name" :: nam :: tail =>
          tailargs = tail
          if (cmdParms.verbose) printf("nam[%s]\n", nam)
          cmdParms.glob = nam

        case "-iname" :: nam :: tail =>
          tailargs = tail
          if (cmdParms.verbose) printf("nam[%s]\n", nam)
          cmdParms.glob = nam
          cmdParms.nocase = true

        case arg :: _ if arg.startsWith("-") =>
          usage(s"unknown predicate '$arg'")

        case sdir :: tail =>
          tailargs = tail
          if (cmdParms.verbose) printf("sdir[%s]\n", sdir)
          if (!sdir.path.exists) {
            usage(s"not found: $sdir")
          }
          cmdParms.dirs :+= sdir
        }
        if (tailargs.nonEmpty) {
          parse(tailargs)
        }
      }
    }
    cmdParms.validate // might exit with usage message
    cmdParms
  }

  // command line interface parameters
  class CmdParams(
      var dirs: Seq[String] = Nil,
      var glob: String = "",
      var ftype: String = "",
      var maxdepth: Int = -1,
      var nocase: Boolean = false,
      var verbose: Boolean = false,
  ) {
    val validFtypes = Seq("f", "d", "l")
    def validate: Unit = {
      if (dirs.isEmpty) {
        usage("must provide one or more dirs")
      }
      if (ftype.nonEmpty && !validFtypes.contains(ftype)) {
        usage(s"not a valid file type [$ftype]")
      }
      val badpaths = paths.filter { (p: Path) =>
        !p.exists
      }
      if (badpaths.nonEmpty) {
        for (path <- badpaths) {
          printf(s"not found: [${path.norm}]\n")
        }
        usage()
      }
    }
    lazy val paths = dirs.map { Paths.get(_) }

    import java.nio.file.{FileSystems, PathMatcher}
    lazy val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
    def nameMatch(p: Path): Boolean = {
      matcher.matches(p.getFileName)
    }
    def typeMatch(p: Path): Boolean = {
      ftype match {
      case ""  => true
      case "f" => p.isFile
      case "d" => p.isDirectory
      case "l" => p.isSymbolicLink
      case _   => false // should never happen, ftype was validated
      }
    }
    def matches(p: Path): Boolean = {
      val nameflag = glob.isEmpty || nameMatch(p)
      val typeflag = ftype.isEmpty || typeMatch(p)
      nameflag && typeflag
    }
  }
}
```

### How to Write Portable Scala Scripts
(TO BE CONTINUED)
