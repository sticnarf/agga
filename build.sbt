name := "agga"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "com.typesafe.akka" %% "akka-remote" % "2.5.6",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.6",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.6" % Test
)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)