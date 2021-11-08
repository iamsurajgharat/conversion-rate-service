name := """conversion-rate-service"""
organization := "com.surajgharat"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.6"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.30.0"
libraryDependencies += "dev.zio" %% "zio" % "2.0.0-M4"
libraryDependencies += "org.mockito" %% "mockito-scala" % "1.16.46"
libraryDependencies += jdbc
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.3",
 // "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc42"
)
libraryDependencies += "com.github.tototoshi" %% "slick-joda-mapper" % "2.4.2"
libraryDependencies += "org.joda" % "joda-convert" % "1.7"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.surajgharat.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.surajgharat.binders._"
