name := "payment-failure-lambda"

organization := "com.gu"

scalaVersion := "2.12.6"
val circeVersion = "0.10.1"
val log4jVersion = "2.20.0"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.2",
  "com.amazonaws" % "aws-java-sdk-sqs" % "1.12.427",
  "com.beachape" %% "enumeratum" % "1.5.13",
  "com.beachape" %% "enumeratum-circe" % "1.5.21",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  // This provides a logback appender which can be used to ensure that multi-line log messages
  // are considered as single log events in cloudwatch. The logback.xml defines a root logger using this appender.
  ("org.jlib" % "jlib-awslambda-logback" % "1.0.0").exclude("org.slf4j", "log4j-over-slf4j"),
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

  // Force a version of jackson-databind that addresses this vulnerability:
  // https://app.snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-469674
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.10.1",
)

// Enables the @JsonCodec - https://circe.github.io/circe/
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

assemblyJarName := "main.jar"

// Fixes the clashes caused by:
// - ch.qos.logback/logback-classic/jars/logback-classic-1.3.0-alpha4.jar:module-info.class
// - ch.qos.logback/logback-core/jars/logback-core-1.3.0-alpha4.jar:module-info.class
// - org.slf4j/slf4j-api/jars/slf4j-api-1.8.0-beta1.jar:module-info.class
// Uses the advice in the stack overflow answer by Elesion Olalekan Fuad and the comment by note:
// https://stackoverflow.com/questions/25144484/sbt-assembly-deduplication-found-error
assembly / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

enablePlugins(RiffRaffArtifact)
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "payment-failure-cfn/cloud-formation.yaml")
