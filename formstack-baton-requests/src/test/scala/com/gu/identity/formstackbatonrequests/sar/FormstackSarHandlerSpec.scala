package com.gu.identity.formstackbatonrequests.sar

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.circeCodecs._
import com.gu.identity.formstackbatonrequests.InitLambdaConfig
import com.gu.identity.formstackbatonrequests.aws.{StepFunctionClientStub, S3ClientStub}
import org.scalatest.{FreeSpec, Matchers}

class FormstackSarHandlerSpec extends FreeSpec with Matchers {
  val mockConfig: InitLambdaConfig = InitLambdaConfig("resultsBucket", "resultsPath", "performSarFunctionName")
  val validInitiateRequest: SarInitiateRequest = SarInitiateRequest("subjectEmail", "formstack", SAR)
  val validStatusRequest: SarStatusRequest = SarStatusRequest("initiationReference")

  "FormstackSarHandler" - {
    "should return stringified circe ParsingFailure if called initiate with invalid json" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val invalidRequest = "invalidJson"
      val testInputStream = new ByteArrayInputStream(invalidRequest.getBytes)
      val testOutputStream = new ByteArrayOutputStream()
      lambda.handleRequest(testInputStream, testOutputStream, null)
      val response = new String(testOutputStream.toByteArray)
      response.contains("io.circe.ParsingFailure") shouldBe true
    }

    "should returning a decoding failure if a dataprovider other than formstack is passed" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val invalidProviderRequest =
        """{
          |"subjectId": "",
          |"subjectEmail" : "testSubjectEmail",
          |"dataProvider" : "zuora",
          |"requestType": "SAR",
          |"action" : "initiate"
          |}
          |""".stripMargin

      val testInputStream = new ByteArrayInputStream(invalidProviderRequest.getBytes)
      val testOutputStream = new ByteArrayOutputStream()
      lambda.handleRequest(testInputStream, testOutputStream, null)
      val response = new String(testOutputStream.toByteArray)
      response.contains("DecodingFailure(invalid dataProvider: zuora, List())") shouldBe true
    }

    "should returning a decoding failure if a requestType other than SAR is passed" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val invalidProviderRequest =
        """{
          |"subjectId": "",
          |"subjectEmail" : "testSubjectEmail",
          |"dataProvider" : "formstack",
          |"requestType": "RER",
          |"action" : "initiate"
          |}
          |""".stripMargin

      val testInputStream = new ByteArrayInputStream(invalidProviderRequest.getBytes)
      val testOutputStream = new ByteArrayOutputStream()
      lambda.handleRequest(testInputStream, testOutputStream, null)
      val response = new String(testOutputStream.toByteArray)
      response.contains("DecodingFailure(excepted request type not found: RER, List())") shouldBe true
    }

    "should return a SarInitiateResponse after invoking the PerformSarLambda" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      lambda
        .handle(validInitiateRequest, null)
        .map(response => response.isInstanceOf[SarInitiateResponse] shouldBe true )
    }

    "should return completed status upon successful completion" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = SarStatusResponse(status = Completed, resultLocations = Some(List("s3Location")))
      lambda
        .handle(validStatusRequest, null)
        .map(response => response shouldBe expectedResponse)
    }

    "should return failed status upon unsuccessful completion" in {
      val lambda = FormstackSarHandler(S3ClientStub.withFailedResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = SarStatusResponse(status = Failed)
      lambda
        .handle(validStatusRequest, null)
        .map(response => response shouldBe expectedResponse)
    }

    "should return pending status when found to be neither success nor failure" in {
      val lambda = FormstackSarHandler(S3ClientStub.withPendingStatusResponse, StepFunctionClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = SarStatusResponse(status = Pending)
      lambda
        .handle(validStatusRequest, null)
        .map(response => response shouldBe expectedResponse)
    }
  }
}
