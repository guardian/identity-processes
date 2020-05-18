package com.gu.identity.formstackbatonrequests.services

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.aws.{DynamoClientStub, SubmissionTableUpdateDate}
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import org.scalatest.{FreeSpec, Matchers}

class DynamoUpdateServiceSpec extends FreeSpec with Matchers {
  val mockConfig: PerformLambdaConfig =
    PerformLambdaConfig(
      "resultsBucket",
      "resultsPath",
      "encryptionPassword",
      FormstackAccountToken(1, "accountOneToken"),
      FormstackAccountToken(2, "accountTwoToken"),
      "bcryptSalt",
      "submissions-table-name",
      "last-updated-table-name"
    )

  val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(FormstackServiceStub.withSuccessResponse, DynamoClientStub.withSuccessResponse, mockConfig)
  "DynamoUpdateService" - {

    "should detect fields with email addresses and return a list of SubmissionIdEmail" in {

      FormstackServiceStub.formSubmissionsForGivenPageSuccess.map { submissionsOrError =>
        val submissionsWithEmail = dynamoUpdateService.submissionsWithEmailAndAccount(submissionsOrError.submissions, accountNumber = 1)
        submissionsWithEmail.length shouldBe 2
      }
    }
  }
}
