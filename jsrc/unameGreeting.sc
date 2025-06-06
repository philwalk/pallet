#!/usr/bin/env -S scala

//> using scala 3.4.3
//> using dep "org.vastblue::pallet::0.11.0"

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
