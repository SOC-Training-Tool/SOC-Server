name := "SettlersOfCatan"

version := "0.1"

scalaVersion := "2.12.8"

val circeVersion = "0.11.1"

val scalatest = "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test"
val akkatest = "com.typesafe.akka" %% "akka-testkit" % "2.5.23" % "test"
val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.23"
val akkatyped = "com.typesafe.akka" %% "akka-actor-typed" % "2.5.23"

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)


libraryDependencies ++= Seq(scalatest, akka, akkatyped, akkatest) ++ circe 
 
