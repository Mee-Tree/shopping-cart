import Dependencies._

ThisBuild / organization := "dev.meetree"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version      := "0.1.0"

ThisBuild / evictionErrorLevel := Level.Warn
ThisBuild / libraryDependencySchemes ++= Schemes.all

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val commons = Seq(
  fork      := true,
  maxErrors := 5,
  scalacOptions ++= Seq(
    "-Ymacro-annotations",
    "-Wunused",
    "-feature",
    "-deprecation",
    "-language:implicitConversions"
  )
)

lazy val root = (project in file("."))
  .settings(name := "shop")
  .aggregate(core, test)

lazy val core = (project in file("modules/core"))
  .settings(commons: _*)
  .settings(
    name := "shop-core",
    libraryDependencies ++= Libs.core,
    libraryDependencies ++= CompilerPlugins.core
  )

lazy val DeepIntegrationTest = IntegrationTest extend (Test)

lazy val test = (project in file("modules/test"))
  .dependsOn(core)
  .configs(DeepIntegrationTest)
  .settings(
    Defaults.itSettings,
    scalafixConfigSettings(DeepIntegrationTest)
  )
  .settings(commons: _*)
  .settings(
    name := "shop-test",
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies ++= Libs.test,
    libraryDependencies ++= CompilerPlugins.test
  )
