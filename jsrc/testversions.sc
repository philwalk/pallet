#!/usr/bin/env -S scala @classpathAtfile

import vastblue.pathextend._
import scala.sys.process._

def main(args: Array[String]): Unit =
  // show runtime scala VERSION
  val scalaHome = sys.props("scala.home")
  val version = Paths.get(s"$scalaHome/VERSION").contentAsString.trim
  printf("%s\n",version)
  
  // display output of uname -a
  printf("%s\n",Seq("uname","-a").lazyLines_!.toList.mkString(""))
