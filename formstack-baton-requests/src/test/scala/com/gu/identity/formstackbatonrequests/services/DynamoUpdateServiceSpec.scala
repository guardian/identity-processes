package com.gu.identity.formstackbatonrequests.services

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.aws.{DynamoClientStub, SubmissionTableUpdateDate}
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}

class DynamoUpdateServiceSpec
  extends FreeSpec
    with Matchers
    with MockFactory {

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

  "DynamoUpdateService" - {

    "should detect fields with email addresses and return a list of SubmissionIdEmail" in {
      FormstackServiceStub.formSubmissionsForGivenPageSuccess.map { submissionsOrError =>
        val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
          formstackClient = FormstackServiceStub.withSuccessResponse,
          dynamoClient = DynamoClientStub.withSuccessResponse,
          config = mockConfig
        )

        val submissionsWithEmail = dynamoUpdateService.submissionsWithEmailAndAccount(submissionsOrError.submissions, accountNumber = 1)
        submissionsWithEmail.length shouldBe 2
      }
    }

    val millisLongerThan30s = 300001
    val millisLessThan30s = 290000

    val dummyFormsPage = 1
    val dummyCount = 0
    val dummyToken = FormstackAccountToken(
      account = 0,
      secret = "baz"
    )
    val dummySubmissionsTableUpdateDate = SubmissionTableUpdateDate(
      formstackSubmissionTableMetadata = "foo",
      date = "bar"
    )

    "successfully calls updateSubmissionsTable with > 30s of lambda runtime available" in {
      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = FormstackServiceStub.withSuccessResponse,
        dynamoClient = DynamoClientStub.withSuccessResponse,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLongerThan30s)

      val expectedUpdateStatus = UpdateStatus(
        completed = true,
        formsPage = None,
        count = None,
        token = dummyToken
      )

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        lastUpdate = dummySubmissionsTableUpdateDate,
        count = dummyCount,
        token = dummyToken,
        context = mockContext
      )

      statusUpdate shouldBe Right(expectedUpdateStatus)
    }

    "successfully calls updateSubmissionsTable with < 30s of lambda runtime available" in {
      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = FormstackServiceStub.withSuccessResponse,
        dynamoClient = DynamoClientStub.withSuccessResponse,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLessThan30s)

      val expectedUpdateStatus = UpdateStatus(
        completed = false,
        formsPage = Some(dummyFormsPage + 1),
        count = Some(FormstackService.formResultsPerPage),
        token = dummyToken
      )

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        lastUpdate = dummySubmissionsTableUpdateDate,
        count = dummyCount,
        token = dummyToken,
        context = mockContext
      )

      statusUpdate shouldBe Right(expectedUpdateStatus)
    }
  }
}
