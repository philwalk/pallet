package vastblue.file

import org.scalatest.*
import vastblue.pathextend.*
import vastblue.file.Paths.{canExist, cwd, defaultDrive, isWindows, normPath}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class PathSpec extends AnyFunSpec with Matchers with BeforeAndAfter {
  var hook = 0
  lazy val TMP = {
    if (canExist("/g".path)) {
      val gdir = Paths.get("/g")
      // val str = gdir.localpath
      gdir.isDirectory && gdir.paths.contains("/tmp") match {
        case true =>
          "/g/tmp"
        case false =>
          "/tmp"
      }
    } else {
      "/tmp"
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
    Paths.get(homeDirTestFile)
  }
  lazy val here  = cwd.normalize.toString.toLowerCase
  lazy val uhere = here.replaceAll("[a-zA-Z]:", "").replace('\\', '/')
  lazy val hereDrive = here.replaceAll(":.*", ":") match {
    case drive if drive >= "a" && drive <= "z" =>
      drive
    case _ => ""
  }

  /** similar to gnu 'touch <filename>'.
    */
  def touch(targetFile: Path): Int = {
    var exitCode = 0
    try {
      // line ending is a place holder, if no lines.
      // targetFile.withWriter(){ _ => }
      targetFile.toFile.createNewFile()
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
    // create homeDirTestFile
    val tfpath = homeDirTestFile.path
    touch(tfpath)
    printf("homeDirTestFile: %s\n", homeDirTestFile)
    // create testFile
    testFile.withWriter() { w =>
      testFileLines.foreach { line =>
        w.print(line + "\n")
      }
    }
    printf("testFile: %s\n", testFile)
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
            System.err.println(s"line ${lnum}:\n  [$line]\n  [$expected]")
          }
        }
        for ((line, lnum) <- testFile.lines.toSeq.zipWithIndex) {
          val expected = testFileLines(lnum)
          if (line != expected) {
            System.err.println(s"line ${lnum}:\n  [$line]\n  [$expected]")
          }
          assert(line == expected, s"line ${lnum}:\n  [$line]\n  [$expected]")
        }
      }
    }

    describe("#tilde-in-path-test") {
      it("should see file in user home directory if present") {
        val ok = testfileb.exists
        assert(ok, s"error: cannot see file '$testfileb'")
      }
      it("should NOT see file in user home directory if NOT present") {
        val test: Boolean = testfileb.delete()
        val ok            = !testfileb.exists || !test
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
      val expectedHomeDir = sys.props("user.home").replace('/', '\\')
      val gdrive          = Paths.get("g:/")
      printf("gdrive.exists: %s\n", gdrive.exists)
      printf("gdrive.isDirectory: %s\n", gdrive.isDirectory)
      printf("gdrive.isRegularFileg: %s\n", gdrive.isDirectory)
      printf("gdrive.isSymbolicLink: %s\n", gdrive.isSymbolicLink)
      val gdriveTests =
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
      lazy val pathDospathPairs = List(
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

      describe("# Path dospath test") {
        it("should correctly handle cygwin dospath drive designations, when present") {
          var loop = -1
          for ((fname, expected) <- pathDospathPairs) {
            loop += 1
            val file = Paths.get(fname)
            printf("%-22s : %s\n", file.stdpath, file.exists)
            val a = expected.toLowerCase
            // val b = file.toString.toLowerCase
            // val c = file.localpath.toLowerCase
            if (fname == "/g") {
              hook += 1
            }
            // def abs(p: Path) = p.toAbsolutePath.normalize
            val d        = file.dospath.toLowerCase
            val df       = normPath(d)
            val af       = normPath(a)
            val sameFile = Paths.isSameFile(af, df)
            if (!sameFile) {
              System.err.printf("expected[%s]\n", expected.toLowerCase)
              System.err.printf("file.localpath[%s]\n", file.localpath.toLowerCase)
              System.err.printf(
                "error: expected[%s] not equal to dospath [%s]\n",
                expected.toLowerCase,
                file.localpath.toLowerCase
              )
              if (file.exists && new JFile(expected).exists) {
                assert(a == d)
              }
            }
          }
        }
      }
      lazy val nonCanonicalDefaultDrive = defaultDrive != "c:"
      lazy val username                 = sys.props("user.name").toLowerCase
      lazy val pathToStringPairs = List(
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
      describe("# Path stdpath test") {
        it("should correctly handle path toString") {
          val upairs = pathToStringPairs.toArray.toSeq
          printf("%d pairs\n", upairs.size)
          var loop = -1
          for ((fname, expected) <- upairs) {
            loop += 1
            if (fname.startsWith("/q")) {
              hook += 1
            }
            val file: Path = Paths.get(fname).toAbsolutePath.normalize()
            printf("%-22s : %s\n", file.stdpath, file.exists)
            val exp = expected.toLowerCase
            val std = file.stdpath.toLowerCase
            // val loc = file.localpath.toLowerCase
            // val dos = file.dospath.toLowerCase
            if (nonCanonicalDefaultDrive) {
              if (!std.endsWith(exp)) {
                System.err.printf("error: stdpath[%s] not endsWith exp[%s]\n", std, exp)
              }
              assert(
                std.endsWith(expected)
              ) // in this case, there should also be a cygdrive prefix (e.g., "/c")
            } else {
              if (exp != std) {
                System.err.printf("error: expected[%s] not equal to toString [%s]\n", exp, std)
              }
              assert(exp == std)
            }

          }
        }
      }
      def getVariants(p: Path): Seq[Path] = {
        val stdpathToo = if (nonCanonicalDefaultDrive) Nil else Seq(p.stdpath)
        val variants: Seq[String] = Seq(
          p.toString,
          p.localpath,
          p.dospath
        ) ++ stdpathToo

        variants.distinct.map { s =>
          if (s == "q:/file") {
            hook += 1
          }
          val u = Paths.get(s)
          u
        }
      }
      describe("# Path consistency") {
        it("round trip conversions should be consistent") {
          for (
            fname <-
              (pathToStringPairs.toMap.keySet ++ pathDospathPairs.toMap.keySet).toList.distinct.sorted
          ) {
            if (fname == "/q/file") {
              hook += 1
            }
            val f1                  = Paths.get(fname)
            val variants: Seq[Path] = getVariants(f1)
            for (v <- variants) { // not necessarily 4 variants (duplicates removed before map to Path)
              // val (k1,k2) = (f1.key,v.key)
              val sameFile = Paths.isSameFile(f1, v)
              if (!sameFile) {
                System.err.printf("f1[%s]\nv[%s]\n", f1, v)
              }
              assert(sameFile, s"f1[$f1] != variant v[$v]")
            }
          }
        }
      }
    }
  }
}
