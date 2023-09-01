lazy val scala213 = "2.13.11"
lazy val scala330 = "3.3.0"
//lazy val supportedScalaVersions = List(scala213, scala330)
lazy val supportedScalaVersions = List(scala330)

//ThisBuild / envFileName  := "dev.env" // sbt-dotenv plugin gets build environment here
ThisBuild / organization := "org.vastblue"
ThisBuild / scalaVersion := "3.3.0"
ThisBuild / version      := "0.8.1-SNAPSHOT"

ThisBuild / crossScalaVersions := supportedScalaVersions

lazy val root = (project in file("."))
  .settings(
//  crossScalaVersions := supportedScalaVersions,
    name := "pallet"
  )

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck"      % "1.17.0" % Test,
  "org.scalatest"  %% "scalatest"       % "3.2.16" % Test,
  "com.github.sbt"  % "junit-interface" % "0.13.3" % Test,
)
//def rtp: String = {
//  val psep = java.io.File.pathSeparator
//  val syspath: List[String] = Option(System.getenv("PATH")).getOrElse("").split(psep).map { _.toString }.toList
//  val javaLibraryPath: List[String] = sys.props("java.library.path").split(psep).map { _.toString }.toList
//  val entries: List[String] = (javaLibraryPath ::: syspath)
//  val path: String = entries.map { _.replace('\\', '/').toLowerCase }.distinct.mkString(";")
//  System.setProperty("java.library.path", path)
//  path
//}
//lazy val runtimePath = settingKey[String]("runtime path")
//
//runtimePath := rtp

scalacOptions := Seq(
//"-Xmaxerrs", "10",
// Warnings as errors!
//"-Xfatal-warnings", // must be commented out for scalafix actions, pre-2.13
//"-Wconf:any:error", // must be commented out for scalafix actions, 2.13+

//"-Wvalue-discard",

  "-encoding", "utf-8",
  "-explaintypes",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",

  // Linting options
  "-unchecked",

  "-Wunused:implicits",
  "-Wunused:imports",
  "-Wunused:locals",
  "-Wunused:params",
  "-Wunused:privates",
)
scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, _)) => Seq(
    "-Xsource:3",
    "-Xmaxerrs", "10",
    "-Yscala3-implicit-resolution",
    "-language:implicitConversions",
  )
  case _ => Nil
})


