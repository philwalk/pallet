import org.junit.Test

import vastblue.file.Paths.*
import vastblue.pathextend.*

class TestUniPath {
  def testArgs = Seq.empty[String]
  @Test def test1(): Unit = {
    val wherebash = where("bash")
    val test = Paths.get(wherebash)
    printf("bash [%s]\n",test)
    val bashVersion: String = exec(where("bash"),"-version")
    printf("%s\n%s\n", test, bashVersion)
    printf("bashPath     [%s]\n",bashPath)
    printf("shellRoot    [%s]\n",shellRoot)
    printf("systemDrive: [%s]\n",systemDrive)
    printf("shellDrive   [%s]\n",shellDrive)
    printf("shellBaseDir [%s]\n",shellBaseDir)
    printf("osName       [%s]\n",osName)
    printf("unamefull    [%s]\n",unamefull)
    printf("unameshort   [%s]\n",unameshort)
    printf("isCygwin     [%s]\n",isCygwin)
    printf("isMsys64     [%s]\n",isMsys64)
    printf("isMingw64    [%s]\n",isMingw64)
    printf("isGitSdk64   [%s]\n",isGitSdk64)
    printf("isWinshell   [%s]\n",isWinshell)
    printf("bash in path [%s]\n",findInPath("bash").getOrElse(""))
    printf("/etc/fstab   [%s]\n",Paths.get("/etc/fstab"))
    // dependent on /etc/fstab, in winshell environment
    printf("javaHome     [%s]\n",javaHome)

    printf("\n")
    printf("all bash in path:\n")
    val bashlist = findAllInPath("bash")
    for (path <- bashlist) {
      printf(" found at %-36s : ", s"[$path]")
      printf("--version: [%s]\n", exec(path.toString, "--version").takeWhile(_ != '('))
    }
    for ((key,valu) <- mountMap ){
      printf("mount %-22s -> %s\n", key, valu)
    }
    assert(bashPath.file.exists,s"bash not found")
  }
}
