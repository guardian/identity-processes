package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, Pending, SarInitiateRequest, SarInitiateResponse, SarRequest, SarResponse, SarStatusRequest, SarStatusResponse}
import org.scalatest.{FreeSpec, Matchers}
import io.circe.parser._
import io.circe.syntax._
import circeCodecs._
import io.circe.Printer

class CirceCodecsSpec extends FreeSpec with Matchers {
  val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  "BatonModels" - {
    "should decode a valid initiate request" in {
      val expectedRequest = SarInitiateRequest(subjectEmail = "testSubjectEmail", dataProvider = "formstack")
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

    "should encode SarInitiateResponse correctly" in {
      val response: SarResponse = SarInitiateResponse("someRequestId")
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","action":"initiate","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should decode a valid status request" in {
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

    "should encode completed SarStatusResponse correctly" in {
      val response: SarResponse = SarStatusResponse(
        status = Completed,
        resultLocations = Some(List("resultLocation"))
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"completed","resultLocations":["resultLocation"],"action":"status","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode failed SarStatusResponse correctly" in {
      val response: SarResponse = SarStatusResponse(
        status = Failed,
        resultLocations = None,
        message = Some("error making request")
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"failed","message":"error making request","action":"status","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode pending SarStatusResponse correctly" in {
      val response: SarResponse = SarStatusResponse(
        status = Pending
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"pending","action":"status","requestType":"SAR","dataProvider":"formstack"}"""
    }

  }
}
