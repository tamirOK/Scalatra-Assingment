val ScalatraVersion = "2.6.2"

organization := "Innopolis"

name := "Assignment1"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.4"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.8.v20171121" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
)
libraryDependencies += "org.scalatra" %% "scalatra-json" % "2.6.2"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.0-M2"
libraryDependencies += "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5"

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
