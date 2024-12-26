//#!/usr/bin/env -S scala -cp target/scala-3.4.3/classes
package vastblue.examples

import vastblue.pallet.*
import vastblue.file.ProcfsPaths.*

object ProcCmdline {
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
}
