package com.gu.identity.formstackbatonrequests.rer

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, RerPerformRequest, RerPerformResponse}
import com.gu.identity.formstackbatonrequests._
import com.gu.identity.formstackbatonrequests.aws.{DynamoClientStub, S3ClientStub}
import com.gu.identity.formstackbatonrequests.services.stub.FormstackServiceStub
import org.scalatest.{FreeSpec, Matchers}

class FormstackPerformRerHandlerSpec extends FreeSpec with Matchers {
  val mockConfig: PerformLambdaConfig =
    PerformLambdaConfig(
      "resultsBucket",
      "resultsPath",
      "encryptionPassword",
      FormstackAccountToken(1, "accountOneToken"),
      "bcryptSalt",
      "submissions-table-name",
      "last-updated-table-name"
    )

  val validRerPerformRequest: RerPerformRequest = RerPerformRequest(
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
      status = Completed
    )

    lambda
      .handle(validRerPerformRequest, null).map(res => res shouldBe expectedResponse)
  }

  "should return an error when request is successful but upload of status object to S3 is unsuccessful" in {

    val lambda =
      FormstackPerformRerHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackServiceStub.withSuccessResponse,
        S3ClientStub.withFailedResponse,
        mockConfig)

    val response = lambda.handle(validRerPerformRequest, null)

    response.isLeft shouldBe true
    response.left.map(err => err.getMessage shouldBe "S3 error")
  }

  "should return an error when Formstack API throws an error" in {

    val lambda =
      FormstackPerformRerHandler(
        DynamoClientStub.withSuccessResponse,
        FormstackServiceStub.withFailedResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val response = lambda.handle(validRerPerformRequest, null)

    response.isLeft shouldBe true
    response.left.map(err => err.getMessage shouldBe "Formstack API error")
  }

  "should return a failed RerPerformResponse when DynamoDB throws an error" in {

    val lambda =
      FormstackPerformRerHandler(
        DynamoClientStub.withFailedResponse,
        FormstackServiceStub.withSuccessResponse,
        S3ClientStub.withSuccessResponse,
        mockConfig)

    val response = lambda.handle(validRerPerformRequest, null)

    response.isLeft shouldBe true
    response.left.map(err => err.getMessage shouldBe "DynamoDB error")
  }
}
