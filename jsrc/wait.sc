#!/usr/bin/env -S scala
package vastblue

import vastblue.pathextend._
import vastblue.Platform._

object Wait {
  def main(args: Array[String]): Unit = {
    val seconds: Int = if (args.isEmpty || args.head.startsWith("-")) 10 else args.head.toInt
    printf("waiting %d seconds ...", seconds)
    Thread.sleep(seconds * 1000L)
    printf("\n")
  }
}