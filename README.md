# pallet


### Library for Cross-Platform Development

![CI](https://github.com/philwalk/pallet/actions/workflows/scala.yml/badge.svg)

<img alt="pallet image" width=240 src="images/wooden-pallet.png">


Provides support for expressive idioms typical of scripting languages, for writing portable code that runs everywhere.
Leverages `vastblue.unifile.Paths.get()` to support both `posix` and `Windows` filesystem paths.

* Supported Scala Versions
  * `scala 3.x`

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
  "org.vastblue" % "pallet"  %% "0.10.19" // scala 3
  "org.vastblue" % "pallet_3" % "0.10.19" // scala 2.13.x
```

## Summary
Simplicity and Universal Portability:
* Use `scala` instead of `bash` or `python` for portable general purpose scripting
* Publish universal scala scripts, rather than multiple OS-customized versions
* Script as though you're running in Linux, even on Windows or Mac.
* standard OS and platform variables, based on `uname` information
* directly read csv file rows from `java.nio.file.Path` objects
Extends the range of scala scripting:
* reference Windows filesystem paths via posix abstractions
* predefined environment information:
  * osType: String     
  * osName: String     
  * scalaHome: String
  * javaHome: String 
  * userhome: String 
  * username: String 
  * hostname: String 
  * uname: String
  * shellRoot: String 
  * isLinux: Boolean   
  * isWinshell: Boolean
  * isDarwin: Boolean  
  * isWsl: Boolean     
  * isCygwin: Boolean 
  * isMsys: Boolean   
  * isMingw: Boolean  
  * isGitSdk: Boolean 
  * isGitbash: Boolean
  * isWindows: Boolean
  * verbose: Boolean 

* extension methods on `java.nio.file.Path` and `java.io.File`
  * name: String
  * basename: String // drop extension
  * parent: Path
  * lines: Iterator[String]
  * md5: String
  * sha256: String
  * cksum: Long
  * lastModified: Long
  * newerThan(other: Path): Boolean
  * olderThan(other: Path): Boolean
  * lastModifiedMillisAgo: Long
  * lastModSecondsAgo: Double
  * lastModMinutesAgo: Double
  * lastModHoursAgo: Double
  * lastModDaysAgo: Double
  * withFileWriter(p: Path)(func: PrintWriter => Any)
  * append DATA to script file
  * many others
* iterate directory subfiles:
  * files: Iterator[JFile]
  * paths: Iterator[Path]
  * walkTree(file: JFile, depth: Int = 1, maxdepth: Int = -1): Iterable[JFile]
  * walkTreeFiltered(file: JFile, depth: Int = 1, maxdepth: Int = -1)(filt: JFile => Boolean): Iterable[JFile]
  * walkTreeFast(p: Path, tossDirs: Set[String], maxdepth: Int = -1)(filt: Path => Boolean)

* read files in the `/proc` tree in Windows, e.g.:
  * `/proc/meminfo`
  * `/proc/$PID/cmdline`

## Requirements
In Windows, requires installing a posix shell:
  * [MSYS64](https://msys2.org)
  * [CYGWIN64](https://www.cygwin.com)
  * [WSL](https://learn.microsoft.com/en-us/windows/wsl/install)
  * [Git Bash](https://gitforwindows.org/)

In `Darwin/OSX`, requires `homebrew` or similar.

Best with a recent version of coreutils:
  (e.g., `ubuntu`: 8.32-4.1ubuntu1, `osx`: stable 9.4)

### Concept
* Concise, expressive and readable scripting idioms
* correct portable handling of command line args
* `vastblue.file.Paths` is a `java.nio.file.Paths` drop-in replacement that:
  * correctly handles mounted `posix` paths
  * returns ordinary `java.nio.file.Path` objects

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

### The Classpath
When using `scala-cli`, the classpath is fully managed for you.
For java version 9 or above, you can also define the classpath in a java options file.
When specifying an options file for `Darwin/Osx`, the `@atFile` path must be absolute.

### Setup for running the example scripts:
There are various ways to write scala3 script `hash-bang` lines:

  * `#!/usr/bin/env -S scala-cli shebang`
  * `#!/usr/bin/env -S scala`
  * `#!/usr/bin/env -S scala @/opt/scalaAtfile`

For scala versions 3.5+, the first two variations are roughly (exactly?) equivalent.
For versions 3.4.3 and earlier, the 3rd form defines the `classpath` in an options file.

Each `scala-cli` script specifies required dependencies internally, and the classpath is managed for you.

Some differences to be aware of between `scala-cli` scripts and legacy `scala` scripts:
  * a `scala-cli` script declares dependencies within the script via special comments
  * if a `main()` method is defined, `scala-cli` requires it to be explicitly called at package level.

### Examples
If you work in a `Windows` posix shell, you are aware that `java.nio.file.Paths.get()` expects file path String to be legal `Windows` paths.

The following command line should print `true` to the Console:
```scala
scala -e 'println(java.nio.file.Paths.get("C:/Windows").toFile.isDirectory)'
```
and the following command line will print `false`:
```scala
scala -e 'println(java.nio.file.Paths.get("/etc/fstab").toFile.isFile)'
```

#### script: display the native path and the number of lines in `/etc/fstab`
```scala
#!/usr/bin/env -S scala-cli shebang

//> using dep "org.vastblue::pallet::0.10.19"
import vastblue.pallet.*
val p = Paths.get("/etc/fstab")
printf("env: %-10s| posixroot: %-12s| %-22s| %d lines\n",
  uname("-o"), posixroot, p.posx, p.lines.size)
```
#### Output of the previous example scripts on various platforms:
```
Linux Mint # env: GNU/Linux | posixroot: /           | /etc/fstab            | 21 lines
Darwin     # env: Darwin    | posixroot: /           | /etc/fstab            | 0 lines
WSL Ubuntu # env: GNU/Linux | posixroot: /           | /etc/fstab            | 6 lines
Cygwin64   # env: Cygwin    | posixroot: C:/cygwin64 | C:/cygwin64/etc/fstab | 24 lines
Msys64     # env: Msys      | posixroot: C:/msys64/  | C:/msys64/etc/fstab   | 22 lines
```
Note that on Darwin, there is no `/etc/fstab` file, so the `Path#lines` extension returns `Nil`.

#### Example: list child directories of "."
```scala
#!/usr/bin/env -S scala-cli shebang

//> using dep "org.vastblue::pallet::0.10.19"
import vastblue.pallet.*

// list child directories of "."
val cwd: Path = Paths.get(".")
for ( p: Path <- cwd.paths.filter { _.isDirectory }) {
  printf("%s\n", p.posx)
}
```
#### Example: print the native paths of command line arguments
```scala
#!/usr/bin/env -S scala-cli shebang
//> using dep "org.vastblue::pallet::0.10.19"
import vastblue.pallet.*

// display native path of command-line provided filenames
if args.isEmpty then
  printf("usage: %s <path1> [<path2> ...]\n", scriptPath)
else
  val dirs = for
    fname <- args
    p = Paths.get(fname)
    if p.isFile
  yield p.posx

  printf("%s\n", dirs.toList.mkString("\n"))
```

### Setup
  * `Windows`: install one of the following:
    * [MSYS64](https://msys2.org)
    * [CYGWIN64](https://www.cygwin.com)
    * [WSL](https://learn.microsoft.com/en-us/windows/wsl/install)
    * [Git Bash](https://gitforwindows.org/)
  * `Linux`: required packages:
    * `sudo apt install coreutils`
  * `Darwin/OSX`:
    * `brew install coreutils`

### How to Write Portable Scala Scripts
Things that maximize the odds of your script running on another system:
  * use `scala 3`
  * use `posix` file paths by default
  * in `Windows`
    * represent paths internally with forward slashes
    * drive letter not needed for paths on the current working drive (often C:)
    * to access disks other than the working drive, mount them via `/etc/fstab`
    * `vastblue.Paths.get()` can parse both `posix` and `Windows` filesystem paths
  * don't assume path strings use `java.nio.File.separator` or `sys.props("line.separator")`
  * use them to format output, as appropriate, never to parse path strings
  * split strings with `"(\r)?\n"` rather than `line.separator`
    * `split("\n")` can leave carriage-return debris
    * `split(File.separator) fails or leaves debris if input string came from another OS
  * create `java.nio.file.Path` objects in either of two ways:
    * `vastblue.file.Paths.get("/etc/fstab")
    * `"/etc/fstab".path       // guaranteed to use `vastblue.file.Paths.get()`
### Examples
Examples below illustrate some of the capabilities.

```scala
#!/usr/bin/env -S scala-cli shebang

//> using dep "org.vastblue::pallet::0.10.19"
import vastblue.pallet.*

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
```scala
#!/usr/bin/env -S scala-cli shebang

//> using dep "org.vastblue::pallet::0.10.19"
import vastblue.pallet.*

import vastblue.pallet.*
import vastblue.file.ProcfsPaths.cmdlines

var verbose = false
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
```
```bash
$ jsrc/procCmdline.sc
```
output when run from a Windows `Msys64` bash session:
```scala
script name: jsrc/procCmdline.sc

/proc/32314/cmdline
'C:\opt\jdk\bin\java.exe' '-Dscala.home=C:/opt/scala' '-classpath' 'C:/opt/scala/lib/scala-library-2.13.10.jar;C:/opt/scala/lib/scala3-library_3-3.4.3.jar;C:/opt/scala/lib/scala-asm-9.5.0-scala-1.jar;C:/opt/scala/lib/compiler-interface-1.3.5.jar;C:/opt/scala/lib/scala3-interfaces-3.4.3.jar;C:/opt/scala/lib/scala3-compiler_3-3.4.3.jar;C:/opt/scala/lib/tasty-core_3-3.4.3.jar;C:/opt/scala/lib/scala3-staging_3-3.4.3.jar;C:/opt/scala/lib/scala3-tasty-inspector_3-3.4.3.jar;C:/opt/scala/lib/jline-reader-3.19.0.jar;C:/opt/scala/lib/jline-terminal-3.19.0.jar;C:/opt/scala/lib/jline-terminal-jna-3.19.0.jar;C:/opt/scala/lib/jna-5.3.1.jar;;' 'dotty.tools.MainGenericRunner' '-classpath' 'C:/opt/scala/lib/scala-library-2.13.10.jar;C:/opt/scala/lib/scala3-library_3-3.4.3.jar;C:/opt/scala/lib/scala-asm-9.5.0-scala-1.jar;C:/opt/scala/lib/compiler-interface-1.3.5.jar;C:/opt/scala/lib/scala3-interfaces-3.4.3.jar;C:/opt/scala/lib/scala3-compiler_3-3.4.3.jar;C:/opt/scala/lib/tasty-core_3-3.4.3.jar;C:/opt/scala/lib/scala3-staging_3-3.4.3.jar;C:/opt/scala/lib/scala3-tasty-inspector_3-3.4.3.jar;C:/opt/scala/lib/jline-reader-3.19.0.jar;C:/opt/scala/lib/jline-terminal-3.19.0.jar;C:/opt/scala/lib/jline-terminal-jna-3.19.0.jar;C:/opt/scala/lib/jna-5.3.1.jar;;' '-deprecation' '-cp' 'target/scala-3.4.3/classes' './procCmdline.sc'

/proc/32274/cmdline
'bash' '/c/opt/scala/bin/scala' '-deprecation' '-cp' 'target/scala-3.4.3/classes' './procCmdline.sc'
```
Example #2: write and read `.csv` files:
```scala
#!/usr/bin/env -S scala-cli shebang

//> using dep "org.vastblue::pallet::0.10.19"
import vastblue.pallet.*

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
  printf("\n# filename: %s\n", testFile.posx)
  // display file text lines
  for ((line: String, i: Int) <- testFile.lines.zipWithIndex){
    printf("%d: %s\n", i, line)
  }
  // display file csv rows
  for (row: Seq[String] <- testFile.csvRows){
    printf("%s\n", row.mkString("|"))
  }
}
```
```bash
$ time jsrc/csvWriteRead.sc
```
Output:
```bash
# filename: C:/Users/username/workspace/pallet/tabTest.csv
0: 1st  2nd     3rd
1: A    B       C
1st|2nd|3rd
A|B|C

# filename: C:/Users/username/workspace/pallet/commaTest.csv
0: 1st,2nd,3rd
1: A,B,C
1st|2nd|3rd
A|B|C

real    0m4.269s
user    0m0.135s
sys     0m0.411s
```
