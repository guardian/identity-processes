name := "payment-failure-lambda"

organization := "com.gu"

scalaVersion := "2.12.6"
val circeVersion = "0.10.1"
val log4jVersion = "2.20.0"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.4.0",
  "com.amazonaws" % "aws-lambda-java-events" % "3.16.1",
  "software.amazon.awssdk" % "sqs" % "2.55.11",
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
  "ch.qos.logback" % "logback-classic" % "1.3.14",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

  // Force a version of jackson that addresses vulnerabilities
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.18.8",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.18.8",
)

// Enables the @JsonCodec - https://circe.github.io/circe/
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

assemblyJarName := "main.jar"

assembly / assemblyMergeStrategy := {
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
