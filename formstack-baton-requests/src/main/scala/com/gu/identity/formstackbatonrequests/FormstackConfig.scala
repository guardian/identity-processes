package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.aws.ParameterStoreClient

case class InitLambdaConfig (
  resultsBucket: String,
  resultsPath: String,
  stateMachineArn: String
)

case class FormstackAccountToken(account: Int, secret: String)

case class PerformLambdaConfig (
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

  private def secureStringFromStore(pathEnvVariable: String): Option[String] = {
    getEnvironmentVariable(pathEnvVariable).flatMap(ParameterStoreClient.readSecureString)
  }

  def getInitHandlerConfig: InitLambdaConfig =
    (for {
      resultsBucket <- getEnvironmentVariable("RESULTS_BUCKET")
      resultsPath <- getEnvironmentVariable("RESULTS_PATH")
      stateMachineArn <- getEnvironmentVariable("STATE_MACHINE_ARN")
    } yield InitLambdaConfig(resultsBucket, resultsPath, stateMachineArn))
      .getOrElse {
        throw new RuntimeException(
          s"Unable to retrieve environment variables for Formstack Init Handler")
      }

  def getPerformHandlerConfig: PerformLambdaConfig =
    (for {
      resultsBucket <- getEnvironmentVariable("RESULTS_BUCKET")
      resultsPath <- getEnvironmentVariable("RESULTS_PATH")
      encryptionPassword <- secureStringFromStore("ENCRYPTION_PASSWORD_PATH")
      accountOneToken <- secureStringFromStore("FORMSTACK_ACCOUNT_ONE_TOKEN_PATH")
      accountTwoToken <- secureStringFromStore("FORMSTACK_ACCOUNT_TWO_TOKEN_PATH")
      bcryptSalt <- secureStringFromStore("BCRYPT_SALT_PATH")
      submissionsTableName <- getEnvironmentVariable("SUBMISSION_TABLE_NAME")
      lastUpdatedTableName <- getEnvironmentVariable("LAST_UPDATED_TABLE_NAME")
    } yield PerformLambdaConfig(resultsBucket, resultsPath, encryptionPassword, FormstackAccountToken(1, accountOneToken), FormstackAccountToken(2, accountTwoToken), bcryptSalt, submissionsTableName, lastUpdatedTableName))
      .getOrElse {
        throw new RuntimeException(
          s"Unable to retrieve environment variables for Formstack Perform SAR Handler"
        )
      }

}
