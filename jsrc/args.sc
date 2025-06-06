#!/usr/bin/env -S scala-cli shebang

//> using dep "org.vastblue::pallet::0.11.0"
import vastblue.pallet.*

// display default args
for (arg <- args) {
  printf("arg [%s]\n", arg)
}
// display extended and repaired args
val argv = prepArgv(args.toSeq)
for ((arg, i) <- argv.zipWithIndex) {
  printf(" %2d: [%s]\n", i, arg)
}
