scalaVersion := "2.12.20"

val SONATYPE_VERSION = sys.env.getOrElse("SONATYPE_VERSION", "3.12.2") // "3.10.0")

//addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.3") // "0.4.17")

addSbtPlugin("ch.epfl.scala"  % "sbt-bloop"     % "2.0.10")
addSbtPlugin("ch.epfl.scala"  % "sbt-scalafix"  % "0.14.3")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype"  % SONATYPE_VERSION)
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"  % "2.5.4")
addSbtPlugin("com.github.sbt" % "sbt-dynver"    % "5.1.0")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.3.1")

addDependencyTreePlugin

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

//resolvers += Resolver.sonatypeRepo("snapshots")
resolvers ++= Resolver.sonatypeOssRepos("snapshots")
resolvers ++= Resolver.sonatypeOssRepos("releases")
