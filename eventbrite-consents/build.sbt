
name := "eventbrite-consents-lambda"

version := "0.1"

scalaVersion := "2.12.18"

val log4jVersion = "2.22.1"
val circeVersion = "0.11.2"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.9",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  ("org.jlib" % "jlib-awslambda-logback" % "1.0.0").exclude("org.slf4j", "log4j-over-slf4j"),
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" %log4jVersion,
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalactic" %% "scalactic" % "3.0.9",
  "org.scalatest" %% "scalatest" % "3.0.9" % "test"
)

scalacOptions += "-Ypartial-unification"

assemblyJarName := "main.jar"

assembly / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
