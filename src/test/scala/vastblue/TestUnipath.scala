package vastblue

import vastblue.pallet.*
import vastblue.Platform.*
import vastblue.file.MountMapper
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import vastblue.file.MountMapper.reverseMountMap

// TODO: 
// add verification tests:
//    at most one of `isCygwin`, `isMsys`, etc. are true
//    uname is known or believable
//    etc.
class TestUniPath extends AnyFunSpec with Matchers {
  describe ("Unipath") {
    it ("should display discovered environment") {
      val wherebash = _where("bash")
      val test      = Paths.get(wherebash)
      printf("bash [%s]\n", test)
      val bashVersion: String = _exec(_where("bash"), "-version")
      printf("%s\n%s\n", test, bashVersion)
      printf("bashPath     [%s]\n", bashPath)
      printf("shellRoot    [%s]\n", shellRoot)
      printf("systemDrive: [%s]\n", driveRoot)
      printf("shellDrive   [%s]\n", shellDrive)
      printf("shellBaseDir [%s]\n", shellBaseDir)
      printf("osName       [%s]\n", osName)
      printf("unamefull    [%s]\n", unameLong)
      printf("unameshort   [%s]\n", unameShort)
      printf("isCygwin     [%s]\n", isCygwin)
      printf("isMsys64     [%s]\n", isMsys)
      printf("isMingw64    [%s]\n", isMingw)
      printf("isGitSdk64   [%s]\n", isGitSdk)
      printf("isWinshell   [%s]\n", isWinshell)
      printf("isLinux      [%s]\n", isLinux)
      printf("bash in path [%s]\n", findInPath("bash").getOrElse(""))
      printf("/etc/fstab   [%s]\n", Paths.get("/etc/fstab"))
      // dependent on /etc/fstab, in winshell environment
      printf("javaHome     [%s]\n", _javaHome)

      printf("\n")
      printf("all bash in path:\n")
      val bashlist = findAllInPath("bash")
      for (path <- bashlist) {
        printf(" found at %-36s : ", s"[$path]")
        printf("--version: [%s]\n", _exec(path.toString, "--version").takeWhile(_ != '('))
      }
      for ((key, valu) <- reverseMountMap) {
        printf("mount %-22s -> %s\n", key, valu)
      }
      val bpExists: Boolean = _bashPath.exists
      val bpIsFile: Boolean = _bashPath.isFile
      assert(bpExists == bpIsFile, s"bash not found")
    }
  }
}
