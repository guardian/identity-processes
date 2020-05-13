package com.gu.identity.formstackbatonrequests

import java.time.LocalDateTime

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
      val response: RerResponse = RerInitiateResponse("someRequestId", "PerformRerLambda invoked", Pending)
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","message":"PerformRerLambda invoked","status":"pending","action":"initiate","requestType":"RER","dataProvider":"formstack"}"""
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
        message = "completed RER"
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","status":"completed","message":"completed RER","action":"status","requestType":"RER","dataProvider":"formstack"}"""
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
        message = "error making request"
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
        message = "RER pending"
      )
      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","status":"pending","message":"RER pending","action":"status","requestType":"RER","dataProvider":"formstack"}"""
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
        subjectEmail = "testSubjectEmail"
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"completed","initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","action":"perform","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode completed RerPerformResponse correctly" in {
      val response: RerResponse = RerPerformResponse(
        status = Completed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail"
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","status":"completed","action":"perform","requestType":"RER","dataProvider":"formstack"}"""
    }

    "should encode failed SarPerformResponse correctly" in {
      val response: SarResponse = SarPerformResponse(
        status = Failed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail"
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"failed","initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","action":"perform","requestType":"SAR","dataProvider":"formstack"}"""
    }

    "should encode failed RerPerformResponse correctly" in {
      val response: RerResponse = RerPerformResponse(
        status = Failed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail"
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","status":"failed","action":"perform","requestType":"RER","dataProvider":"formstack"}"""
    }

    "should decode valid UpdateDynamoRequest with account number" in {
      val expectedResult: UpdateDynamoRequest = UpdateDynamoRequest(
        requestType = SAR,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        dataProvider = "formstack",
        accountNumber = Some(1),
        formPage = 1,
        count = 25,
        timeOfStart = LocalDateTime.of(2020, 2, 1, 0, 0)
      )

      val jsonRequest =
        """{
          |"requestType": "SAR",
          |"initiationReference": "someRequestId",
          |"subjectEmail": "testSubjectEmail",
          |"dataProvider": "formstack",
          |"accountNumber": 1,
          |"formPage": 1,
          |"count": 25,
          |"timeOfStart": "2020-02-01T00:00"
          |}
          |""".stripMargin

      decode[UpdateDynamoRequest](jsonRequest) shouldBe Right(expectedResult)
    }

    "should decode valid UpdateDynamoRequest without account number" in {
      val expectedResult: UpdateDynamoRequest = UpdateDynamoRequest(
        requestType = SAR,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        dataProvider = "formstack",
        accountNumber = None,
        formPage = 1,
        count = 25,
        timeOfStart = LocalDateTime.of(2020, 2, 1, 0, 0)
      )

      val jsonRequest =
        """{
          |"requestType": "SAR",
          |"initiationReference": "someRequestId",
          |"subjectEmail": "testSubjectEmail",
          |"dataProvider": "formstack",
          |"formPage": 1,
          |"count": 25,
          |"timeOfStart": "2020-02-01T00:00"
          |}
          |""".stripMargin

      decode[UpdateDynamoRequest](jsonRequest) shouldBe Right(expectedResult)
    }

    "should encode pending UpdateDynamoResponse" in {
      val response: UpdateDynamoResponse = UpdateDynamoResponse(
        status = Pending,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        dataProvider = "formstack",
        accountNumber = 1,
        formPage = Some(3),
        count = Some(75),
        requestType = SAR,
        timeOfStart = LocalDateTime.of(2020, 2, 1, 0, 0)
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"pending","initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","dataProvider":"formstack","accountNumber":1,"formPage":3,"count":75,"requestType":"SAR","timeOfStart":"2020-02-01T00:00:00"}"""
    }

    "should encode completed UpdateDynamoResponse" in {
      val response: UpdateDynamoResponse = UpdateDynamoResponse(
        status = Completed,
        initiationReference = "someRequestId",
        subjectEmail = "testSubjectEmail",
        dataProvider = "formstack",
        accountNumber = 1,
        formPage = None,
        count = None,
        requestType = SAR,
        timeOfStart = LocalDateTime.of(2020, 2, 1, 0, 0)
      )

      response.asJson.printWith(jsonPrinter) shouldBe """{"status":"completed","initiationReference":"someRequestId","subjectEmail":"testSubjectEmail","dataProvider":"formstack","accountNumber":1,"requestType":"SAR","timeOfStart":"2020-02-01T00:00:00"}"""
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
