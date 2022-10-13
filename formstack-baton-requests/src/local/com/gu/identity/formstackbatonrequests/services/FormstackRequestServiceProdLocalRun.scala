package com.gu.identity.formstackbatonrequests.services

import com.gu.identity.formstackbatonrequests.aws.ParameterStoreClient
import com.gu.identity.formstackbatonrequests.sar.SubmissionIdEmail
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}

//Just a a manual trigger to run the FormstackRequestService code for fetching submissionData in PROD. 
object FormstackRequestServiceProdLocalRun extends App {

  val config: PerformLambdaConfig =
    (for {
      encryptionPassword <- ParameterStoreClient.readSecureString("/identity/formstack-baton-requests/encryption-password")
      accountOneToken <- ParameterStoreClient.readSecureString("/identity/formstack-baton-requests/formstack-account-one-token")
      accountTwoToken <- ParameterStoreClient.readSecureString("/identity/formstack-baton-requests/formstack-account-two-token")
      bcryptSalt <- ParameterStoreClient.readSecureString("/identity/formstack-baton-requests/bcrypt-salt")
    } yield PerformLambdaConfig(
      resultsBucket = "not used",
      resultsPath = "not used",
      encryptionPassword = encryptionPassword,
      accountOneToken = FormstackAccountToken(1, accountOneToken),
      accountTwoToken = FormstackAccountToken(2, accountTwoToken),
      bcryptSalt = bcryptSalt,
      submissionTableName = "not used",
      lastUpdatedTableName = "not used")).get

  val invalidSubmission = SubmissionIdEmail(
    email = "",
    submissionId = "1234",
    receivedByLambdaTimestamp = 0,
    accountNumber = 1
  )
  
  val submissionIdEmails = List(invalidSubmission)
  val response = FormstackService().submissionData(submissionIdEmails, config)
  println("response was:")
  println(response)

}
