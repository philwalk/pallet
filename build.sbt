lazy val scala213 = "2.13.12"
lazy val scala331 = "3.3.1"
lazy val scalaVer = scala331

lazy val supportedScalaVersions = List(scala213, scala331)
//lazy val supportedScalaVersions = List(scalaVer)

//ThisBuild / envFileName  := "dev.env" // sbt-dotenv plugin gets build environment here
ThisBuild / scalaVersion := scalaVer
ThisBuild / version      := "0.8.4-SNAPSHOT"

ThisBuild / organization         := "org.vastblue"
ThisBuild / organizationName     := "vastblue.org"
ThisBuild / organizationHomepage := Some(url("https://vastblue.org/"))

//cancelable in Global := true

parallelExecution := false

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/philwalk/pallet"),
    "scm:git@github.com:philwalk/pallet.git"
  )
)

ThisBuild / developers.withRank(KeyRanks.Invisible) := List(
  Developer(
    id = "philwalk",
    name = "Phil Walker",
    email = "philwalk9@gmail.com",
    url = url("https://github.com/philwalk")
  )
)

// Remove all additional repository other than Maven Central from POM
ThisBuild / publishTo := {
  // For accounts created after Feb 2021:
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

ThisBuild / publishMavenStyle.withRank(KeyRanks.Invisible) := true

ThisBuild / crossScalaVersions := supportedScalaVersions

// For all Sonatype accounts created on or after February 2021
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

resolvers += Resolver.mavenLocal

publishTo := sonatypePublishToBundle.value

lazy val root = (project in file(".")).settings(
  crossScalaVersions := supportedScalaVersions,
  name               := "pallet"
)

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck"      % "1.17.0" % Test,
  "org.scalatest"  %% "scalatest"       % "3.2.17" % Test,
  "com.github.sbt"  % "junit-interface" % "0.13.3" % Test
)

// If you created a new account on or after February 2021, add sonatypeCredentialHost settings:

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
