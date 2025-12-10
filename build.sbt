ThisBuild / scalaVersion := "3.4.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.greencreeper"
ThisBuild / organizationName := "greencreeper"

lazy val root = (project in file("."))
  .settings(
    name := "zest",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "cats-effect" % "3.5.3",
      "org.typelevel" %% "cats-parse" % "1.0.0",
      "co.fs2" %% "fs2-core" % "3.9.3",
      "co.fs2" %% "fs2-io" % "3.9.3",
      "com.lihaoyi" %% "os-lib" % "0.9.1", // Added for temporary file creation in tests
      "org.scalameta" %% "munit" % "1.0.0-M10" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.0.0-M4" % Test,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
    )
  )