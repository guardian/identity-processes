name := "payment-failure-lambda"

organization := "com.gu"

scalaVersion := "2.12.6"
val circeVersion = "0.10.1"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.2",
  "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.472",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test"
)

assemblyJarName := "main.jar"

enablePlugins(RiffRaffArtifact)
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "payment-failure-cfn/cloud-formation.yaml")
