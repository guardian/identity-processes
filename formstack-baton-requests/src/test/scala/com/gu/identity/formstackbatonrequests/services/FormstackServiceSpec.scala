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

  val submissionIdEmail111 = SubmissionIdEmail(
    email = "some@email.com",
    submissionId = "111",
    receivedByLambdaTimestamp = 0L,
    accountNumber = 1
  )
  val submissionIdEmail222 = SubmissionIdEmail(
    email = "another@email.com",
    submissionId = "222",
    receivedByLambdaTimestamp = 0L,
    accountNumber = 1
  )
  val successFieldBody1 =
    """
      |{
      |  "id": "field1",
      |  "label": "Email address"
      |}
      |""".stripMargin

  val successResponse111 =
    """
      |{
      |  "id": "111",
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
    id = "111",
    timestamp = "2012-03-07 21:31:09",
    fields = List(
      FormstackLabelValue("Email address", "some@email.com")
    )
  )
  val successFieldBody2 =
    """
      |{
      |  "id": "field2",
      |  "label": "Another Email address field"
      |}
      |""".stripMargin

  val successResponse222 =
    """
      |{
      |  "id": "222",
      |  "timestamp": "2013-03-07 21:31:09",
      |  "user_agent": "anotherUserAgent",
      |  "remote_addr": "1.2.3.4",
      |  "payment_status": "",
      |  "form": "4321",
      |  "latitude": "11",
      |  "longitude": "12",
      |  "data": [
      |    {
      |      "field": "field2",
      |      "value": "another@email.com"
      |    }
      |  ]
      |}
      |""".stripMargin

  val parsedSuccessResponse2 = FormstackSubmissionQuestionAnswer(
    id = "222",
    timestamp = "2013-03-07 21:31:09",
    fields = List(
      FormstackLabelValue("Another Email address field", "another@email.com")
    )
  )


  def successRequest(responseBody: String, token: FormstackAccountToken) = mockRequest(responseBody, 200, token)

  val notFoundResponseBody =
    """
      |{
      |"status":"error",
      |"error":"A valid submission id was not supplied"
      |}
      |""".stripMargin

  def NotFoundRequest(token:FormstackAccountToken)  = errorReturningRequest("A valid submission id was not supplied", 404, token)

  def errorReturningRequest(message: String, status: Int, token: FormstackAccountToken) = {
    val errorBody =
      s"""
         |{
         |"status":"error",
         |"error":"$message"
         |}
         |""".stripMargin
    mockRequest(errorBody, status, token)
  }

  /**
   * The library this project uses has a builder pattern where you first get a request object, then you set everything on it and finally call a method like toString to execute it.
   * Ideally we would have just refactored it so that the actual lower level http stuff is in a client class separate from the service but for now we are mocking this the best we can.
   * this function returns a fake request that expects the correct things to be set on it and returns a fake response. It doesn't go as far as to check that everything is called in the
   * right sequence though
   * @param responseBody
   * @param status
   * @param token
   * @return
   */
  def mockRequest(responseBody: String, status: Int, token: FormstackAccountToken) = {
    val request = stub[HttpRequest]
    val response = HttpResponse[String](body = responseBody, code = status, headers = Map("Status" -> mutable.ArraySeq(status.toString)))
    (request.header _).when("Authorization", token.secret).returns(request)
    (request.param _).when("encryption_password", "encryptionPassword").returns(request)
    (request.asString _).when().returns(response)
    request
  }

  "FormstackService.submissionData" - {

    "return submissions data from account 1" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountOneToken))

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountOneToken))

      val formstackService = FormstackService(http)
      formstackService.submissionData(List(submissionIdEmail111, submissionIdEmail222), config) shouldBe Right(List(parsedSuccessResponse1, parsedSuccessResponse2))
    }

    "return submissions data from account 2" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountTwoToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountTwoToken))

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountTwoToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountTwoToken))

      val formstackService = FormstackService(http)
      val account2SubmissionIds = List(submissionIdEmail111, submissionIdEmail222).map(_.copy(accountNumber = 2))
      formstackService.submissionData(submissionIdEmails = account2SubmissionIds, config) shouldBe Right(List(parsedSuccessResponse1, parsedSuccessResponse2))
    }

    "return submissions data from account 1 and account 2" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountTwoToken))

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountTwoToken))

      val formstackService = FormstackService(http)
      val mixedSubmissionIds = List(submissionIdEmail111, submissionIdEmail222.copy(accountNumber = 2))

      formstackService.submissionData(submissionIdEmails = mixedSubmissionIds, config) shouldBe Right(List(parsedSuccessResponse1, parsedSuccessResponse2))
    }

    "return error if a non skippable error occurs " in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(errorReturningRequest(message ="fake error!", status = 500, token =  config.accountTwoToken))

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountTwoToken))

      val formstackService = FormstackService(http)
      val mixedSubmissionIds = List(submissionIdEmail111, submissionIdEmail222.copy(accountNumber = 2))
      formstackService.submissionData(submissionIdEmails = mixedSubmissionIds, config) should be ('left)

    }

    "skip submissions that are not found and return the others submissions data" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(NotFoundRequest(config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountOneToken))

      val formstackService = FormstackService(http)
      val submissionIds = List(submissionIdEmail111, submissionIdEmail222)
      formstackService.submissionData(submissionIdEmails = submissionIds, config) shouldBe Right(List(parsedSuccessResponse2))
    }

    //All the forms in account 2 are going to be moved to account 1 gradually so we don't really know if a submission we have in dynamo as belonging to account 2 is now in account 1
    //when the migration is done we can remove account 2 completely and stop looking at the account number in dynamo just assuming everything is in account 1
    "if submissions saved as account 2 are not found we should attempt to get them from account 1" in {
            val http = stub[BaseHttp]
      //      //account one has both submissions
            (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))

            val submissionNotFoundInAccountTwo = NotFoundRequest(config.accountTwoToken)
            val submissionFoundInAccountOne = successRequest(successResponse222, config.accountOneToken)
            //we assume the first call is in token 2 so we return the mocked response that would throw a null pointer exception if the token one is passed because of how the stub is set up
            (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionNotFoundInAccountTwo).noMoreThanOnce()
            (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionFoundInAccountOne).noMoreThanOnce()

            (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
            (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountOneToken))

            val formstackService = FormstackService(http)
            val mixedSubmissionIds = List(submissionIdEmail111, submissionIdEmail222.copy(accountNumber = 2))

            //our list of submissions to retrieve are 111 from account 1 and 222 from account 2, but we expect it to get 222 from account 1 as well
            formstackService.submissionData(mixedSubmissionIds, config) shouldBe Right(List(parsedSuccessResponse1, parsedSuccessResponse2))

      //just to verify the two calls that are made to the same endpoint are done in the correct sequence of first attempting account two and then account one
      inSequence {
        (submissionNotFoundInAccountTwo.header _).verify("Authorization", config.accountTwoToken.secret)
        (submissionNotFoundInAccountTwo.asString _).verify()
        (submissionFoundInAccountOne.header _).verify("Authorization", config.accountOneToken.secret)
        (submissionFoundInAccountOne.asString _).verify()
      }
      }
  }



}
