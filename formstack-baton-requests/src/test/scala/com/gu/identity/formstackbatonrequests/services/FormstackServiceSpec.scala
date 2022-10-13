package com.gu.identity.formstackbatonrequests.services

import com.gu.identity.formstackbatonrequests.sar.{FormstackLabelValue, FormstackSubmissionQuestionAnswer, SubmissionIdEmail}
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}
import scalaj.http.{BaseHttp, HttpRequest, HttpResponse}

import scala.collection.mutable

class FormstackServiceSpec extends FreeSpec with Matchers with MockFactory {
  val config = PerformLambdaConfig(
    resultsBucket = "not used",
    resultsPath = "not used",
    encryptionPassword = "encryptionPassword",
    accountOneToken = FormstackAccountToken(1, "accountOneToken"),
    accountTwoToken = FormstackAccountToken(2, "accountTwoToken"),
    bcryptSalt = "bcryptSalt",
    submissionTableName = "not used",
    lastUpdatedTableName = "not used")

  def submissionId(id: String) = SubmissionIdEmail(
    email = "",
    submissionId = id,
    receivedByLambdaTimestamp = 0,
    accountNumber = 1
  )

  val successFieldBody =
    """
      |{
      |  "id": "field1",
      |  "label": "Email address"
      |}
      |""".stripMargin

  val successResponse1 =
    """
      |{
      |  "id": "123",
      |  "timestamp": "2012-03-07 21:31:09",
      |  "user_agent": "userAgentString",
      |  "remote_addr": "123.123.123.123",
      |  "payment_status": "",
      |  "form": "12341",
      |  "latitude": "0",
      |  "longitude": "1",
      |  "data": [
      |    {
      |      "field": "field1",
      |      "value": "some@email.com"
      |    }
      |  ]
      |}
      |""".stripMargin

  val parsedSuccessResponse1 = FormstackSubmissionQuestionAnswer(
    id = "123",
    timestamp = "2012-03-07 21:31:09",
    fields = List(
      FormstackLabelValue("Email address", "some@email.com")
    )
  )

  val successResponse2 =
    """
      |{
      |  "id": "321",
      |  "timestamp": "2013-03-07 21:31:09",
      |  "user_agent": "anotherUserAgent",
      |  "remote_addr": "1.2.3.4",
      |  "payment_status": "",
      |  "form": "4321",
      |  "latitude": "11",
      |  "longitude": "12",
      |  "data": [
      |    {
      |      "field": "field1",
      |      "value": "another@email.com"
      |    }
      |  ]
      |}
      |""".stripMargin

  val parsedSuccessResponse2 = FormstackSubmissionQuestionAnswer(
    id = "321",
    timestamp = "2013-03-07 21:31:09",
    fields = List(
      FormstackLabelValue("Email address", "another@email.com")
    )
  )


  def successRequest(responseBody: String) = {
    val request = stub[HttpRequest]
    val successResponse = HttpResponse[String](body = responseBody, code = 200, headers = Map.empty)
    (request.header _).when("Authorization", "accountOneToken").returns(request)
    (request.param _).when("encryption_password", "encryptionPassword").returns(request)
    (request.asString _).when().returns(successResponse)
    request
  }

  def  notFoundResponse  = {
    val errorBody =
      """
        |{
        |"status":"error",
        |"error":"A valid submission id was not supplied"
        |}
        |""".stripMargin

    val request = stub[HttpRequest]
    val errorResponse = HttpResponse[String](body = errorBody, code = 404, headers = Map("Status" -> mutable.ArraySeq("404")))
    (request.header _).when("Authorization", "accountOneToken").returns(request)
    (request.param _).when("encryption_password", "encryptionPassword").returns(request)
    (request.asString _).when().returns(errorResponse)
    request
  }

  "FormstackService.submissionData" - {

    "return submissions data" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse1))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse2))

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody))

      val formstackService = FormstackService(http)
      formstackService.submissionData(submissionIdEmails = List(submissionId("111"), submissionId("222")), config) shouldBe Right(List(parsedSuccessResponse1, parsedSuccessResponse2))
    }

    "skip submissions that are not found and return the others submissions data" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(notFoundResponse)
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse2))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody))

      val formstackService = FormstackService(http)
      formstackService.submissionData(submissionIdEmails = List(submissionId("111"), submissionId("222")), config) shouldBe Right(List(parsedSuccessResponse2))
    }

  }


}
