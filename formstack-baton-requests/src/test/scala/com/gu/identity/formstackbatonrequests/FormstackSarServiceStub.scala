package com.gu.identity.formstackbatonrequests
import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import io.circe.syntax._

class FormstackSarServiceStub(
  accountFormsForGivenPageResponse: Either[Throwable, FormsResponse],
  formSubmissionsForGivenPageResponse: Either[Throwable, FormSubmissions],
  submissionDataResponse: Either[Throwable, List[FormstackSubmissionQuestionAnswer]]) extends FormstackSar {
  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse] = accountFormsForGivenPageResponse
  override def formSubmissionsForGivenPage(page: Int, formId: String, minTime: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions] = formSubmissionsForGivenPageResponse
  override def submissionData(submissionIdEmails: List[SubmissionIdEmail], config: PerformSarLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = submissionDataResponse

}

object FormstackSarServiceStub {
  val accountFormsForGivenPageSuccess =
    Right(FormsResponse(List(Form("123"), Form("234"), Form("345")), 3))

  val formSubmissionsForGivenPageSuccess =
    Right(FormSubmissions(
        List(
          FormSubmission("987", Map("fieldWithoutEmail"-> ResponseValue("noEmail".asJson), "fieldWithEmail"-> ResponseValue("email@test.com".asJson))),
          FormSubmission("876", Map("fieldWithInvalidEmail"-> ResponseValue("email@test2com".asJson))),
          FormSubmission("765", Map("fieldWithEmail"-> ResponseValue("email2@test.com".asJson))),
          FormSubmission("654", Map("fieldWithoutEmail"-> ResponseValue("noEmail".asJson))),
        ),
  pages = 1))

  val submissionDataSuccess =
    Right(List(FormstackSubmissionQuestionAnswer(
      "123",
      "2019-01-01 00:00:00",
      List(FormstackLabelValue("What is your name?", "Test Testington")))))

  val genericFormstackError = Left(new Exception("Formstack API error"))


  def withFailedResponse = new FormstackSarServiceStub(genericFormstackError, genericFormstackError, genericFormstackError)
  def withSuccessResponse = new FormstackSarServiceStub(accountFormsForGivenPageSuccess, formSubmissionsForGivenPageSuccess, submissionDataSuccess)
}