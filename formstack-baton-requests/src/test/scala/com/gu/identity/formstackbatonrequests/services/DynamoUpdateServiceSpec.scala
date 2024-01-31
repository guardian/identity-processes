package com.gu.identity.formstackbatonrequests.services

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, DynamoClientStub}
import com.gu.identity.formstackbatonrequests.aws.DynamoClientStub.writeSubmissionsSuccess
import com.gu.identity.formstackbatonrequests.circeCodecs.{Form, FormSubmission, FormSubmissions, FormsResponse, ResponseValue}
import com.gu.identity.formstackbatonrequests.services.stub.FormstackServiceStub
import com.gu.identity.formstackbatonrequests.services.stub.FormstackServiceStub.{accountFormsForGivenPageSuccess, deleteDataSuccess, formSubmissionsForGivenPageSuccess, submissionDataSuccess}
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import io.circe.syntax._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{EitherValues, FreeSpec, Matchers}

import java.time.LocalDateTime

class DynamoUpdateServiceSpec
  extends FreeSpec
    with Matchers
    with EitherValues
    with MockFactory {

  val mockConfig: PerformLambdaConfig =
    PerformLambdaConfig(
      "resultsBucket",
      "resultsPath",
      "encryptionPassword",
      FormstackAccountToken(1, "accountOneToken"),
      "bcryptSalt",
      "submissions-table-name",
      "last-updated-table-name"
    )

  "DynamoUpdateService" - {

    "should detect fields with email addresses and return a list of SubmissionIdEmail" in {
      FormstackServiceStub.formSubmissionsForGivenPageSuccess.map { submissionsOrError =>
        val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
          formstackClient = FormstackServiceStub.withSuccessResponse,
          dynamoClient = DynamoClientStub.withSuccessResponse,
          config = mockConfig
        )

        val submissionsWithEmail = dynamoUpdateService.submissionsWithEmailAndAccount(submissionsOrError.submissions, accountNumber = 1)
        submissionsWithEmail.length shouldBe 2
      }
    }

    val millisLongerThan10m = 850001
    val millisLessThan10m = 849999

    val dummyFormsPage = 1
    val dummyCount = 0
    val dummyToken = FormstackAccountToken(
      account = 0,
      secret = "baz"
    )
    val dummySubmissionsTableUpdateDate =  LocalDateTime.of(2023,1,1,9,23,33)
    val dummyMaxDate = LocalDateTime.of(2023,1,2,11,30,15)

    "call formstack service with the right parameters" in {
       val formstackService = mock[FormstackRequestService]
      val formsPage1 = List(Form("123", "form_123"), Form("234", "form_234"))
      val formsPage2 = List(Form("345", "form_345)"))
      val firstageOfFormsResponse =  Right(FormsResponse(formsPage1, 2))
      //the code will detect that its the last page when the set amount of forms returned is lower than the "count" parameter which represents the max amount of forms per page
      val lastPageOfFormsResponse =  Right(FormsResponse(formsPage2, 2))


      (formstackService.accountFormsForGivenPage _).expects(1, dummyToken).returning(firstageOfFormsResponse)
      (formstackService.accountFormsForGivenPage _).expects(2, dummyToken).returning(lastPageOfFormsResponse)
      formsPage1.foreach{ f =>
      (formstackService.formSubmissionsForGivenPage _).expects(1, f.id, dummySubmissionsTableUpdateDate, Some(dummyMaxDate), mockConfig.encryptionPassword, dummyToken).returning(formSubmissionsForGivenPageSuccess)
      (formstackService.formSubmissionsForGivenPage _).expects(2, f.id, dummySubmissionsTableUpdateDate, Some(dummyMaxDate), mockConfig.encryptionPassword, dummyToken).returning(formSubmissionsForGivenPageSuccess)
      (formstackService.formSubmissionsForGivenPage _).expects(3, f.id, dummySubmissionsTableUpdateDate, Some(dummyMaxDate), mockConfig.encryptionPassword, dummyToken).returning(formSubmissionsForGivenPageSuccess)
      }

      formsPage2.foreach{ f =>
      (formstackService.formSubmissionsForGivenPage _).expects(1, f.id, dummySubmissionsTableUpdateDate, Some(dummyMaxDate), mockConfig.encryptionPassword, dummyToken).returning(formSubmissionsForGivenPageSuccess)
      (formstackService.formSubmissionsForGivenPage _).expects(2, f.id, dummySubmissionsTableUpdateDate, Some(dummyMaxDate), mockConfig.encryptionPassword, dummyToken).returning(formSubmissionsForGivenPageSuccess)
      (formstackService.formSubmissionsForGivenPage _).expects(3, f.id, dummySubmissionsTableUpdateDate, Some(dummyMaxDate), mockConfig.encryptionPassword, dummyToken).returning(formSubmissionsForGivenPageSuccess)
      }
      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = formstackService,
        dynamoClient = DynamoClientStub.withSuccessResponse,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLongerThan10m)

      val expectedUpdateStatus = UpdateStatus(
        completed = true,
        formsPage = None,
        count = None,
        token = dummyToken
      )

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        minTimeUTC = dummySubmissionsTableUpdateDate,
        maxTimeUTC = Some(dummyMaxDate),
        count = dummyCount,
        token = dummyToken,
        context = mockContext
      )
      statusUpdate.right.value shouldBe expectedUpdateStatus
    }
    "successfully calls updateSubmissionsTable with > 10m of lambda runtime available" in {
      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = FormstackServiceStub.withSuccessResponse,
        dynamoClient = DynamoClientStub.withSuccessResponse,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLongerThan10m)

      val expectedUpdateStatus = UpdateStatus(
        completed = true,
        formsPage = None,
        count = None,
        token = dummyToken
      )

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        minTimeUTC = dummySubmissionsTableUpdateDate,
        maxTimeUTC = None,
        count = dummyCount,
        token = dummyToken,
        context = mockContext
      )
      statusUpdate.right.value shouldBe expectedUpdateStatus
    }

    "successfully calls updateSubmissionsTable with < 10m of lambda runtime available" in {
      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = FormstackServiceStub.withSuccessResponse,
        dynamoClient = DynamoClientStub.withSuccessResponse,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLessThan10m)

      val expectedUpdateStatus = UpdateStatus(
        completed = false,
        formsPage = Some(dummyFormsPage + 1),
        count = Some(FormstackService.formResultsPerPage),
        token = dummyToken
      )

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        minTimeUTC = dummySubmissionsTableUpdateDate,
        maxTimeUTC = None,
        count = dummyCount,
        token = dummyToken,
        context = mockContext
      )

      statusUpdate.right.value shouldBe expectedUpdateStatus
    }

    "receives a formstack error when calling updateSubmissionsTable" in {
      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = FormstackServiceStub.withFailedResponse,
        dynamoClient = DynamoClientStub.withSuccessResponse,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLessThan10m)

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        minTimeUTC = dummySubmissionsTableUpdateDate,
        maxTimeUTC = None,
        count = dummyCount,
        token = dummyToken,
        context = mockContext
      )

      statusUpdate.left.value shouldBe FormstackServiceStub.genericFormstackError
    }

    "does not fail when encountering a skippable error in formSubmissionsForGivenPageResponse" in {
      val skippableFormstackError = FormstackAuthError("Error")
      val skippableFormstackErrorLeft = Left(skippableFormstackError)

      val withSkippableErrorsFormstackSubmission = new FormstackServiceStub(
        accountFormsForGivenPageResponse = accountFormsForGivenPageSuccess,
        formSubmissionsForGivenPageResponse = skippableFormstackErrorLeft,
        submissionDataResponse = submissionDataSuccess,
        deleteDataResponse = deleteDataSuccess
      )

      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = withSkippableErrorsFormstackSubmission,
        dynamoClient = DynamoClientStub.withSuccessResponse,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLessThan10m)

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        minTimeUTC = dummySubmissionsTableUpdateDate,
        maxTimeUTC = None,
        count = dummyCount,
        token = dummyToken,
        context = mockContext
      )

      val expectedUpdateStatus = UpdateStatus(
        completed = false,
        formsPage = Some(dummyFormsPage + 1),
        count = Some(FormstackService.formResultsPerPage),
        token = dummyToken
      )

      skippableFormstackError shouldBe a[FormstackSkippableError]

      statusUpdate.right.value shouldBe expectedUpdateStatus
    }

    "receives dynamodb errors when calling updateSubmissionsTable" in {
      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = FormstackServiceStub.withSuccessResponse,
        dynamoClient = DynamoClientStub.withFailedResponse,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLessThan10m)

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        minTimeUTC = dummySubmissionsTableUpdateDate,
        maxTimeUTC = None,
        count = dummyCount,
        token = dummyToken,
        context = mockContext
      )

      // The current implementation turns a list of exceptions into a string
      // and then creates an exception using that string, for simplicity we'll
      // just check for failure here.
      statusUpdate.left.value shouldBe a[Exception]
    }

    "skips ybtj forms when calling updateSubmissionsTable" in {

      val nonSkippedForms = List( Form("123", "form123"), Form("345","form345"))
      val skippedForm = Form("234","YbTj-form-234-should_be_skipped!")
      val allForms = nonSkippedForms.head :: skippedForm :: nonSkippedForms.tail
      val accountFormsResponse = Right(FormsResponse(allForms, 3))

      def formSubmissionsForGivenPageSuccess(formId: String) =
        Right(FormSubmissions(
          List(
            FormSubmission(s"$formId-1", Map("fieldWithoutEmail" -> ResponseValue("noEmail".asJson), "fieldWithEmail" -> ResponseValue("email@test.com".asJson))),
            FormSubmission(s"$formId-2", Map("fieldWithInvalidEmail" -> ResponseValue("email@test2com".asJson))),
            FormSubmission(s"$formId-3", Map("fieldWithEmail" -> ResponseValue("email2@test.com".asJson))),
            FormSubmission(s"$formId-4", Map("fieldWithoutEmail" -> ResponseValue("noEmail".asJson))),
          ),
          pages = 1))
      val dynamoclient = stub[DynamoClient]
      (dynamoclient.writeSubmissions _).when(*,*,*).returns(writeSubmissionsSuccess)

      val formstackService = stub[FormstackRequestService]
      (formstackService.accountFormsForGivenPage _ ).when(1, dummyToken).returns(accountFormsResponse)

      nonSkippedForms.foreach{ form =>
      (formstackService.formSubmissionsForGivenPage _ ).when(*,form.id,*,*,*,*).returns(formSubmissionsForGivenPageSuccess(form.id))
      }

      val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(
        formstackClient = formstackService,
        dynamoClient = dynamoclient,
        config = mockConfig
      )

      val mockContext = stub[Context]

      (mockContext.getRemainingTimeInMillis _)
        .when()
        .anyNumberOfTimes()
        .returns(millisLongerThan10m)

      val statusUpdate = dynamoUpdateService.updateSubmissionsTable(
        formsPage = dummyFormsPage,
        minTimeUTC = dummySubmissionsTableUpdateDate,
        maxTimeUTC = None,
        count = 4, // how many forms per page of forms
        token = dummyToken,
        context = mockContext
      )

      statusUpdate.right.value shouldBe UpdateStatus(true,None,None, dummyToken)

      nonSkippedForms.foreach{ form => (formstackService.formSubmissionsForGivenPage _).verify(*,form.id,*,*,*,*).once() }
      (formstackService.formSubmissionsForGivenPage _).verify(*,skippedForm.id,*,*,*,*).never()

      (dynamoclient.writeSubmissions _).verify(*,*,*).repeat(2)
    }
  }
}
