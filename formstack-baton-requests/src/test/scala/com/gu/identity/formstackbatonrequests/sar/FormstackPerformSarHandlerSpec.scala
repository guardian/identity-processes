package com.gu.identity.formstackbatonrequests.sar

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, SarPerformRequest, SarPerformResponse}
import com.gu.identity.formstackbatonrequests._
import com.gu.identity.formstackbatonrequests.aws.{DynamoClientStub, S3ClientStub}
import com.gu.identity.formstackbatonrequests.services.FormstackServiceStub
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

  val validSarPerformRequest: SarPerformRequest = SarPerformRequest(
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
      subjectEmail = "someSubjectEmail"
    )

    lambda
      .handle(validSarPerformRequest, null).map(res => res shouldBe expectedResponse)
  }

  "should return an error when request is successful but upload to S3 is unsuccessful" in {

    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackServiceStub.withSuccessResponse,
        S3ClientStub.withFailedResponse,
        mockConfig)

    val response = lambda.handle(validSarPerformRequest, null)

    response.isLeft shouldBe true
    response.left.map(err => err.getMessage shouldBe "S3 error")
  }

  "should return an error when Formstack API throws an error" in {

    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackServiceStub.withFailedResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val response = lambda.handle(validSarPerformRequest, null)

    response.isLeft shouldBe true
    response.left.map(err => err.getMessage shouldBe "Formstack API error")
  }

  "should return an error when DynamoDB throws an error" in {

    val lambda =
      FormstackPerformSarHandler(
        DynamoClientStub.withFailedResponse,
        FormstackServiceStub.withSuccessResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val response = lambda.handle(validSarPerformRequest, null)

    response.isLeft shouldBe true
    response.left.map(err => err.getMessage shouldBe "DynamoDB error")
  }
}
