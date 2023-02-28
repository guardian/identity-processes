name := "formstack-consents-lambda"

version := "0.1"

scalaVersion := "2.12.8"
val circeVersion = "0.11.0"
val log4jVersion = "2.20.0"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.2",
  "org.typelevel" %% "cats-core" % "2.0.0-M1",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "com.typesafe" % "config" % "1.3.3",
  "joda-time" % "joda-time" % "2.12.2",
  "org.joda" % "joda-convert" % "1.6",
  ("org.jlib" % "jlib-awslambda-logback" % "1.0.0").exclude("org.slf4j", "log4j-over-slf4j"),
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

scalacOptions += "-Ypartial-unification"

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

assemblyJarName := "main.jar"

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
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "formstack-consents-cfn/cloud-formation.yaml")
