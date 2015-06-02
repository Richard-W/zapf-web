name := "zapf-web"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.4.0",
  "org.webjars" % "bootstrap" % "3.3.4",
  "xyz.wiedenhoeft" %% "scalacrypt" % "0.4-SNAPSHOT",
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

