package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, SarInitiateRequest, SarPerformRequest, SarPerformResponse}
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
      "bcryptSalt",
      "submissions-table-name",
      "last-updated-table-name"
    )

  val validSarPerformRequest = SarPerformRequest(
    initiationReference = "someRequestId",
    subjectEmail = "someSubjectEmail",
    dataProvider = "formstack"
  )

  "should return a successful SarPerformResponse when a SAR runs successfully and writes to S3" in {

    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackSarServiceStub.withSuccessResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val expectedResponse = SarPerformResponse(
      status = Completed,
      initiationReference = "someRequestId",
      subjectEmail = "someSubjectEmail",
      None
    )

    lambda
      .handle(validSarPerformRequest).map(res => res shouldBe expectedResponse)
  }

  "should return a failed SarPerformResponse when request is successful but upload to S3 is unsuccessful" in {

    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackSarServiceStub.withSuccessResponse,
        S3ClientStub.withFailedResponse,
        mockConfig)

    val expectedResponse = SarPerformResponse(
      status = Failed,
      initiationReference = "someRequestId",
      subjectEmail = "someSubjectEmail",
      message = Some("S3 error")
    )

    lambda
      .handle(validSarPerformRequest) shouldBe Right(expectedResponse)
  }

  "should return a failed SarPerformResponse when Formstack API throws an error" in {

    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackSarServiceStub.withFailedResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val expectedResponse = SarPerformResponse(
      status = Failed,
      initiationReference = "someRequestId",
      subjectEmail = "someSubjectEmail",
      message = Some("Formstack API error")
    )

    lambda
      .handle(validSarPerformRequest) shouldBe Right(expectedResponse)
  }

  "should return a failed SarPerformResponse when DynamoDB throws an error" in {

    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withFailedResponse,
        FormstackSarServiceStub.withSuccessResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val expectedResponse = SarPerformResponse(
      status = Failed,
      initiationReference = "someRequestId",
      subjectEmail = "someSubjectEmail",
      message = Some("DynamoDB error")
    )

    lambda
      .handle(validSarPerformRequest) shouldBe Right(expectedResponse)
  }

  "should update dynamo with submissions from Formstack" in {
    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackSarServiceStub.withSuccessResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val expectedResponse = Right(())

    lambda
      .updateDynamo(SubmissionTableUpdateDate("lastUpdated", "2020-01-01 00:00:00")) shouldBe expectedResponse
  }

  "should detect fields with email addresses and return a list of SubmissionIdEmail" in {
    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackSarServiceStub.withSuccessResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    FormstackSarServiceStub.formSubmissionsForGivenPageSuccess.map { submissionsOrError =>
      val submissionsWithEmail = lambda.submissionsWithEmailAndAccount(submissionsOrError.submissions, accountNumber = 1)
      submissionsWithEmail.length shouldBe 2
    }
  }

}
