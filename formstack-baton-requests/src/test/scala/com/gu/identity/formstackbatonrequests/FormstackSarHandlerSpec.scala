package com.gu.identity.formstackbatonrequests

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.scalatest.{FreeSpec, Matchers}
import circeCodecs._
import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, Pending, SarInitiateRequest, SarInitiateResponse, SarStatusRequest, SarStatusResponse}

class FormstackSarHandlerSpec extends FreeSpec with Matchers {
  val mockConfig: SarLambdaConfig = SarLambdaConfig("resultsBucket", "resultsPath", "performSarFunctionName")
  val validInitiateRequest: SarInitiateRequest = SarInitiateRequest("subjectEmail", "formstack")
  val validStatusRequest: SarStatusRequest = SarStatusRequest("initiationReference")

  "FormstackSarHandler" - {
    "should return stringified circe ParsingFailure if called initiate with invalid json" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val invalidRequest = "invalidJson"
      val testInputStream = new ByteArrayInputStream(invalidRequest.getBytes)
      val testOutputStream = new ByteArrayOutputStream()
      lambda.handleRequest(testInputStream, testOutputStream)
      val response = new String(testOutputStream.toByteArray)
      response.contains("io.circe.ParsingFailure") shouldBe true
    }

    "should returning a decoding failure in a dataprovider other than formstack is passed" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
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
      lambda.handleRequest(testInputStream, testOutputStream)
      val response = new String(testOutputStream.toByteArray)
      response.contains("DecodingFailure(invalid dataProvider: zuora, List())") shouldBe true
    }

    "should return a SarInitiateResponse after invoking the PerformSarLambda" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      lambda
        .handle(validInitiateRequest)
        .map(response => response.isInstanceOf[SarInitiateResponse] shouldBe true )
    }

    "should return completed status upon successful completion" in {
      val lambda = FormstackSarHandler(S3ClientStub.withSuccessResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = SarStatusResponse(status = Completed, resultLocations = Some(List("s3Location")))
      lambda
        .handle(validStatusRequest)
        .map(response => response shouldBe expectedResponse)
    }

    "should return failed status upon unsuccessful completion" in {
      val lambda = FormstackSarHandler(S3ClientStub.withFailedResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = SarStatusResponse(status = Failed)
      lambda
        .handle(validStatusRequest)
        .map(response => response shouldBe expectedResponse)
    }

    "should return pending status when found to be neither success nor failure" in {
      val lambda = FormstackSarHandler(S3ClientStub.withPendingResponse, LambdaClientStub.withSuccessResponse, mockConfig)
      val expectedResponse = SarStatusResponse(status = Pending)
      lambda
        .handle(validStatusRequest)
        .map(response => response shouldBe expectedResponse)
    }
  }

}
