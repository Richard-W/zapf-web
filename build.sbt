name := "zapf-web"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers += "Bintray Richard-W" at "https://dl.bintray.com/richard-w/maven"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.4.0",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "angularjs" % "1.3.15",
  "com.typesafe.play" %% "play-mailer" % "3.0.0-RC1",
  "xyz.wiedenhoeft" %% "play-authenticator" % "0.1.1"
)

routesGenerator := InjectedRoutesGenerator

lazy val root = (project in file(".")).enablePlugins(PlayScala)

