//#!/usr/bin/env -S scala @./atFile
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
