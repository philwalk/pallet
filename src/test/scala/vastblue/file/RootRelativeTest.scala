package vastblue.file

import vastblue.pathextend.*
import vastblue.file.Paths.*
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class RootRelativeTest extends AnyFunSpec with Matchers with BeforeAndAfter {
  describe("Root-relative paths") {
    it("should correctly resolve pathRelative paths in Windows") {
      // NOTE: current working directory is set before running test (e.g., in IDE)
      val currentWorkingDirectory = Paths.get(".").toAbsolutePath.getRoot.toString.take(2)
      if (currentWorkingDirectory.contains(":")) {
        // windows os
        printf("cwd: %s\n", currentWorkingDirectory)
        val testdirs = Seq("/opt", "/OPT", "/$RECYCLE.BIN")
        val mounts   = reverseMountMap.keySet.toArray
        for (testdir <- testdirs) {
          val mounted = mounts.find((dir: String) => sameFile(dir, testdir))
          val thisPath = mounted match {
            case Some(str) =>
              reverseMountMap(str)
            case None =>
              testdir
          }
          val jf = Paths.get(thisPath)
          printf("[%s]: exists [%s]\n", jf.norm, jf.exists)
          val sameDriveLetter = jf.toString.take(2).equalsIgnoreCase(currentWorkingDirectory)
          if (mounted.isEmpty && !sameDriveLetter) {
            hook += 1
          }
          // if path is not affected by mount map, drive letters must match
          assert(mounted.nonEmpty || sameDriveLetter)
        }
      }
    }

    it("should correctly apply mountMap") {
      printf("%s\n", envpath)
      printf("%s\n", jvmpath)
      val mounts = reverseMountMap.keySet.toArray
      if (mounts.nonEmpty) {
        val testdirs = Seq("/opt", "/optx")
        for (dir <- testdirs) {
          val mounted = mounts.find((s: String) => sameFile(s, dir))
          val thisPath = mounted match {
            case Some(str) =>
              reverseMountMap(str)
            case None =>
              dir
          }
          val jf = java.nio.file.Paths.get(thisPath)
          printf("[%s]: exists [%s]\n", jf.norm, jf.exists)
          val testdir = java.nio.file.Paths.get(dir)
          if (mounted.nonEmpty != testdir.exists) {
            hook += 1
          }
          assert(mounted.nonEmpty == testdir.exists)
        }
      }
    }
  }

  def envpath: String = {
    val psep = java.io.File.pathSeparator
    val entries: List[String] =
      Option(System.getenv("PATH")).getOrElse("").split(psep).map { _.toString }.toList
    val path: String = entries.map { _.replace('\\', '/').toLowerCase }.distinct.mkString(";")
    path
  }
  def jvmpath: String = {
    val psep                  = java.io.File.pathSeparator
    val entries: List[String] = sys.props("java.library.path").split(psep).map { _.toString }.toList
    val path: String = entries.map { _.replace('\\', '/').toLowerCase }.distinct.mkString(";")
    path
  }
}
