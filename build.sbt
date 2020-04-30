name := "scala-labs"

version := "0.1"

scalaVersion := "2.12.11"
scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.0.0",
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.1.1",
  "org.scalacheck" %% "scalacheck" % "1.14.3",
  "org.scalatestplus" %% "scalacheck-1-14" % "3.1.1.1"
).map(_ % Test)
