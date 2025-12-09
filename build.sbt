ThisBuild / organization := "com.block.zest"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.1"

val catsEffectVersion = "3.5.7"
val munitVersion = "1.0.2"

lazy val root = (project in file("."))
  .aggregate(core, examples)
  .settings(
    name := "zest-root"
  )

lazy val core = (project in file("zest-core"))
  .settings(
    name := "zest-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.scalameta" %% "munit" % munitVersion % Test
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Ykind-projector:underscores",
      "-source:future"
    )
  )

lazy val examples = (project in file("examples"))
  .dependsOn(core)
  .settings(
    name := "zest-examples",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion
    )
  )
