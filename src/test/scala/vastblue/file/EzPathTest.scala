//#!/usr/bin/env -S scala -explain -cp target/scala-3.3.0/classes/*
package vastblue.file

import vastblue.file.EzPath._
//import vastblue.pathextend._
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class EzPathTest extends AnyFunSpec with Matchers with BeforeAndAfter {
  describe("EzPath constructors") {
    it("should correctly create and display EzPath objects") {
      val upathstr    = "/opt/ue"
      val wpathstr    = upathstr.replace('/', '\\')
      val posixAbsstr = s"$platformPrefix$upathstr" // can insert current working directory prefix
      val windowsAbsstr =
        posixAbsstr.replace('/', '\\') // windows version of java.io.File.separator
      val localAbsstr = if (isWindows) windowsAbsstr else posixAbsstr
      printf("notWindows: %s\n", notWindows)
      printf("isWindows: %s\n", isWindows)

      printf("\n")
      printf("upathstr      [%s]\n", upathstr)
      printf("wpathstr      [%s]\n", wpathstr)
      printf("posixAbsstr   [%s]\n", posixAbsstr)
      printf("windowsAbsstr [%s]\n", windowsAbsstr)
      printf("\n")

      // test whether input strings have forward or back slash
      // accept defaults (also should match Paths.get)

      val unxa = PathUnx(upathstr) // should match java.nio.file.Paths.get
      printf("unxa.pstr [%s], ", unxa.initstring)
      printf("unxa.norm [%s], ", unxa.norm)
      printf("unxa.sl   [%s], ", unxa.sl)
      printf("unxa.abs  [%s]\n", unxa.abs)
      // printf("\n")
      assert(unxa.sl == Slash.Unx)
      assert(unxa.initstring == upathstr)
      assert(unxa.abs == posixAbsstr)

      val wina = PathWin(upathstr) // should match java.nio.file.Paths.get
      printf("wina.pstr [%s], ", wina.initstring)
      printf("wina.norm [%s], ", wina.norm)
      printf("wina.sl   [%s], ", wina.sl)
      printf("wina.abs  [%s]\n", wina.abs)
      // printf("\n")
      assert(wina.sl == Slash.Win)
      assert(wina.initstring == upathstr)
      assert(wina.abs == localAbsstr.replace('/', Slash.win))

      val unxb = PathUnx(upathstr) // should match java.nio.file.Paths.get
      printf("unxb.pstr [%s], ", unxb.initstring)
      printf("unxb.norm [%s], ", unxb.norm)
      printf("unxb.sl   [%s], ", unxb.sl)
      printf("unxb.abs  [%s]\n", unxb.abs)
      // printf("\n")
      assert(unxb.sl == Slash.Unx)
      assert(unxb.initstring == upathstr)
      assert(unxb.abs == posixAbsstr)

      val winb = PathWin(upathstr) // should match java.nio.file.Paths.get
      printf("winb.pstr [%s], ", winb.initstring)
      printf("winb.norm [%s], ", winb.norm)
      printf("winb.sl   [%s], ", winb.sl)
      printf("winb.abs  [%s]\n", winb.abs)
      // printf("\n")
      assert(winb.sl == Slash.Win)
      assert(winb.initstring == upathstr)
      assert(winb.abs == localAbsstr.slash(winb.sl))

      val winw = PathWin(windowsAbsstr)
      printf("winw.pstr [%s], ", winw.initstring)
      printf("winw.norm [%s], ", winw.norm)
      printf("winw.sl   [%s], ", winw.sl)
      printf("winw.abs  [%s]\n", winw.abs)
      printf("\n")
      assert(winw.sl == Slash.Win)
      assert(winw.initstring == windowsAbsstr)
      assert(winw.abs == windowsAbsstr)

      val ezpc = EzPath(wpathstr) // should match java.nio.file.Paths.get
      printf("ezpc.pstr [%s], ", ezpc.initstring)
      printf("ezpc.norm [%s], ", ezpc.norm)
      printf("ezpc.sl   [%s], ", ezpc.sl)
      printf("ezpc.abs  [%s]\n", ezpc.abs)
      // printf("\n")
      assert(ezpc.sl == defaultSlash)
      if (isWindows) {
        assert(ezpc.initstring == wpathstr)
        assert(ezpc.abs == localAbsstr.replace('/', Slash.win))
      } else {
        assert(ezpc.initstring == upathstr)
        assert(ezpc.abs == localAbsstr)
      }

      val ezpd = EzPath(wpathstr) // should match java.nio.file.Paths.get
      printf("ezpd.pstr [%s], ", ezpd.initstring)
      printf("ezpd.norm [%s], ", ezpd.norm)
      printf("ezpd.sl   [%s], ", ezpd.sl)
      printf("ezpd.abs  [%s]\n", ezpd.abs)
      printf("\n")
      assert(ezpc.sl == defaultSlash)
      if (isWindows) {
        assert(ezpd.initstring == wpathstr)
        assert(ezpd.abs == windowsAbsstr)
      } else {
        assert(ezpc.initstring == upathstr)
        assert(ezpc.abs == localAbsstr)
      }

      val ezxw = EzPath(wpathstr, Slash.Win) // should match specified (same as default) slash
      printf("ezxw.pstr [%s], ", ezxw.initstring)
      printf("ezxw.norm [%s], ", ezxw.norm)
      printf("ezxw.sl   [%s], ", ezxw.sl)
      printf("ezxw.abs  [%s]\n", ezxw.abs)
      // printf("\n")
      assert(ezxw.sl == Slash.Win)
      if (isWindows) {
        assert(ezxw.initstring == wpathstr)
        assert(ezxw.abs == windowsAbsstr)
      } else {
        assert(ezpc.initstring == upathstr)
        assert(ezpc.abs == localAbsstr)
      }

      val ezpu = EzPath(wpathstr, Slash.Unx) // should match non-default slash
      printf("ezpu.pstr [%s], ", ezpu.initstring)
      printf("ezpu.norm [%s], ", ezpu.norm)
      printf("ezpu.sl   [%s], ", ezpu.sl)
      printf("ezpu.abs  [%s]\n", ezpu.abs)
      printf("\n")
      assert(ezpu.sl == Slash.Unx)
      if (isWindows) {
        assert(ezpu.initstring == wpathstr)
        assert(ezpu.abs == posixAbsstr)
      } else {
        assert(ezpc.initstring == upathstr)
        assert(ezpc.abs == localAbsstr)
      }
    }
  }
}
