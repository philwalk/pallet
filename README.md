# pallet


### Library for Cross-Platform Development

![CI](https://github.com/philwalk/pallet/actions/workflows/scala.yml/badge.svg)

<img alt="pallet image" width=240 src="images/wooden-pallet.png">


* Provides expressive idioms for writing portable code.

* Write one version of a script that runs in 90% or more of development environments.

The JVM on most platforms other than `Windows` share similarities, and are unix-like.
You can install unix-like shell environments on Windows, but the JVM doesn't understand them.
This library extends unifile [Unifile](https://github.com/philwalk/unifile) to provide filesystem support.
`unifile` might be a better alternative if you don't need Date & Time functions, `csv` support, or if you
want to avoid 3rd party dependencies.

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

To use this library in `scala-cli` or `scala 3.5+` scripts:
```scala
#!/usr/bin/env -S scala-cli shebang
//> using dep org.vastblue::unifile:0.3.12
//> using dep org.vastblue::pallet:0.10.22
import vastblue.pallet.*
```

For sbt projects:
```sbt
  "org.vastblue" % "pallet"  %% "0.10.22" // scala 3
  "org.vastblue" % "pallet_3" % "0.10.22" // scala 2.13.x
```
## Summary
* Use `scala` instead of `bash` or `python` for portable general purpose scripting
* Publish one script version, rather than multiple OS-customized versions
* Script as though you're running in Linux, even on Windows or Mac.
* standard OS and platform variables, based on `uname` information
* directly read csv file rows from `java.nio.file.Path` objects
* lots of commonly-needed file extensions:
  * `if scriptPath.path.lastModified > "/etc/hosts".path.lastModified then ...`
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
* iterate directory subfiles:
  * files: Iterator[JFile]
  * paths: Iterator[Path]
  * walkTree(file: JFile, depth: Int = 1, maxdepth: Int = -1): Iterable[JFile]

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

### Setup for running the example scripts:
recommended scala-cli `hash-bang` line:
  * `#!/usr/bin/env -S scala-cli shebang`

### Portable Conversion of Path Strings to java.nio.file.Path
A posix shell path such as "/etc/fstab" is not recognized by the Windows jvm
as referring to "C:\msys64\etc\fstab", and attempting to read from it probably
throws `FileNotFoundException`.

The following command lines illustrate the default Windows JVM behavior:
```scala
# prints `true` to the Console for Windows paths:
scala -e 'println(java.nio.file.Paths.get("C:/Windows").toFile.isDirectory)'
```
```scala
# prints `false` to the Console for mounted posix paths:
scala -e 'println(java.nio.file.Paths.get("/etc/fstab").toFile.isFile)'
```

### Example OS portable scripts

#### display the native path and the number of lines in `/etc/fstab`
```scala
#!/usr/bin/env -S scala-cli shebang
//> using dep "org.vastblue::pallet::0.10.22"
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

//> using dep "org.vastblue::pallet::0.10.22"
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
//> using dep "org.vastblue::pallet::0.10.22"
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
Things that maximize the odds of your script running on most systems:
  * use `scala 3`
  * represent paths internally with forward slashes
  * drive letter not needed for paths on the current working drive (often C:)
    * to access disks other than the working drive, mount them via `/etc/fstab`
    * `vastblue.Paths.get()` can parse both `posix` and `Windows` filesystem paths
  * don't rely on `java.File.pathSeparator` for parsing path strings
  * split strings to lines using `"(\r)?\n"` rather than JVM default line ending
    * `split("\n")` can leave carriage-return debris
    * `split(java.io.File.separator) fails or leaves debris if input string came from another OS
  * split PATH-like environment variables using `java.io.File.pathSeparator`
  * create `java.nio.file.Path` objects in either of two ways:
    * `vastblue.file.Paths.get("/etc/fstab")
    * `"/etc/fstab".path       // guaranteed to use `vastblue.file.Paths.get()`
### Examples
Examples below illustrate some of the capabilities.

```scala
#!/usr/bin/env -S scala-cli shebang

//> using dep "org.vastblue::pallet::0.10.22"
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

//> using dep "org.vastblue::pallet::0.10.22"
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

//> using dep "org.vastblue::pallet::0.10.22"
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
