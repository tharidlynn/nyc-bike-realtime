import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "Example, Inc."

lazy val root = (project in file("."))
  .settings(
    name := "nyc-bike",
    libraryDependencies += scalaTest % Test,
    libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.1",
    libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.3",
    libraryDependencies += "org.apache.kafka" %% "kafka" % "2.1.1",
    libraryDependencies += "org.apache.kafka" %% "kafka-streams-scala" % "2.0.0",
    libraryDependencies += "javax.ws.rs" % "javax.ws.rs-api" % "2.1" artifacts(Artifact("javax.ws.rs-api", "jar", "jar"))
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
