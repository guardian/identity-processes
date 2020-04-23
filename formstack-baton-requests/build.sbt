name := "formstack-baton-requests"

version := "0.1"

scalaVersion := "2.12.8"
val circeVersion = "0.13.0"
val amazonSdkVersion = "1.11.755"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.gu" %% "scanamo" % "1.0.0-M6" excludeAll ExclusionRule(organization = "com.amazonaws"), //brings obsolete aws version 1.11.256
  "com.amazonaws" % "aws-java-sdk-dynamodb" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-lambda" % amazonSdkVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.jlib" % "jlib-awslambda-logback" % "1.0.0",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.1"
)

scalacOptions += "-Ypartial-unification"

assemblyJarName := "main.jar"

assemblyMergeStrategy in assembly := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

startDynamoDBLocal := startDynamoDBLocal.dependsOn(compile in Test).value
test in Test := (test in Test).dependsOn(startDynamoDBLocal).value
testOnly in Test := (testOnly in Test).dependsOn(startDynamoDBLocal).evaluated
testOptions in Test += dynamoDBLocalTestCleanup.value

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

enablePlugins(RiffRaffArtifact)
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "formstack-baton-requests-cfn/cloud-formation.yaml")