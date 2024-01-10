#!/usr/bin/env -S scala

//> using scala "3.3.1"
//> using lib "org.vastblue::pallet::0.10.6"

import vastblue.pallet.*

object UnameGreeting {
  def main(args: Array[String]): Unit = {
    printf("uname / osType / osName:\n%s\n", s"platform info: ${unameShort} / ${osType} / ${osName}")
    if (isLinux) {
      // uname is "Linux"
      printf("hello Linux\n")
    } else if (isDarwin) {
      // uname is "Darwin*"
      printf("hello Mac\n")
    } else if (isWinshell) {
      // isWinshell: Boolean = isMsys | isCygwin | isMingw | isGitSdk | isGitbash
      printf("hello %s\n", unameShort)
    } else if (envOrEmpty("MSYSTEM").nonEmpty) {
      printf("hello %s\n", envOrEmpty("MSYSTEM"))
    } else {
      assert(isWindows, s"unknown environment: ${unameLong} / ${osType} / ${osName}")
      printf("hello Windows\n")
    }
  }
}
