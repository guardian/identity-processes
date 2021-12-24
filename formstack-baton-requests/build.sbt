import sys.process._

name := "formstack-baton-requests"

version := "0.1"

scalaVersion := "2.12.8"
val circeVersion = "0.13.0"
val amazonSdkVersion = "1.11.755"
val log4jVersion = "2.16.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.gu" %% "scanamo" % "1.0.0-M6" excludeAll ExclusionRule(organization = "com.amazonaws"), //brings obsolete aws version 1.11.256
  "com.amazonaws" % "aws-java-sdk-dynamodb" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-stepfunctions" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-ssm" % amazonSdkVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  ("org.jlib" % "jlib-awslambda-logback" % "1.0.0").exclude("org.slf4j", "log4j-over-slf4j"),
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.1"
)

scalacOptions += "-Ypartial-unification"

assemblyJarName := "main.jar"

assembly / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

enablePlugins(RiffRaffArtifact)
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "formstack-baton-requests-cfn/cloud-formation.yaml")

Test / testOptions += Tests.Setup { () =>
  "./localenv/start-dependencies.sh".!
}

Test / testOptions += Tests.Cleanup { () =>
  "./localenv/stop-dependencies.sh".!
}

Compile / run / fork := true
Compile / mainClass := Some("com.gu.identity.formstackbatonrequests.LocalRun")
Compile / run / mainClass := Some("com.gu.identity.formstackbatonrequests.LocalRun")
run / javaOptions  += "-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y"
run / envVars := Map(
  "BCRYPT_SALT_PATH" ->	"/identity/formstack-baton-requests/bcrypt-salt",
  "ENCRYPTION_PASSWORD_PATH" ->	"/identity/formstack-baton-requests/encryption-password",
  "FORMSTACK_ACCOUNT_ONE_TOKEN_PATH" ->	"/identity/formstack-baton-requests/formstack-account-one-token",
  "FORMSTACK_ACCOUNT_TWO_TOKEN_PATH" ->	"/identity/formstack-baton-requests/formstack-account-two-token",
  "LAST_UPDATED_TABLE_NAME" ->	"formstack-submissions-last-updated",
  "RESULTS_BUCKET" ->	"gu-baton-results",
  "RESULTS_PATH" ->	"formstack-results/PROD",
  "STAGE" ->	"PROD",
  "SUBMISSION_TABLE_NAME" ->	"formstack-submission-ids",
)
