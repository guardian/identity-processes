package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import org.scalatest.{FreeSpec, Matchers}

class FormstackPerformSarHandlerSpec extends FreeSpec with Matchers {
  val mockConfig: PerformSarLambdaConfig =
    PerformSarLambdaConfig(
      "resultsBucket",
      "resultsPath",
      "encryptionPassword",
      FormstackAccountToken(1, "accountOneToken"),
      FormstackAccountToken(2, "accountTwoToken"),
      "bcryptSalt"
    )

  "FormstackSarHandler" - {
    "should update dynamo with submissions from Formstack" in {
      val lambda =
        FormstackPerformSarHandler(
          DynamoClientStub.withSuccessResponse,
          FormstackClientStub.withSuccessResponse,
          mockConfig)

      val expectedResponse = Right(())

      lambda
        .updateDynamo(SubmissionTableUpdateDate("lastUpdated", "2020-01-01 00:00:00")) shouldBe expectedResponse
    }

    "should detect fields with email addresses and return a list of SubmissionIdEmail" in {
      val lambda =
        FormstackPerformSarHandler(
          DynamoClientStub.withSuccessResponse,
          FormstackClientStub.withSuccessResponse,
          mockConfig)

      FormstackClientStub.formSubmissionsForGivenPageSuccess.map { submissionsOrError =>
        val submissionsWithEmail = lambda.submissionsWithEmailAndAccount(submissionsOrError.submissions, accountNumber = 1)
        submissionsWithEmail.length shouldBe 2
      }
    }
  }

}
