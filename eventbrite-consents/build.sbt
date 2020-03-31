
name := "eventbrite-consents-lambda"

version := "0.1"

scalaVersion := "2.12.8"
val circeVersion = "0.11.0"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.2",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "org.jlib" % "jlib-awslambda-logback" % "1.0.0",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalactic" %% "scalactic" % "3.0.5",
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
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "eventbrite-consents-cfn/cloud-formation.yaml")