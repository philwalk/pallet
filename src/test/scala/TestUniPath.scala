import org.junit.Test
import vastblue.pallet._
import vastblue.Platform // .{where, } // isWinshell

class TestUniPath {
  val verbose = Option(System.getenv("VERBOSE_TESTS")).nonEmpty

  def testArgs = Seq.empty[String]
  @Test def test1(): Unit = {
    val wherebash = where("bash")
    val test      = Paths.get(wherebash)
    printf("bash [%s]\n", test)
    val bashVersion: String = exec(where("bash"), "-version")
    printf("%s\n%s\n", test, bashVersion)
    printf("bashPath     [%s]\n", bashPath)
    printf("shellRoot    [%s]\n", Platform._shellRoot)
    printf("systemDrive: [%s]\n", Platform.driveRoot)
    printf("shellDrive   [%s]\n", Platform.shellDrive)
    printf("shellBaseDir [%s]\n", Platform.shellBaseDir)
    printf("osName       [%s]\n", osName)
    printf("unamefull    [%s]\n", unameLong)
    printf("unameshort   [%s]\n", unameShort)
    printf("isCygwin     [%s]\n", isCygwin)
    printf("isMsys64     [%s]\n", isMsys)
    printf("isMingw64    [%s]\n", isMingw)
    printf("isGitSdk64   [%s]\n", isGitSdk)
    printf("isWinshell   [%s]\n", isWinshell)
    printf("isLinux      [%s]\n", isLinux)
    printf("bash in path [%s]\n", Platform.findInPath("bash").getOrElse(""))
    printf("/etc/fstab   [%s]\n", Paths.get("/etc/fstab"))
    // dependent on /etc/fstab, in winshell environment
    printf("javaHome     [%s]\n", javaHome)

    printf("\n")
    printf("all bash in path:\n")
    val bashlist = Platform.findAllInPath("bash")
    for (path <- bashlist) {
      printf(" found at %-36s : ", s"[$path]")
      printf("--version: [%s]\n", exec(path.toString, "--version").takeWhile(_ != '('))
    }
    for ((key, valu) <- Platform.reverseMountMap) {
      printf("mount %-22s -> %s\n", key, valu)
    }
    assert(bashPath.exists == bashPath.isFile, s"bash not found")
  }
}
