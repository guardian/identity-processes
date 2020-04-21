package com.gu.identity.formstackbatonrequests

case class SarLambdaConfig (
  resultsBucket: String,
  resultsPath: String,
  performSarFunctionName: String
)

case class FormstackAccountToken(account: Int, secret: String)

case class PerformSarLambdaConfig (
  resultsBucket: String,
  resultsPath: String,
  encryptionPassword: String,
  accountOneToken: FormstackAccountToken,
  accountTwoToken: FormstackAccountToken,
  bcryptSalt: String,
  submissionTableName: String,
  lastUpdatedTableName: String
)

object FormstackConfig {

  private def getEnvironmentVariable(env: String): Option[String] =
    Option(System.getenv(env))

  def getSarHandlerConfig: SarLambdaConfig =
    (for {
      resultsBucket <- getEnvironmentVariable("RESULTS_BUCKET")
      resultsPath <- getEnvironmentVariable("RESULTS_PATH")
      performSarFunctionName <- getEnvironmentVariable("PERFORM_SAR_FUNCTION_NAME")
    } yield SarLambdaConfig(resultsBucket, resultsPath, performSarFunctionName))
      .getOrElse {
        throw new RuntimeException(
          s"Unable to retrieve environment variables for Formstack SAR Handler")
      }

  def getPerformSarHandlerConfig: PerformSarLambdaConfig =
    (for {
      resultsBucket <- getEnvironmentVariable("RESULTS_BUCKET")
      resultsPath <- getEnvironmentVariable("RESULTS_PATH")
      encryptionPassword <- getEnvironmentVariable("ENCRYPTION_PASSWORD")
      accountOneToken <- getEnvironmentVariable("FORMSTACK_ACCOUNT_ONE_TOKEN")
      accountTwoToken <- getEnvironmentVariable("FORMSTACK_ACCOUNT_TWO_TOKEN")
      bcryptSalt <- getEnvironmentVariable("BCRYPT_SALT")
      submissionsTableName <- getEnvironmentVariable("SUBMISSION_TABLE_NAME")
      lastUpdatedTableName <- getEnvironmentVariable("LAST_UPDATED_TABLE_NAME")
    } yield PerformSarLambdaConfig(resultsBucket, resultsPath, encryptionPassword, FormstackAccountToken(1, accountOneToken), FormstackAccountToken(2, accountTwoToken), bcryptSalt, submissionsTableName, lastUpdatedTableName))
      .getOrElse {
        throw new RuntimeException(
          s"Unable to retrieve environment variables for Formstack Perform SAR Handler"
        )
      }

}
