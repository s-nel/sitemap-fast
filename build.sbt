organization := "com.snacktrace"
name := "sitemap-fast"
version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-lang" % "scala-xml" % "2.11.0-M4"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.29"
libraryDependencies += "co.fs2" %% "fs2-core" % "0.9.0-RC2"
libraryDependencies += "co.fs2" %% "fs2-io" % "0.9.0-RC2"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % Test
libraryDependencies += "org.easymock" % "easymock" % "3.4" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7" % Test

    