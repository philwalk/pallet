#!/usr/bin/env -S scala @./atFile
package vastblue

import vastblue.pathextend._

def main(args: Array[String]): Unit = {
  printf("[%s]\n", sys.props("sun.java.command"))
  for (arg <- args){
    printf("arg [%s]\n", arg)
  }
  for (arg <- scriptArgv){
    printf("argv[%s]\n", arg)
  }
}
