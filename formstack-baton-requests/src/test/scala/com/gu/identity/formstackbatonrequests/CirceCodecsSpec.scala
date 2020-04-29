package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, Pending, SarInitiateRequest, SarInitiateResponse, SarPerformRequest, SarPerformResponse, SarRequest, SarResponse, SarStatusRequest, SarStatusResponse}
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

    "should decode a valid perform request" in {
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

    "should encode completed SarPerformResponse correctly" in {
      val response: SarResponse = SarPerformResponse(
        status = Completed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        message = None
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"completed","initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","action":"perform","requestType":"SAR","dataProvider":"formstack"}"""
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

    "should decode valid FormsSubmissions with data" in {
      val jsonResponse =
        """{
          |"submissions": [
          |{
          |"id": "123456",
          |"data": {"1111": {"field": "22", "value": "test", "flat_value": "test", "label": "Name"}, "1112": {"field": "23", "value": "email@test.com", "label": "Email"}}
          |},
          |{
          |"id": "654321",
          |"data": {"2222": {"field": "33", "value": "test2", "flat_value": "test2", "label": "Name2"}, "2223": {"field": "34", "value": "email@test2.com", "label": "Email"}}
          |}],
          |"pages": 2
          |}
          |""".stripMargin

      decode[FormSubmissions](jsonResponse) shouldBe
        Right(FormSubmissions(List(
          FormSubmission("123456",
            Map(
              "1111" -> ResponseValue("test".asJson),
              "1112" -> ResponseValue("email@test.com".asJson)
            )),
          FormSubmission("654321",
            Map(
              "2222" -> ResponseValue("test2".asJson),
              "2223" -> ResponseValue("email@test2.com".asJson)
            ))), 2))
    }

    "should decode valid FormsSubmissions without data" in {
      val jsonResponse =
        """{
          |"submissions": [
          |{
          |"id": "123456",
          |"data": []
          |}],
          |"pages": 2
          |}
          |""".stripMargin

      decode[FormSubmissions](jsonResponse) shouldBe
        Right(FormSubmissions(List(FormSubmission("123456", Map.empty)), 2))
    }

    "should fail to decode invalid FormsSubmissions" in {
      val jsonResponse =
        """{
          |"submissions": [
          |{
          |"noId": "true",
          |"data": []
          |}],
          |"pages": 2
          |}
          |""".stripMargin

      decode[FormSubmissions](jsonResponse).isLeft shouldBe true
    }
  }
}
