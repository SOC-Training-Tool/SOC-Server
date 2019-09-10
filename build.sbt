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
//val grpc = "io.grpc" % "grpc-netty" % "scalapb.compiler.Version.grpcJavaVersion"
// val scalapb = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % "scalapb.compiler.Version.scalapbVersion"

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)



libraryDependencies ++= Seq(scalatest, akka, akkatyped, akkatest, s3, ddb, junit) ++ circe
libraryDependencies ++= Seq(
    "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
)
//libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

// Seeminlgy needed to set this once to get things to work?
//PB.protocVersion := "-v300"


// No need to run tests while building jar
test in assembly := {}
// Simple and constant jar name
assemblyJarName in assembly := s"app-assembly.jar"
// Merge strategy for assembling conflicts
assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}
