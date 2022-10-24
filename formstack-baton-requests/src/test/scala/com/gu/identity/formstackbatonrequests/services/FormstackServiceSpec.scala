package com.gu.identity.formstackbatonrequests.services

import com.gu.identity.formstackbatonrequests.circeCodecs.{Submission, SubmissionData}
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
    bcryptSalt = "bcryptSalt",
    submissionTableName = "not used",
    lastUpdatedTableName = "not used")
  val testEmail = "test@email.com"
  val encryptedEmail = "encryptedEmail!"
  val submission111 = Submission(
    id = "111",
    timestamp = "2012-03-07 21:31:09",
    data = List(SubmissionData("field1", testEmail)))

  val submission222 = Submission(
    id = "222",
    timestamp = "2013-03-07 21:31:09",
    data = List(SubmissionData("field2", testEmail))
  )



  val submissionIdEmail111 = SubmissionIdEmail(
    email = encryptedEmail,
    submissionId = "111",
    receivedByLambdaTimestamp = 0L,
    accountNumber = 1
  )
  val submissionIdEmail222 = SubmissionIdEmail(
    email = encryptedEmail,
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
      |      "value": "test@email.com"
      |    }
      |  ]
      |}
      |""".stripMargin


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
      |      "value": "test@email.com"
      |    }
      |  ]
      |}
      |""".stripMargin

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

    val parsedSuccessResponse111 = FormstackSubmissionQuestionAnswer(
      id = "111",
      timestamp = "2012-03-07 21:31:09",
      fields = List(
        FormstackLabelValue("Email address", testEmail)
      )
    )

    val parsedSuccessResponse2222 = FormstackSubmissionQuestionAnswer(
      id = "222",
      timestamp = "2013-03-07 21:31:09",
      fields = List(
        FormstackLabelValue("Another Email address field", testEmail)
      )
    )

    "return submissions data from account 1" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountOneToken))

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountOneToken))

      val formstackService = new FormstackService(http)
      formstackService.submissionData(testEmail, List(submissionIdEmail111, submissionIdEmail222), config) shouldBe Right(List(parsedSuccessResponse111, parsedSuccessResponse2222))
    }

//    "return submissions data from account 2" in {
//      val http = stub[BaseHttp]
//      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountTwoToken))
//
//      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountTwoToken))
//      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountTwoToken))
//
//      val formstackService = new FormstackService(http)
//      val account2SubmissionIds = List(submissionIdEmail111, submissionIdEmail222).map(_.copy(accountNumber = 2))
//      formstackService.submissionData(testEmail, submissionIdEmails = account2SubmissionIds, config) shouldBe Right(List(parsedSuccessResponse111, parsedSuccessResponse2222))
//    }

//    "return submissions data from account 1 and account 2" in {
//      val http = stub[BaseHttp]
//      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))
//     // (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountTwoToken))
//
//      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
//  //    (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountTwoToken))
//
//      val formstackService = new FormstackService(http)
//      val mixedSubmissionIds = List(submissionIdEmail111, submissionIdEmail222.copy(accountNumber = 2))
//
//      formstackService.submissionData(testEmail, submissionIdEmails = mixedSubmissionIds, config) shouldBe Right(List(parsedSuccessResponse111, parsedSuccessResponse2222))
//    }

    "return error if a non skippable error occurs " in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(errorReturningRequest(message ="fake error!", status = 500, token =  config.accountOneToken))

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))

      val formstackService = new FormstackService(http)
      val mixedSubmissionIds = List(submissionIdEmail111, submissionIdEmail222.copy(accountNumber = 2))
      formstackService.submissionData(testEmail, submissionIdEmails = mixedSubmissionIds, config) should be ('left)

    }

    "skip submissions that are not found in account 1 and return the other submission data" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(NotFoundRequest(config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountOneToken))

      val formstackService = new FormstackService(http)
      val submissionIds = List(submissionIdEmail111, submissionIdEmail222)
      formstackService.submissionData(testEmail, submissionIdEmails = submissionIds, config) shouldBe Right(List(parsedSuccessResponse2222))
    }

    //All the forms in account 2 are going to be moved to account 1 gradually so we don't really know if a submission we have in dynamo as belonging to account 2 is now in account 1
    //when the migration is done we can remove account 2 completely and stop looking at the account number in dynamo just assuming everything is in account 1
    "if submissions saved as account 2 are not found we should attempt to get them from account 1" in {
            val http = stub[BaseHttp]

            (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))

       //     val submissionNotFoundInAccountTwo = NotFoundRequest(config.accountTwoToken)
            val submissionFoundInAccountOne = successRequest(successResponse222, config.accountOneToken)
            //we assume the first call is in token 2 so we return the mocked response that would throw a null pointer exception if the token one is passed because of how the stub is set up
       //     (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionNotFoundInAccountTwo).noMoreThanOnce()
            (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionFoundInAccountOne).noMoreThanOnce()

            (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
            (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountOneToken))

            val formstackService = new FormstackService(http)
            val mixedSubmissionIds = List(submissionIdEmail111, submissionIdEmail222.copy(accountNumber = 2))

            //our list of submissions to retrieve are 111 from account 1 and 222 from account 2, but we expect it to get 222 from account 1 as well
            formstackService.submissionData(testEmail, mixedSubmissionIds, config) shouldBe Right(List(parsedSuccessResponse111, parsedSuccessResponse2222))

      //just to verify the two calls that are made to the same endpoint are done in the correct sequence of first attempting account two and then account one
      inSequence {
     //   (submissionNotFoundInAccountTwo.header _).verify("Authorization", config.accountTwoToken.secret)
    //    (submissionNotFoundInAccountTwo.asString _).verify()
        (submissionFoundInAccountOne.header _).verify("Authorization", config.accountOneToken.secret)
        (submissionFoundInAccountOne.asString _).verify()
      }
      }

    "skip submissions that are expected in account 2 but they are not found in either account " in {
      val http = stub[BaseHttp]
      //      //account one has both submissions
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))

 //     val submissionNotFoundInAccountTwo = NotFoundRequest(config.accountTwoToken)
      val submissionNotFoundInAccountOne = NotFoundRequest(config.accountOneToken)
      //we assume the first call is in token 2 so we return the mocked response that would throw a null pointer exception if the token one is passed because of how the stub is set up
    //  (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionNotFoundInAccountTwo).noMoreThanOnce()
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionNotFoundInAccountOne).noMoreThanOnce()

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))

      val formstackService = new FormstackService(http)
      val mixedSubmissionIds = List(submissionIdEmail111, submissionIdEmail222.copy(accountNumber = 2))

      //account 222 is not in the response as it was not found in either account
      formstackService.submissionData(testEmail, mixedSubmissionIds, config) shouldBe Right(List(parsedSuccessResponse111))

      //just to verify the two calls that are made to the same endpoint are done in the correct sequence of first attempting account two and then account one
      inSequence {
   //     (submissionNotFoundInAccountTwo.header _).verify("Authorization", config.accountTwoToken.secret)
    //    (submissionNotFoundInAccountTwo.asString _).verify()
        (submissionNotFoundInAccountOne.header _).verify("Authorization", config.accountOneToken.secret)
        (submissionNotFoundInAccountOne.asString _).verify()
      }
    }

    "Skip submissions not found in account 2 but only found in account 1 but with the wrong email " in {
      val http = stub[BaseHttp]
  //    val submissionNotFoundInAccountTwo = NotFoundRequest(config.accountTwoToken)
      val submissionFoundInAccountOne = successRequest(successResponse222, config.accountOneToken)
      //we assume the first call is in token 2 so we return the mocked response that would throw a null pointer exception if the token one is passed because of how the stub is set up
    //  (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionNotFoundInAccountTwo).noMoreThanOnce()
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionFoundInAccountOne).noMoreThanOnce()

      (http.apply _).when("https://www.formstack.com/api/v2/field/field1").returns(successRequest(successFieldBody1, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/field/field2").returns(successRequest(successFieldBody2, config.accountOneToken))

      val formstackService = new FormstackService(http)
      val submissionIdEmail222InAccountTwo = submissionIdEmail222.copy(accountNumber = 2)
      //our list of submissions to retrieve are 111 from account 1 and 222 from account 2, but we expect it to get 222 from account 1 as well
      formstackService.submissionData("aDifferentEmailNotInFormastack@email.com", List(submissionIdEmail222InAccountTwo), config) shouldBe Right(List.empty)
         //just to verify the two calls that are made to the same endpoint are done in the correct sequence of first attempting account two and then account one
      inSequence {
  //      (submissionNotFoundInAccountTwo.header _).verify("Authorization", config.accountTwoToken.secret)
   //     (submissionNotFoundInAccountTwo.asString _).verify()
        (submissionFoundInAccountOne.header _).verify("Authorization", config.accountOneToken.secret)
        (submissionFoundInAccountOne.asString _).verify()
      }
    }
  }

  "FormstackService.getValidatedSubmissionData" - {

    "return submissions data from account 1" in {
      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountOneToken))

      val formstackService = new FormstackService(http)
      val expected = ValidatedSubmissions(
        accountOneResponse = FormstackResponses(found = List(submission111, submission222), notFound = List.empty),
      )
      formstackService.getValidatedSubmissionData(testEmail,List(submissionIdEmail111, submissionIdEmail222), config) shouldBe Right(expected)
    }
    "return submissions data from account 2" in {
      val http = stub[BaseHttp]
//      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountTwoToken))
//      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(successResponse222, config.accountTwoToken))

      val formstackService = new FormstackService(http)
      val expected = ValidatedSubmissions(
        accountOneResponse = FormstackResponses(found = List.empty, notFound = List.empty),
      )
      val account2SubmissionIdEmails = List(
//        submissionIdEmail111.copy(accountNumber = config.accountTwoToken.account),
//        submissionIdEmail222.copy(accountNumber = config.accountTwoToken.account)
                )
      formstackService.getValidatedSubmissionData(testEmail,account2SubmissionIdEmails, config) shouldBe Right(expected)
    }

    "should get from account 1 submissions that are stored as account 1 in dynamodb" in {
      val http = stub[BaseHttp]
      val submissionFoundInAccountOne = successRequest(successResponse222, config.accountOneToken)
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(submissionFoundInAccountOne)
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))


      val formstackService = new FormstackService(http)

      val subsmissionIdEmail11FromAccount2 = submissionIdEmail222.copy(accountNumber = 2)
      val mixedAccountSubmissionIdEmails = List(
        submissionIdEmail111,
        subsmissionIdEmail11FromAccount2)

      val expected = ValidatedSubmissions(FormstackResponses(found = List(submission111, submission222), notFound = List.empty))


      formstackService.getValidatedSubmissionData(testEmail,mixedAccountSubmissionIdEmails, config) shouldBe Right(expected)

      //just to verify the two calls that are made to the same endpoint are done in the correct sequence of first attempting account two and then account one
      inSequence {
   //     (submissionNotFoundInAccountTwo.header _).verify("Authorization", config.accountTwoToken.secret)
    //    (submissionNotFoundInAccountTwo.asString _).verify()
        (submissionFoundInAccountOne.header _).verify("Authorization", config.accountOneToken.secret)
        (submissionFoundInAccountOne.asString _).verify()
      }
    }
  }
  "FormstackService.validateAndFixSubmissionIdEmails" - {
    "return the list unchanged if all the submissions are found in the expected account" in {
      val submissionsIds = List(
        submissionIdEmail111, submissionIdEmail222
      )
      val http = stub[BaseHttp]
      val formstackService = new FormstackService(http) {
        override def getValidatedSubmissionData(requestEmail:String , submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig) = Right(
          ValidatedSubmissions(
            accountOneResponse =  FormstackResponses(found = List(submission111, submission222), notFound = List.empty),
          ))
      }

      formstackService.validateAndFixSubmissionIdEmails(testEmail, submissionsIds, config)  shouldBe Right(submissionsIds)
    }

    "fix the account number if a submission is expected in account 2 but it's actually found in account 1" in {
      val account2Submissions = List(
        submissionIdEmail111.copy(accountNumber = 2),
        submissionIdEmail222.copy(accountNumber = 2)
      )
      val http = stub[BaseHttp]
      val formstackService = new FormstackService(http) {
        override def getValidatedSubmissionData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig) = Right(
          ValidatedSubmissions(FormstackResponses(found = List(submission111, submission222), notFound = List.empty)))
      }
      val SubmissionIdsAllInAccountOne = List(
        submissionIdEmail111,
        submissionIdEmail222
      )
      formstackService.validateAndFixSubmissionIdEmails(testEmail, account2Submissions, config)  shouldBe Right(SubmissionIdsAllInAccountOne)
    }

    "remove submissions that are not found in either account" in {
      val account2Submissions = List(
        submissionIdEmail111.copy(accountNumber = 2),
        submissionIdEmail222.copy(accountNumber = 2)
      )
      val http = stub[BaseHttp]
      val formstackService = new FormstackService(http) {
        override def getValidatedSubmissionData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig) = Right(
          ValidatedSubmissions(
            accountOneResponse =  FormstackResponses(found = List.empty, notFound = List(submissionIdEmail222, submissionIdEmail111)),
//            accountTwoResponse = FormstackResponses(found =  List.empty, notFound = List(submissionIdEmail111)),
          ))
      }

      formstackService.validateAndFixSubmissionIdEmails(testEmail, account2Submissions, config)  shouldBe Right(List.empty)
    }

    "remove submissions with the wrong email" in {
      val response222WithTheWrongEmail =
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
          |      "value": "WRONGEMAIL@email.com"
          |    }
          |  ]
          |}
          |""".stripMargin

      val http = stub[BaseHttp]
      (http.apply _).when("https://www.formstack.com/api/v2/submission/111.json").returns(successRequest(successResponse111, config.accountOneToken))
      (http.apply _).when("https://www.formstack.com/api/v2/submission/222.json").returns(successRequest(response222WithTheWrongEmail, config.accountOneToken))

      val formstackService = new FormstackService(http)
      val expected = ValidatedSubmissions(
        accountOneResponse = FormstackResponses(found =  List(submission111), notFound = List(submissionIdEmail222)),
//        accountTwoResponse = FormstackResponses(found = List.empty, notFound = List.empty),
      )
      val subIds = List(
        submissionIdEmail111,
        submissionIdEmail222)
      formstackService.getValidatedSubmissionData(testEmail,subIds, config) shouldBe Right(expected)
    }
  }
  "FormstackService.validateEmail" - {

    def submissionWithData(data: Map[String,String]) = Submission(
      id = "someID",
      timestamp = "",
      data = data.map {
        case (key,value) => SubmissionData(key, value)
      }.toList
    )

    val submission = submissionWithData(
      Map(
        "key11" -> "notTheEmail@email.com",
        "key1" -> "value1",
        "key2" -> "expected@email.com",
        "key3" -> "value3",
        "key4" -> "alsonotTheEmail@email.com"

      ))
    "return true if the formstack submission contains the expected email" in {
      val http = stub[BaseHttp]
      val formstackService = new FormstackService(http)
      formstackService.validateEmail("expected@email.com", submission) shouldBe(true)
    }
    "return true if the formstack submission contains the expected email, ignoring case" in {
      val http = stub[BaseHttp]
      val formstackService = new FormstackService(http)
      formstackService.validateEmail("ExPecTed@email.com", submission) shouldBe(true)
    }
    "return false if the formstack submission does not contain the expected email" in {
      val http = stub[BaseHttp]
      val formstackService = new FormstackService(http)
      formstackService.validateEmail("notInForm@email.com", submission) shouldBe(false)
    }
    "return false if the formstack submission does not contain any emails" in {
      val http = stub[BaseHttp]
      val formstackService = new FormstackService(http)
      val noEmailsSubmission = submissionWithData(Map("key1" -> "notAnEmail"))
      formstackService.validateEmail("notInForm@email.com", noEmailsSubmission) shouldBe(false)
    }
    "return false if the formstack submission does not contain any data" in {
      val http = stub[BaseHttp]
      val formstackService = new FormstackService(http)
      val emptySubmission = submission.copy(data = List.empty)
      formstackService.validateEmail("notInForm@email.com", emptySubmission) shouldBe(false)
    }
  }
}
