package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels._
import org.scalatest.{FreeSpec, Matchers}
import io.circe.parser._
import io.circe.syntax._
import circeCodecs._
import io.circe.Printer

class CirceCodecsSpec extends FreeSpec with Matchers {
  val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  "BatonModels" - {
    "should decode a valid SAR initiate request" in {
      val expectedRequest = SarInitiateRequest(subjectEmail = "testSubjectEmail", dataProvider = "formstack", requestType = SAR)
      val jsonRequest =
        """{
          |"subjectId": "",
          |"subjectEmail" : "testSubjectEmail",
          |"dataProvider" : "formstack",
          |"requestType": "SAR",
          |"action" : "initiate"
          |}
          |""".stripMargin

      decode[SarRequest](jsonRequest) shouldBe Right(expectedRequest)
    }

    "should decode a valid RER initiate request" in {
      val expectedRequest = RerInitiateRequest(subjectEmail = "testSubjectEmail", dataProvider = "formstack", requestType = RER)
      val jsonRequest =
        """{
          |"subjectId": "",
          |"subjectEmail" : "testSubjectEmail",
          |"dataProvider" : "formstack",
          |"requestType": "RER",
          |"action" : "initiate"
          |}
          |""".stripMargin

      decode[RerRequest](jsonRequest) shouldBe Right(expectedRequest)
    }

    "should encode SarInitiateResponse correctly" in {
      val response: SarResponse = SarInitiateResponse("someRequestId")
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","action":"initiate","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode RerInitiateResponse correctly" in {
      val response: RerResponse = RerInitiateResponse("someRequestId")
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","action":"initiate","requestType":"RER","dataProvider":"formstack"}"""
    }

    "should decode a valid SAR status request" in {
      val expectedRequest = SarStatusRequest(initiationReference = "someRequestId")
      val jsonRequest =
        """{
          |"initiationReference": "someRequestId",
          |"requestType": "SAR",
          |"action" : "status"
          |}
          |""".stripMargin

      decode[SarRequest](jsonRequest) shouldBe Right(expectedRequest)
    }

    "should decode a valid RER status request" in {
      val expectedRequest = RerStatusRequest(initiationReference = "someRequestId")
      val jsonRequest =
        """{
          |"initiationReference": "someRequestId",
          |"requestType": "RER",
          |"action" : "status"
          |}
          |""".stripMargin

      decode[RerRequest](jsonRequest) shouldBe Right(expectedRequest)
    }

    "should encode completed SarStatusResponse correctly" in {
      val response: SarResponse = SarStatusResponse(
        status = Completed,
        resultLocations = Some(List("resultLocation"))
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"completed","resultLocations":["resultLocation"],"action":"status","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode completed RerStatusResponse correctly" in {
      val response: RerResponse = RerStatusResponse(
        initiationReference = "someRequestId",
        status = Completed,
        message = None
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","status":"completed","action":"status","requestType":"RER","dataProvider":"formstack"}"""
    }

    "should encode failed SarStatusResponse correctly" in {
      val response: SarResponse = SarStatusResponse(
        status = Failed,
        resultLocations = None,
        message = Some("error making request")
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"failed","message":"error making request","action":"status","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode failed RerStatusResponse correctly" in {
      val response: RerResponse = RerStatusResponse(
        initiationReference = "someRequestId",
        status = Failed,
        message = Some("error making request")
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","status":"failed","message":"error making request","action":"status","requestType":"RER","dataProvider":"formstack"}"""
    }

    "should encode pending SarStatusResponse correctly" in {
      val response: SarResponse = SarStatusResponse(
        status = Pending
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"pending","action":"status","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode pending RerStatusResponse correctly" in {
      val response: RerResponse = RerStatusResponse(
        initiationReference = "someRequestId",
        status = Pending,
        message = None
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","status":"pending","action":"status","requestType":"RER","dataProvider":"formstack"}"""
    }

    "should decode a valid SAR perform request" in {
      val expectedRequest = SarPerformRequest("someRequestId", "testSubjectEmail", "formstack")

      val jsonRequest =
        """{
          |"initiationReference": "someRequestId",
          |"subjectEmail": "testSubjectEmail",
          |"dataProvider" : "formstack",
          |"requestType" : "SAR",
          |"action" : "perform"
          |}
          |""".stripMargin

      decode[SarRequest](jsonRequest) shouldBe Right(expectedRequest)
    }

    "should decode a valid RER perform request" in {
      val expectedRequest = RerPerformRequest("someRequestId", "testSubjectEmail", "formstack")

      val jsonRequest =
        """{
          |"initiationReference": "someRequestId",
          |"subjectEmail": "testSubjectEmail",
          |"dataProvider" : "formstack",
          |"requestType" : "RER",
          |"action" : "perform"
          |}
          |""".stripMargin

      decode[RerRequest](jsonRequest) shouldBe Right(expectedRequest)
    }

    "should encode completed SarPerformResponse correctly" in {
      val response: SarResponse = SarPerformResponse(
        status = Completed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        message = None
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"completed","initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","action":"perform","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode completed RerPerformResponse correctly" in {
      val response: RerResponse = RerPerformResponse(
        status = Completed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        message = None
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","status":"completed","action":"perform","requestType":"RER","dataProvider":"formstack"}"""
    }

    "should encode failed SarPerformResponse correctly" in {
      val response: SarResponse = SarPerformResponse(
        status = Failed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        message = Some("Error writing to S3")
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"failed","initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","message":"Error writing to S3","action":"perform","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode failed RerPerformResponse correctly" in {
      val response: RerResponse = RerPerformResponse(
        status = Failed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        message = Some("Error writing to S3")
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","status":"failed","message":"Error writing to S3","action":"perform","requestType":"RER","dataProvider":"formstack"}"""
    }

  }
}
