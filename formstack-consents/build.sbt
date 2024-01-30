name := "formstack-consents-lambda"

version := "0.1"

scalaVersion := "2.12.18"
val circeVersion = "0.11.2"
val log4jVersion = "2.22.1"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.9",
  "org.typelevel" %% "cats-core" % "2.0.0-M1",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "com.typesafe" % "config" % "1.3.4",
  "joda-time" % "joda-time" % "2.12.6",
  "org.joda" % "joda-convert" % "1.9.2",
  ("org.jlib" % "jlib-awslambda-logback" % "1.0.0").exclude("org.slf4j", "log4j-over-slf4j"),
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalactic" %% "scalactic" % "3.0.9",
  "org.scalatest" %% "scalatest" % "3.0.9" % "test"
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
