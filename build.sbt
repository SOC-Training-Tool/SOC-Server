name := "SettlersOfCatan"

version := "0.1"

scalaVersion := "2.12.8"

val circeVersion = "0.11.1"

val scalatest = "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test"
val akkatest = "com.typesafe.akka" %% "akka-testkit" % "2.5.23" % "test"
val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.23"
val akkatyped = "com.typesafe.akka" %% "akka-actor-typed" % "2.5.23"
val s3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.311"
val ddb = "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.311"
val junit = "junit" % "junit" % "4.11"

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)


libraryDependencies ++= Seq(scalatest, akka, akkatyped, akkatest, s3, ddb, junit) ++ circe
 
