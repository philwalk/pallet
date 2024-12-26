#!/usr/bin/env -S scala-cli shebang

//> using scala "3.4.3"
//> using dep "org.vastblue::unifile::0.3.6"
//> using dep "org.vastblue::pallet::0.10.16"

//import vastblue.pallet.*
import vastblue.unifile.*

object FstabCli {
  def main(args: Array[String]): Unit = {
    // `shellRoot` is the native path corresponding to "/"
    // display the native path and lines.size of /etc/fstab
    val p = Paths.get("/etc/fstab")
    // format: off
    printf("env: %-10s| shellRoot: %-12s| %-22s| %d lines\n",
      uname("-o"), shellRoot, p.posx, p.lines.size)
    printf("env: %-10s| posixroot: %-12s| %-22s| %d lines\n",
      uname("-o"), posixroot, p.posx, p.lines.size)
  }
}
FstabCli.main(args)
