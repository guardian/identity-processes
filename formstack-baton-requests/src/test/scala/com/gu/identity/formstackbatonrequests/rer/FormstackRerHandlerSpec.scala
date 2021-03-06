package com.gu.identity.formstackbatonrequests.rer

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.circeCodecs._
import com.gu.identity.formstackbatonrequests.InitLambdaConfig
import com.gu.identity.formstackbatonrequests.aws.{StepFunctionClientStub, S3ClientStub}
import org.scalatest.{FreeSpec, Matchers}

class FormstackRerHandlerSpec extends FreeSpec with Matchers {
  val mockConfig: InitLambdaConfig = InitLambdaConfig("resultsBucket", "resultsPath", "performSarFunctionName")
  val validInitiateRequest: RerInitiateRequest = RerInitiateRequest("subjectEmail", "formstack", RER)
  val validStatusRequest: RerStatusRequest = RerStatusRequest("initiationReference")

  "FormstackRerHandler" - {
    "should return stringified circe ParsingFailure if called initiate with invalid json" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val invalidRequest = "invalidJson"
      val testInputStream = new ByteArrayInputStream(invalidRequest.getBytes)
      val testOutputStream = new ByteArrayOutputStream()
      lambda.handleRequest(testInputStream, testOutputStream, null)
      val response = new String(testOutputStream.toByteArray)
      response.contains("io.circe.ParsingFailure") shouldBe true
    }

    "should returning a decoding failure if a dataprovider other than formstack is passed" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
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
      lambda.handleRequest(testInputStream, testOutputStream, null)
      val response = new String(testOutputStream.toByteArray)
      response.contains("DecodingFailure(invalid dataProvider: zuora, List())") shouldBe true
    }

    "should returning a decoding failure if a requestType other than RER is passed" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
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
      lambda.handleRequest(testInputStream, testOutputStream, null)
      val response = new String(testOutputStream.toByteArray)
      response.contains("DecodingFailure(excepted request type not found: SAR, List())") shouldBe true
    }

    "should return a RerInitiateResponse after invoking the PerformRerLambda" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      lambda
        .handle(validInitiateRequest, null)
        .map(response => response.isInstanceOf[RerInitiateResponse] shouldBe true )
    }

    "should return completed status upon successful completion" in {
      val lambda = FormstackRerHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = RerStatusResponse("initiationReference", status = Completed, "RER completed: completed RER results for initiation reference initiationReference found in s3: List(s3Location)")
      lambda
        .handle(validStatusRequest, null)
        .map(response => response shouldBe expectedResponse)
    }

    "should return failed status upon unsuccessful completion" in {
      val lambda = FormstackRerHandler(S3ClientStub.withFailedResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = RerStatusResponse("initiationReference", status = Failed, "RER failed: failed path found in S3 for initiation reference initiationReference. Please check FormstackPerformRerLambda logs")
      lambda
        .handle(validStatusRequest, null)
        .map(response => response shouldBe expectedResponse)
    }

    "should return pending status when found to be neither success nor failure" in {
      val lambda = FormstackRerHandler(S3ClientStub.withPendingStatusResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = RerStatusResponse("initiationReference", status = Pending, "RER pending: no results found in S3 for initiation reference initiationReference.")
      lambda
        .handle(validStatusRequest, null)
        .map(response => response shouldBe expectedResponse)
    }
  }
}
