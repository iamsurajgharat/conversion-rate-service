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

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.surajgharat.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.surajgharat.binders._"
