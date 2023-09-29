#!/usr/bin/env -S scala @classpathAtfile

import vastblue.pathextend.*
import vastblue.Platform.{getStdout, envPath, isWindows}
import scala.util.control.Breaks._
import java.nio.file.{Files => JFiles, Paths => JPaths}

def main(args: Array[String]): Unit =
  for (arg <- args){
    val list = findAllInPath(arg)
    printf("found %d [%s] in PATH:\n", list.size, arg)
    for (path <- list) {
      printf(" [%s] found at [%s]\n", arg, path.norm)
      printf("--version: [%s]\n", getStdout(path.norm, "--version"))
    }
  }

def fsep = java.io.File.separator
def exeSuffix: String = if (isWindows) ".exe" else ""

def findAllInPath(prog: String): Seq[Path] = {
  val progname = prog.replace('\\', '/').split("/").last // remove path, if present
  var found    = List.empty[Path]
  for (dir <- envPath) {
    // sort .exe suffix ahead of no .exe suffix
    for (name <- Seq(s"$dir$fsep$progname$exeSuffix", s"$dir$fsep$progname").distinct) {
      val p = JPaths.get(name)
      if (p.toFile.isFile) {
        found ::= p.normalize
      }
    }
  }
  found.reverse
}
