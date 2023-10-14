#!/usr/bin/env -S scala @./atFile 
package vastblue

import vastblue.pathextend._
import java.nio.file.{FileSystems, PathMatcher}

object Find {
  var parms = new Parms()
  var verbose = false

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

  def main(ignore: Array[String]): Unit = {
    try {
      // Windows jvm expands args wildcards, so mainArgs is args
      parseArgs(mainArgs)
      parms.validate // calls usage if invalid
      for (dir <- parms.paths){
        hook += 1
        for(f <- walkTree(dir.toFile, maxdepth = parms.maxdepth)) {
          hook += 1
          if (parms.ftype.isEmpty || parms.nameMatch(f.toPath)) {
            printf("%s\n", f.stdpath)
          }
        }
      }
    } catch {
    case t: Throwable =>
      showLimitedStack(t)
      sys.exit(1)
    }
  }

  def parseArgs(args: Seq[String]): Unit = {
    if (args.nonEmpty) {
      var tailargs = List.empty[String]
      args match {
        case Nil =>
          usage()
        case "-v" :: tail =>
          verbose = true
          tailargs = tail
        case "-maxdepth" :: dep :: tail =>
          if (dep.matches("[0-9]+")) {
            parms.maxdepth = dep.toInt
            tailargs = tail
          } else {
            usage(s"-maxdepth followed by a non-integer: [$dep]")
          }

        case "-type" :: typ :: tail =>
          typ match {
          case "f" | "d" | "l" =>
            parms.ftype = typ
            tailargs = tail
          case _ =>
            usage(s"-type [$typ] not supported")
          }

        case "-name" :: nam :: tail =>
          if (verbose) printf("nam[%s]\n", nam)
          parms.glob = nam
          tailargs = tail

        case "-iname" :: nam :: tail =>
          if (verbose) printf("nam[%s]\n", nam)
          parms.glob = nam
          parms.nocase = true
          tailargs = tail

        case sdir :: tail =>
          if (verbose) printf("sdir[%s]\n", sdir)
          parms.dirs :+= sdir
          tailargs = tail
      }
      if (tailargs.nonEmpty) {
        parseArgs(tailargs)
      }
    }
  }

  class Parms(
    var dirs: Seq[String] = Nil,
    var glob: String = "",
    var ftype: String = "",
    var maxdepth: Int = -1,
    var nocase: Boolean = false
  ) {
    val validFtypes = Seq("f", "d", "l")
    def validate: Unit = {
      if (dirs.isEmpty){
        usage("must provide one or more dirs")
      }
      if (glob.isEmpty){
        usage("no regex expression provided")
      }
      if (ftype.nonEmpty && !validFtypes.contains(ftype)){
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
    lazy val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
    def nameMatch(p: Path): Boolean = {
      matcher.matches(p.getFileName)
    }
  }
}
