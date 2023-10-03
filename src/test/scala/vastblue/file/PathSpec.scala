package vastblue.file

import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import vastblue.pathextend._
import vastblue.file.Paths.{canExist, normPath}
import vastblue.Platform.{driveRoot, cwd, cygdrive}

class PathSpec extends AnyFunSpec with Matchers with BeforeAndAfter {
  val verbose   = Option(System.getenv("VERBOSE_TESTS")).nonEmpty
  var hook: Int = 0

  val cygroot: String = cygdrive match {
    case str if str.endsWith("/") => str
    case str                      => s"$str/"
  }

  lazy val TMP: String = {
    val driveLetter = "g"
    val driveRoot   = s"${cygroot}${driveLetter}"
    if (canExist(driveRoot.path)) {
      val tmpdir = Paths.get(driveRoot)
      // val str = tmpdir.localpath
      tmpdir.isDirectory && tmpdir.paths.contains("/tmp") match {
        case true =>
          s"${cygroot}g/tmp"
        case false =>
          "/tmp"
      }
    } else {
      "/tmp"
    }
  }

  /** similar to gnu 'touch <filename>' */
  def touch(p: Path): Int = {
    var exitCode = 0
    try {
      p.toFile.createNewFile()
    } catch {
      case _: Exception =>
        exitCode = 17
    }
    exitCode
  }
  def touch(file: String): Int = {
    touch(file.toPath)
  }
  before {
    testFile.withWriter() { (w: PrintWriter) =>
      testFileLines.foreach { line =>
        w.print(line + "\n")
      }
    }
    printf("testFile: %s\n", testFile)
  }

  describe("File") {
    describe("#eachline") {
      // def parseCsvLine(line:String,columnTypes:String,delimiter:String="") = {
      it("should correctly deliver all file lines") {
        // val lines = testFile.lines
        System.out.printf("testFile[%s]\n", testFile)
        for ((line, lnum) <- testFile.lines.toSeq.zipWithIndex) {
          val expected = testFileLines(lnum)
          if (line != expected) {
            System.err.println(s"line ${lnum}:\n  [$line]\n  [$expected]")
          }
        }
        for ((line, lnum) <- testFile.lines.toSeq.zipWithIndex) {
          val expected = testFileLines(lnum)
          if (line != expected) {
            System.err.println(s"failure: line ${lnum}:\n  [$line]\n  [$expected]")
          } else {
            println(s"success: line ${lnum}:\n  [$line]\n  [$expected]")
          }
          assert(line == expected, s"line ${lnum}:\n  [$line]\n  [$expected]")
        }
      }
    }

    describe("#tilde-in-path-test") {
      it("should see file in user home directory if present") {
        val ok = testfileb.exists
        if (ok) println(s"tilde successfully converted to path '$testfileb'")
        assert(ok, s"error: cannot see file '$testfileb'")
      }
      it("should NOT see file in user home directory if NOT present") {
        val test: Boolean = testfileb.delete()
        val ok            = !testfileb.exists || !test
        if (ok)
          println(
            s"delete() successfull, and correctly detected by 'exists' method on path '$testfileb'"
          )
        assert(ok, s"error: can still see file '$testfileb'")
      }
    }
    if (isWindows) {
      printf("gdrive.exists: %s\n", gdrive.exists)
      printf("gdrive.isDirectory: %s\n", gdrive.isDirectory)
      printf("gdrive.isRegularFileg: %s\n", gdrive.isDirectory)
      printf("gdrive.isSymbolicLink: %s\n", gdrive.isSymbolicLink)
      describe("# dospath test") {
        it("should correctly handle cygwin dospath drive designations, when present") {
          var loop = -1
          for ((fname, expected) <- pathDospathPairs) {
            loop += 1
            printf("fname[%s], expected[%s]\n", fname, expected)
            val file = Paths.get(fname)
            printf("%-22s : %s\n", file.stdpath, file.exists)
            val a = expected.toLowerCase
            // val b = file.toString.toLowerCase
            // val c = file.localpath.toLowerCase
            val d        = file.dospath.toLowerCase
            val df       = normPath(d)
            val af       = normPath(a)
            val sameFile = Paths.isSameFile(af, df)
            if (sameFile || a == d) {
              println(s"a [$a] == d [$d]")
              assert(a == d)
            } else {
              System.err.printf("expected[%s]\n", expected.toLowerCase)
              System.err.printf("file.localpath[%s]\n", file.localpath.toLowerCase)
              System.err.printf(
                "error: expected[%s] not equal to dospath [%s]\n",
                expected.toLowerCase,
                file.localpath.toLowerCase
              )
              val x = file.exists
              val y = new JFile(expected).exists
              if (x && y) {
                assert(a == d)
              } else {
                println(s"[$file].exists: [$x]\n[$expected].exists: [$y]")
              }
            }
          }
        }
      }
      describe("# stdpath test") {
        it("should correctly handle toString") {
          val upairs = toStringPairs.toArray.toSeq
          printf("%d pairs\n", upairs.size)
          var loop = -1
          for ((fname, expected) <- upairs) {
            loop += 1
            if (true || verbose) {
              printf("=====================\n")
              printf("fname[%s]\n", fname)
              printf("expec[%s]\n", expected)
            }
            val file: Path = Paths.get(fname).toAbsolutePath.normalize()
            printf("file.norm[%-22s] : %s\n", file.norm, file.exists)
            printf("file.stdpath[%-22s] : %s\n", file.stdpath, file.exists)
            val exp = expected.toLowerCase
            val std = file.stdpath.toLowerCase
            val nrm = file.norm.toLowerCase
            printf("exp[%s] : std[%s] : nrm[%s]\n", exp, std, nrm)
            // val loc = file.localpath.toLowerCase
            // val dos = file.dospath.toLowerCase
            if (!std.endsWith(exp)) {
              hook += 1
            }
            if (!nrm.endsWith(exp)) {
              hook += 1
            }

            // note: in some cases (on Windows, for semi-absolute paths not on the default drive), the posix
            // version of the path must include the posix drive letter.  This test is subtle in order to
            // recognize this case.
            if (nonCanonicalDefaultDrive) {
              printf("hereDrive[%s]\n", hereDrive)
              if (std.endsWith(expected)) {
                println(s"std[$std].endsWith(expected[$expected]) for hereDrive[$hereDrive]");
              }
              // in this case, there should also be a cygroot prefix (e.g., s"${cygroot}c")
              assert(
                std.endsWith(expected)
              )
            } else {
              if (exp == std) {
                println(s"std[$std] == exp[$exp]")
              } else {
                System.err.printf("error: expected[%s] not equal to toString [%s]\n", exp, std)
              }
              assert(exp == std) // || exp.drop(2) == std.drop(2) || std.contains(exp))
            }
          }
        }
      }

      def getVariants(p: Path): Seq[Path] = {
        val pstr = p.toString.toLowerCase
        import vastblue.DriveRoot._
        val stdpathToo = if (nonCanonicalDefaultDrive) Nil else Seq(p.stdpath)

        val variants: Seq[String] = Seq(
          p.norm,
          p.toString,
          p.localpath,
          p.dospath
        ) ++ stdpathToo // stdpath fails round-trip test when default drive != C:

        val vlist = variants.distinct.map { s =>
          val p = Paths.get(s)
          if (p.toString.take(1).toLowerCase != pstr.take(1)) {
            hook += 1
          }
          p
        }
        vlist.distinct
      }

      describe("# Path consistency") {
        it("round trip conversions should be consistent") {
          for (
            fname <-
              (toStringPairs.toMap.keySet ++ pathDospathPairs.toMap.keySet).toList.distinct.sorted
          ) {
            if (fname == s"${cygroot}q/file") {
              hook += 1
            }
            val f1: Path            = Paths.get(fname)
            val variants: Seq[Path] = getVariants(f1)
            for (v <- variants) { // not necessarily 4 variants (duplicates removed before map to Path)
              // val (k1,k2) = (f1.key,v.key)
              val sameFile = Paths.isSameFile(f1, v)
              if (f1 != v || !sameFile) {
                System.err.printf("f1[%s]\nv[%s]\n", f1, v)
              }
              if (f1.equals(v)) {
                println(s"f1[$f1] == v[$v]")
              }
              assert(sameFile, s"not sameFile: f1[$f1] != variant v[$v]")
              assert(f1.equals(v), s"f1[$f1] != variant v[$v]")
            }
          }
        }
      }
    }
  }

  lazy val testFile: Path = {
    val fnamestr = s"${TMP}/youMayDeleteThisDebrisFile.txt"
    Paths.get(fnamestr)
  }

  lazy val maxLines      = 10
  lazy val testFileLines = (0 until maxLines).toList.map { _.toString }

  lazy val homeDirTestFile = "~/shellExecFileTest.out"
  lazy val testfileb: Path = {
    val p = Paths.get(homeDirTestFile)
    touch(p)
    p
  }

  lazy val here  = cwd.normalize.toString.toLowerCase
  lazy val uhere = here.replaceAll("[a-zA-Z]:", "").replace('\\', '/')

  lazy val hereDrive = here.replaceAll(":.*", ":") match {
    case drive if drive >= "a" && drive <= "z" =>
      drive
    case _ => ""
  }

  lazy val expectedHomeDir = sys.props("user.home").replace('/', '\\')

  lazy val gdrive = Paths.get("g:/")

  lazy val gdriveTests = List(
    (s"${cygroot}g", "g:\\"),
    (s"${cygroot}g/", "g:\\")
  )

  lazy val pathDospathPairs = List(
    (".", here),
    (hereDrive, here),         // jvm treats this as cwd, if on c:
    (s"${cygroot}q/", "q:\\"), // assumes /etc/fstab mounts /cygroot to /
    (s"${cygroot}q", "q:\\"),  // assumes /etc/fstab mounts /cygroot to /
    (s"${cygroot}c", "c:\\"),
    (s"${cygroot}c/", "c:\\"),
    ("~", expectedHomeDir),
    ("~/", expectedHomeDir),
    (s"${cygroot}g", "g:\\"),
    (s"${cygroot}g/", "g:\\"),
    (s"${cygroot}c/data/", "c:\\data")
  ) ::: gdriveTests

  lazy val nonCanonicalDefaultDrive = driveRoot.toUpperCase != "C:"

  lazy val username = sys.props("user.name").toLowerCase

  lazy val toStringPairs = List(
    (".", uhere),
    (s"${cygroot}q/", s"${cygroot}q"),
    (s"${cygroot}q/file", s"${cygroot}q/file"), // assumes there is no Q: drive
    (hereDrive, uhere),                         // jvm: bare drive == cwd
    (s"${cygroot}c/", s"${cygroot}c"),
    ("~", s"/users/${username}"),
    ("~/", s"/users/${username}"),
    (s"${cygroot}g", s"${cygroot}g"),
    (s"${cygroot}g/", s"${cygroot}g"),
    (s"${cygroot}c/data/", "/data")
  )
}
