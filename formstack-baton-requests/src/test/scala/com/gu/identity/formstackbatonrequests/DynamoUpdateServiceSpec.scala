package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
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

  val dynamoUpdateService = DynamoUpdateService(FormstackServiceStub.withSuccessResponse, DynamoClientStub.withSuccessResponse, mockConfig)
  "DynamoUpdateService" - {
    "should update dynamo with submissions from Formstack" in {

      val expectedResponse = Right(())

      dynamoUpdateService
        .updateDynamo(SubmissionTableUpdateDate("lastUpdated", "2020-01-01 00:00:00")) shouldBe expectedResponse
    }

    "should detect fields with email addresses and return a list of SubmissionIdEmail" in {

      FormstackServiceStub.formSubmissionsForGivenPageSuccess.map { submissionsOrError =>
        val submissionsWithEmail = dynamoUpdateService.submissionsWithEmailAndAccount(submissionsOrError.submissions, accountNumber = 1)
        submissionsWithEmail.length shouldBe 2
      }
    }
  }
}
