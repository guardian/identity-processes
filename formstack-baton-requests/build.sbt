
name := "formstack-baton-requests"

version := "0.1"

scalaVersion := "2.12.8"
val circeVersion = "0.13.0"
val amazonSdkVersion = "1.11.755"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-java-sdk-s3" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-lambda" % amazonSdkVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.jlib" % "jlib-awslambda-logback" % "1.0.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

scalacOptions += "-Ypartial-unification"

assemblyJarName := "main.jar"

assemblyMergeStrategy in assembly := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

enablePlugins(RiffRaffArtifact)
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "formstack-baton-requests-cfn/cloud-formation.yaml")