package vastblue.file

import vastblue.pathextend.*
import org.scalatest.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import vastblue.Platform.{workingDrive, driveRoot, cwd}

class FileSpec extends AnyFunSpec with Matchers with BeforeAndAfter {
  var verbose = false // manual control
  lazy val TMP = {
    val gdir = Paths.get("/g")
    // val str = gdir.localpath
    gdir.isDirectory && gdir.paths.nonEmpty match {
      case true =>
        "/g/tmp"
      case false =>
        "/tmp"
    }
  }

  /** similar to gnu 'touch <filename>'.
    */
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
  lazy val testFile: Path = {
    val fnamestr = s"${TMP}/youMayDeleteThisDebrisFile.txt"
    isWindows match {
      case true =>
        Paths.get(fnamestr)
      case false =>
        Paths.get(fnamestr)
    }
  }
  lazy val maxLines      = 10
  lazy val testFileLines = (0 until maxLines).toList.map { _.toString }

  lazy val testfilename = "~/shellExecFileTest.out"
  lazy val testfileb = {
    val p = Paths.get(testfilename)
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
  lazy val gdrive = "g:/".path
  lazy val gdriveTests =
    if (gdrive.exists) { // should NOT really be a function of whether driver exists!
      List(
        ("/g", "g:\\"),
        ("/g/", "g:\\")
      )
    } else {
      List(
        ("/g", "g:\\"),
        ("/g/", "g:\\")
      )
    }
  lazy val expectedHomeDir = sys.props("user.home").replaceAll("/", "\\")
  lazy val fileDospathPairs = List(
    (".", here),
    (hereDrive, here), // jvm treats this as cwd, if on c:
    ("/q/", "q:\\"),   // assumes /etc/fstab mounts /cygdrive to /
    ("/q", "q:\\"),    // assumes /etc/fstab mounts /cygdrive to /
    ("/c/", "c:\\"),
    ("~", expectedHomeDir),
    ("~/", expectedHomeDir),
    ("/g", "g:\\"),
    ("/g/", "g:\\"),
    ("/c/data/", "c:\\data")
  ) ::: gdriveTests

  lazy val nonCanonicalDefaultDrive = {
    val dd = driveRoot.take(1).toLowerCase
    dd != "c"
  }
  lazy val username = sys.props("user.name").toLowerCase
  lazy val toStringPairs = List(
    (".", uhere),
    ("/q/", "/q"),
    ("/q/file", "/q/file"), // assumes there is no Q: drive
    (hereDrive, uhere),     // jvm treats bare drive letter as cwd, if default drive
    ("/c/", "/c"),
    ("~", s"/users/${username}"),
    ("~/", s"/users/${username}"),
    ("/g", "/g"),
    ("/g/", "/g"),
    ("/c/data/", "/data")
  )

  before {
    // vastblue.fileutils.touch(testfilename)
    testFile.withWriter() { (w: PrintWriter) =>
      testFileLines.foreach { line =>
        w.print(line + "\n")
      }
    }
  }
//  after {
////    if( testFile.exists ) testFile.delete()
////    if( testfileb.exists ) testfileb.delete()
//  }

  describe("File") {
    describe("#eachline") {
      // def parseCsvLine(line:String,columnTypes:String,delimiter:String="") = {
      it("should correctly deliver all file lines") {
        // val lines = testFile.lines
        System.out.printf("testFile[%s]\n", testFile)
        for ((line, lnum) <- testFile.lines.toSeq.zipWithIndex) {
          val expected = testFileLines(lnum)
          if (line != expected) {
            println(s"line ${lnum}:\n  [$line]\n  [$expected]")
          }
        }
        for ((line, lnum) <- testFile.lines.toSeq.zipWithIndex) {
          val expected = testFileLines(lnum)
          if (line != expected) {
            println(s"failure: line ${lnum}:\n  [$line]\n  [$expected]")
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
        testfileb.delete()
        val ok = !testfileb.exists
        if (ok)
          println(
            s"delete() successfull, and correctly detected by 'exists' method on path '$testfileb'"
          )
        assert(ok, s"error: can still see file '$testfileb'")
      }
    }
    // expected values of stdpath and localpath depend
    // on whether g:/ exists.
    // If so, c:/g is expected to resolve to /g and g:\\
//    lazy val (gu,gw) = os.dirExists("g:/") match {
//      case true =>  ("/g",  "g:\\")
//      case false => ("/c/g","c:\\g")
//    }
    if (isWindows) {
      printf("gdrive.exists: %s\n", gdrive.exists)
      printf("gdrive.isDirectory: %s\n", gdrive.isDirectory)
      printf("gdrive.isRegularFileg: %s\n", gdrive.isDirectory)
      printf("gdrive.isSymbolicLink: %s\n", gdrive.isSymbolicLink)
      describe("# dospath test") {
        it("should correctly handle cygwin dospath drive designations, when present") {
          var loop = -1
          for ((fname, expected) <- fileDospathPairs) {
            loop += 1
            printf("fname[%s], expected[%s]\n", fname, expected)
            val file = Paths.get(fname)
            printf("%-22s : %s\n", file.stdpath, file.exists)
            val a = expected.toLowerCase
            // val b = file.toString.toLowerCase
            // val c = file.localpath.toLowerCase
            val d = file.dospath.toLowerCase
            if (a == d) {
              println(s"a [$a] == d [$d]")
              assert(a == d)
            } else {
              printf("expected[%s]\n", expected.toLowerCase)
              printf("file.localpath[%s]\n", file.localpath.toLowerCase)
              printf(
                "error: expected[%s] not equal to dospath [%s]\n",
                expected.toLowerCase,
                file.localpath.toLowerCase
              )
              if (file.exists && new JFile(expected).exists) {
                assert(a == d)
              } else {
                println(s"file.exists and expected.exists: [$file] == d [$expected]")
              }
            }
          }
        }
      }
      describe("# toString test") {
        it("should correctly handle toString") {
          val upairs = toStringPairs.toArray.toSeq
          printf("%d pairs\n", upairs.size)
          for ((fname, expected) <- upairs) {
            if (true || verbose) {
              printf("=====================\n")
              printf("fname[%s]\n", fname)
              printf("expec[%s]\n", expected)
            }
            val file: Path = Paths.get(fname)
            printf("file.norm[%-22s] : %s\n", file.norm, file.exists)
            printf("file.stdpath[%-22s] : %s\n", file.stdpath, file.exists)
            val exp = expected.toLowerCase
            val std = file.stdpath.toLowerCase
            val nrm = file.norm.toLowerCase
            printf("exp[%s] : std[%s] : nrm[%s]\n", exp, std, nrm)
            // val c = file.localpath.toLowerCase
            // val d = file.dospath.toLowerCase
            if (!std.endsWith(exp)) {
              printf("error: toString[%s] doesn't end with expected[%s]\n", nrm, exp)
            }
            // note: in some cases (on Windows, for semi-absolute paths not on the default drive), the `stdpath` version`
            // of the path must include a `cygdrive` version of the drive letter.  This test is more subtle in order to
            // recognize this case.
            if (nonCanonicalDefaultDrive) {
              printf("hereDrive[%s]\n", hereDrive)
              if (std.endsWith(exp)) {
                println(s"std[$std].endsWith(exp[$exp]) for hereDrive[$hereDrive]");
              }
              assert(std.endsWith(exp))
            } else {
              if (exp == std) {
                println(s"std[$std] == exp[$exp]")
              } else {
                hook += 1
              }
              if (driveRoot.nonEmpty) {
                assert(exp == std) // || exp.drop(2) == std.drop(2) || std.contains(exp))
              } else {
                assert(exp == std)
              }
            }

          }
        }
      }

      def getVariants(p: Path): Seq[Path] = {
        val pstr = p.toString.toLowerCase
        def includeStdpath: Seq[String] = if (pstr.startsWith(workingDrive.string)) {
          List(p.stdpath)
        } else {
          Nil
        }

        val variants: Seq[String] = List(
          p.norm,
          p.toString,
          p.localpath,
          p.dospath
        ) ++ includeStdpath // stdpath fails round-trip test when default drive != C:

        val vlist = variants.distinct.map { s =>
          val p = Paths.get(s)
          if (p.toString.take(1).toLowerCase != pstr.take(1)) {
            hook += 1
          }
          p
        }
        vlist.distinct
      }
      describe("# File name consistency") {
        it("round trip conversions should be consistent") {
          for (
            fname <-
              (toStringPairs.toMap.keySet ++ fileDospathPairs.toMap.keySet).toList.distinct.sorted
          ) {
            val f1: Path            = Paths.get(fname)
            val variants: Seq[Path] = getVariants(f1)
            for (v <- variants) { // not necessarily 4 variants (duplicates removed before map to Path)
              // val (k1,k2) = (f1.key,v.key)
              if (f1 != v) {
                printf("f1[%s]\nv[%s]\n", f1, v)
              }
              if (f1.equals(v)) {
                println(s"f1[$f1] == v[$v]")
              }
              assert(f1.equals(v), s"f1[$f1] != variant v[$v]")
            }
          }
        }
      }
    }
  }
}
