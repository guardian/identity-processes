package com.gu.identity.formstackbatonrequests

case class SarLambdaConfig (
  resultsBucket: String,
  resultsPath: String,
  performSarFunctionName: String
)

object FormstackConfig {

  private def getEnvironmentVariable(env: String): Option[String] =
    Option(System.getenv(env))

  def getSarHandlerConfig: SarLambdaConfig = {
    (for {
      resultsBucket <- getEnvironmentVariable("resultsBucket")
      resultsPath <- getEnvironmentVariable("resultsPath")
      performSarFunctionName <- getEnvironmentVariable("performSarFunctionName")
    } yield SarLambdaConfig(resultsBucket, resultsPath, performSarFunctionName))
      .getOrElse {
        throw new RuntimeException(
          s"Unable to retrieve environment variables for Formstack SAR Handler")
      }
  }
}
