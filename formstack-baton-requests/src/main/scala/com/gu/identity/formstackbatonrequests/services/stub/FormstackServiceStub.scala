package com.gu.identity.formstackbatonrequests.services.stub

import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import com.gu.identity.formstackbatonrequests.circeCodecs.{Form, FormSubmission, FormSubmissions, FormsResponse, ResponseValue, SubmissionDeletionReponse}
import com.gu.identity.formstackbatonrequests.sar.{FormstackLabelValue, FormstackSubmissionQuestionAnswer, SubmissionIdEmail}
import com.gu.identity.formstackbatonrequests.services.FormstackRequestService
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import io.circe.syntax._

class FormstackServiceStub(
  accountFormsForGivenPageResponse: Either[Throwable, FormsResponse],
  formSubmissionsForGivenPageResponse: Either[Throwable, FormSubmissions],
  submissionDataResponse: Either[Throwable, List[FormstackSubmissionQuestionAnswer]],
  deleteDataResponse: Either[Throwable, List[SubmissionDeletionReponse]]) extends FormstackRequestService {
  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse] = accountFormsForGivenPageResponse
  override def formSubmissionsForGivenPage(page: Int, formId: String, minTimeUTC: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions] = formSubmissionsForGivenPageResponse
  override def submissionData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = submissionDataResponse
  override def deleteUserData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]] = deleteDataResponse
}

object FormstackServiceStub {
  val accountFormsForGivenPageSuccess =
    Right(FormsResponse(List(Form("123", "form123"), Form("234", "form234"), Form("345","form345")), 3))

  val formSubmissionsForGivenPageSuccess =
    Right(FormSubmissions(
        List(
          FormSubmission("987", Map("fieldWithoutEmail"-> ResponseValue("noEmail".asJson), "fieldWithEmail"-> ResponseValue("email@test.com".asJson))),
          FormSubmission("876", Map("fieldWithInvalidEmail"-> ResponseValue("email@test2com".asJson))),
          FormSubmission("765", Map("fieldWithEmail"-> ResponseValue("email2@test.com".asJson))),
          FormSubmission("654", Map("fieldWithoutEmail"-> ResponseValue("noEmail".asJson))),
        ),
  pages = 3))

  val submissionDataSuccess =
    Right(List(FormstackSubmissionQuestionAnswer(
      "123",
      "2019-01-01 00:00:00",
      List(FormstackLabelValue("What is your name?", "Test Testington")))))

  val deleteDataSuccess =
    Right(List(SubmissionDeletionReponse(1)))

  val genericFormstackError = new Exception("Formstack API error")
  val genericFormstackErrorLeft = Left(genericFormstackError)

  def withFailedResponse = new FormstackServiceStub(genericFormstackErrorLeft, genericFormstackErrorLeft, genericFormstackErrorLeft, genericFormstackErrorLeft)
  def withSuccessResponse = new FormstackServiceStub(accountFormsForGivenPageSuccess, formSubmissionsForGivenPageSuccess, submissionDataSuccess, deleteDataSuccess)
}