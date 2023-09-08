lazy val scala213               = "2.13.11"
lazy val scala331               = "3.3.1"
lazy val supportedScalaVersions = List(scala331)

//ThisBuild / envFileName  := "dev.env" // sbt-dotenv plugin gets build environment here
ThisBuild / organization := "org.vastblue"
ThisBuild / scalaVersion := scala331
ThisBuild / version      := "0.8.1-SNAPSHOT"

ThisBuild / crossScalaVersions := supportedScalaVersions

lazy val root = (project in file("."))
  .settings(
    crossScalaVersions := supportedScalaVersions,
    name               := "pallet"
  )

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck"      % "1.17.0" % Test,
  "org.scalatest"  %% "scalatest"       % "3.2.17" % Test,
  "com.github.sbt"  % "junit-interface" % "0.13.3" % Test
)

/*
 * build.sbt
 * SemanticDB is enabled for all sub-projects via ThisBuild scope.
 * https://www.scala-sbt.org/1.x/docs/sbt-1.3-Release-Notes.html#SemanticDB+support
 */
inThisBuild(
  List(
    scalaVersion      := "3.3.1", // 2.13.12, or 3.x
    semanticdbEnabled := true     // enable SemanticDB
    // semanticdbVersion := scalafixSemanticdb.revision // only required for Scala 2.x
  )
)

scalacOptions := Seq(
//"-Xmaxerrs", "10",
  "-encoding",
  "utf-8",
  "-explaintypes",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",

  // Linting options
  "-unchecked"
)
