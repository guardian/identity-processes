package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, SarPerformRequest, SarPerformResponse}
import org.scalatest.{FreeSpec, Matchers}

class FormstackPerformSarHandlerSpec extends FreeSpec with Matchers {
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

  val validSarPerformRequest = SarPerformRequest(
    initiationReference = "someRequestId",
    subjectEmail = "someSubjectEmail",
    dataProvider = "formstack"
  )

  "should return a successful SarPerformResponse when a SAR runs successfully and writes to S3" in {

    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackServiceStub.withSuccessResponse,
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
        FormstackServiceStub.withSuccessResponse,
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
        FormstackServiceStub.withFailedResponse,
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
        FormstackServiceStub.withSuccessResponse,
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
}
