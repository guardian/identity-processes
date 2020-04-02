
name := "formstack-baton-requests"

version := "0.1"

scalaVersion := "2.12.8"
val circeVersion = "0.13.0"
val amazonSdkVersion = "1.11.755"

libraryDependencies ++= Seq(
)

scalacOptions += "-Ypartial-unification"

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

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