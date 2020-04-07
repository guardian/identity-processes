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
      resultsBucket <- getEnvironmentVariable("RESULTS_BUCKET")
      resultsPath <- getEnvironmentVariable("RESULTS_PATH")
      performSarFunctionName <- getEnvironmentVariable("PERFORM_SAR_FUNCTION_NAME")
    } yield SarLambdaConfig(resultsBucket, resultsPath, performSarFunctionName))
      .getOrElse {
        throw new RuntimeException(
          s"Unable to retrieve environment variables for Formstack SAR Handler")
      }
  }
}
