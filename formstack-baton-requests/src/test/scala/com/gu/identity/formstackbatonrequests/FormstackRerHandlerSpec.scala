package com.gu.identity.formstackbatonrequests

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.circeCodecs._
import org.scalatest.{FreeSpec, Matchers}

class FormstackRerHandlerSpec extends FreeSpec with Matchers {
  val mockConfig: InitLambdaConfig = InitLambdaConfig("resultsBucket", "resultsPath", "performSarFunctionName")
  val validInitiateRequest: RerInitiateRequest = RerInitiateRequest("subjectEmail", "formstack", RER)
  val validStatusRequest: RerStatusRequest = RerStatusRequest("initiationReference")

  "FormstackRerHandler" - {
    "should return stringified circe ParsingFailure if called initiate with invalid json" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val invalidRequest = "invalidJson"
      val testInputStream = new ByteArrayInputStream(invalidRequest.getBytes)
      val testOutputStream = new ByteArrayOutputStream()
      lambda.handleRequest(testInputStream, testOutputStream)
      val response = new String(testOutputStream.toByteArray)
      response.contains("io.circe.ParsingFailure") shouldBe true
    }

    "should returning a decoding failure if a dataprovider other than formstack is passed" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val invalidProviderRequest =
        """{
          |"subjectId": "",
          |"subjectEmail" : "testSubjectEmail",
          |"dataProvider" : "zuora",
          |"requestType": "RER",
          |"action" : "initiate"
          |}
          |""".stripMargin

      val testInputStream = new ByteArrayInputStream(invalidProviderRequest.getBytes)
      val testOutputStream = new ByteArrayOutputStream()
      lambda.handleRequest(testInputStream, testOutputStream)
      val response = new String(testOutputStream.toByteArray)
      response.contains("DecodingFailure(invalid dataProvider: zuora, List())") shouldBe true
    }

    "should returning a decoding failure if a requestType other than RER is passed" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val invalidProviderRequest =
        """{
          |"subjectId": "",
          |"subjectEmail" : "testSubjectEmail",
          |"dataProvider" : "formstack",
          |"requestType": "SAR",
          |"action" : "initiate"
          |}
          |""".stripMargin

      val testInputStream = new ByteArrayInputStream(invalidProviderRequest.getBytes)
      val testOutputStream = new ByteArrayOutputStream()
      lambda.handleRequest(testInputStream, testOutputStream)
      val response = new String(testOutputStream.toByteArray)
      response.contains("DecodingFailure(excepted request type not found: SAR, List())") shouldBe true
    }

    "should return a RerInitiateResponse after invoking the PerformRerLambda" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      lambda
        .handle(validInitiateRequest)
        .map(response => response.isInstanceOf[RerInitiateResponse] shouldBe true )
    }

    "should return completed status upon successful completion" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = RerStatusResponse("initiationReference", status = Completed, None)
      lambda
        .handle(validStatusRequest)
        .map(response => response shouldBe expectedResponse)
    }

    "should return failed status upon unsuccessful completion" in {
      val lambda = FormstackRerHandler(S3ClientStub.withFailedResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = RerStatusResponse("initiationReference", status = Failed, None)
      lambda
        .handle(validStatusRequest)
        .map(response => response shouldBe expectedResponse)
    }

    "should return pending status when found to be neither success nor failure" in {
      val lambda = FormstackRerHandler(S3ClientStub.withPendingStatusResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = RerStatusResponse("initiationReference", status = Pending, None)
      lambda
        .handle(validStatusRequest)
        .map(response => response shouldBe expectedResponse)
    }
  }
}
