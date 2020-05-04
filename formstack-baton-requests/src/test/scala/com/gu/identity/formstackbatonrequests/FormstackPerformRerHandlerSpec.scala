package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, RerPerformRequest, RerPerformResponse, SarPerformRequest, SarPerformResponse}
import org.scalatest.{FreeSpec, Matchers}

class FormstackPerformRerHandlerSpec extends FreeSpec with Matchers {
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

  val validRerPerformRequest = RerPerformRequest(
    initiationReference = "someRequestId",
    subjectEmail = "someSubjectEmail",
    dataProvider = "formstack"
  )

  "should return a successful RerPerformResponse when a RER runs successfully and writes to S3" in {

    val lambda =
      FormstackPerformRerHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackServiceStub.withSuccessResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val expectedResponse = RerPerformResponse(
      initiationReference = "someRequestId",
      subjectEmail = "someSubjectEmail",
      status = Completed,
      None
    )

    lambda
      .handle(validRerPerformRequest).map(res => res shouldBe expectedResponse)
  }

  "should return a failed RerPerformResponse when request is successful but upload of status object to S3 is unsuccessful" in {

    val lambda =
      FormstackPerformRerHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackServiceStub.withSuccessResponse,
        S3ClientStub.withFailedResponse,
        mockConfig)

    val expectedResponse = RerPerformResponse(
      initiationReference = "someRequestId",
      subjectEmail = "someSubjectEmail",
      status = Failed,
      message = Some("S3 error")
    )

    lambda
      .handle(validRerPerformRequest) shouldBe Right(expectedResponse)
  }

  "should return a failed RerPerformResponse when Formstack API throws an error" in {

    val lambda =
      FormstackPerformRerHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackServiceStub.withFailedResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val expectedResponse = RerPerformResponse(
      initiationReference = "someRequestId",
      subjectEmail = "someSubjectEmail",
      status = Failed,
      message = Some("Formstack API error")
    )

    lambda
      .handle(validRerPerformRequest) shouldBe Right(expectedResponse)
  }

  "should return a failed RerPerformResponse when DynamoDB throws an error" in {

    val lambda =
      FormstackPerformRerHandler(
        DynamoClientStub.withFailedResponse,
        FormstackServiceStub.withSuccessResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val expectedResponse = RerPerformResponse(
      initiationReference = "someRequestId",
      subjectEmail = "someSubjectEmail",
      status = Failed,
      message = Some("DynamoDB error")
    )

    lambda
      .handle(validRerPerformRequest) shouldBe Right(expectedResponse)
  }
}
