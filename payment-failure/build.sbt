name := "payment-failure-lambda"

organization := "com.gu"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.2"

)

assemblyJarName := "main.jar"

enablePlugins(RiffRaffArtifact)
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "payment-failure-cfn/cloud-formation.yaml")
