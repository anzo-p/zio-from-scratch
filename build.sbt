ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.9"

lazy val root = (project in file("."))
  .settings(
    name := "zio-from-scratch"
  )

libraryDependencies ++= Seq(
  "org.scalatest"     %% "scalatest"       % "3.2.10"   % Test,
  "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % Test
)
