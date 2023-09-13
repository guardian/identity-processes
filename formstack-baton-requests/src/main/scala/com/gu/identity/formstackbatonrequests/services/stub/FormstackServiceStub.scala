package com.gu.identity.formstackbatonrequests.services.stub

import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import com.gu.identity.formstackbatonrequests.circeCodecs.{Form, FormSubmission, FormSubmissions, FormsResponse, ResponseValue, SubmissionDeletionReponse}
import com.gu.identity.formstackbatonrequests.sar.{FormstackLabelValue, FormstackSubmissionQuestionAnswer, SubmissionIdEmail}
import com.gu.identity.formstackbatonrequests.services.FormstackRequestService
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._

import java.time.LocalDateTime

class FormstackServiceStub(
  accountFormsForGivenPageResponse: Either[Throwable, FormsResponse],
  formSubmissionsForGivenPageResponse: Either[Throwable, FormSubmissions],
  submissionDataResponse: Either[Throwable, List[FormstackSubmissionQuestionAnswer]],
  deleteDataResponse: Either[Throwable, List[SubmissionDeletionReponse]]) extends FormstackRequestService  with LazyLogging{
  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse] = {
    logger.info(s"called FormstackServiceStub.accountFormsForGivenPage(page=$page, accountToken= ***")
    accountFormsForGivenPageResponse
  }
  override def formSubmissionsForGivenPage(page: Int, formId: String, minTimeUTC: LocalDateTime, maxTimeUTC: Option[LocalDateTime], encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions] = {
    logger.info(s"called FormstackServiceStub.formSubmissionsForGivenPage(page=$page, formId=$formId, minTimeUTC=$minTimeUTC, maxDate=$maxTimeUTC, encryptionPassword=***, accountToken=***")
    formSubmissionsForGivenPageResponse
  }
  override def submissionData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = {
    logger.info("called FormstackServiceStub.submissionData")
    submissionDataResponse
  }
  override def deleteUserData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]] = {
    logger.info("called FormstackServiceStub.deleteUserData")
    deleteDataResponse
  }
}
class FormstackServiceStub1(
  accountFormsForGivenPageResponse: List[Either[Throwable, FormsResponse]],
  formSubmissionsForGivenPageResponse: Either[Throwable, FormSubmissions],
  submissionDataResponse: Either[Throwable, List[FormstackSubmissionQuestionAnswer]],
  deleteDataResponse: Either[Throwable, List[SubmissionDeletionReponse]]) extends FormstackRequestService  with LazyLogging{
  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse] = {
    logger.info(s"called FormstackServiceStub.accountFormsForGivenPage(page=$page, accountToken= ***")
    if (page == 1)
      accountFormsForGivenPageResponse(0)
    else
      accountFormsForGivenPageResponse(1)
  }
  override def formSubmissionsForGivenPage(page: Int, formId: String, minTimeUTC: LocalDateTime, maxTimeUTC: Option[LocalDateTime], encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions] = {
    logger.info(s"called FormstackServiceStub.formSubmissionsForGivenPage(page=$page, formId=$formId, minTimeUTC=$minTimeUTC, maxDate=$maxTimeUTC, encryptionPassword=***, accountToken=***")
    formSubmissionsForGivenPageResponse
  }
  override def submissionData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = {
    logger.info("called FormstackServiceStub.submissionData")
    submissionDataResponse
  }
  override def deleteUserData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]] = {
    logger.info("called FormstackServiceStub.deleteUserData")
    deleteDataResponse
  }
}

object FormstackServiceStub {
  val accountFormsForGivenPageSuccess =
    Right(FormsResponse(List(Form("123", "form123"), Form("234", "form234"), Form("345","form345")), 3))
  val accountFormsForGivenPageSuccess1 =
    Right(FormsResponse(List(Form("123", "form123"), Form("234", "form234"), Form("345","form345")), 6))
  val accountFormsForGivenPageSuccess2 =
    Right(FormsResponse(List(Form("22123", "form123"), Form("22234", "form234"), Form("22345","form345")), 6))

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
  def withSuccessResponse1 = new FormstackServiceStub1(List(accountFormsForGivenPageSuccess1, accountFormsForGivenPageSuccess2), formSubmissionsForGivenPageSuccess, submissionDataSuccess, deleteDataSuccess)
}