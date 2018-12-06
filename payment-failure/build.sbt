name := "payment-failure-lambda"

organization := "com.gu"

scalaVersion := "2.12.6"

assemblyJarName := "main.jar"

enablePlugins(RiffRaffArtifact)
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloud-formation.yaml") -> "payment-failure-cfn/cloud-formation.yaml")
