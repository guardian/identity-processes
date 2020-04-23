package com.gu.identity.formstackbatonrequests
import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import io.circe.syntax._

class FormstackSarServiceStub(
  accountFormsForGivenPageResponse: Either[Throwable, FormstackFormsResponse],
  formSubmissionsForGivenPageResponse: Either[Throwable, FormstackFormSubmissionsResponse],
  submissionDataResponse: Either[Throwable, List[FormstackSubmissionQuestionAnswer]]) extends FormstackSar {
  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormstackFormsResponse] = accountFormsForGivenPageResponse
  override def formSubmissionsForGivenPage(page: Int, formId: String, minTime: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormstackFormSubmissionsResponse] = formSubmissionsForGivenPageResponse
  override def submissionData(submissionIdEmails: List[SubmissionIdEmail], config: PerformSarLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = submissionDataResponse

}

object FormstackSarServiceStub {
  val accountFormsForGivenPageSuccess =
    Right(FormstackFormsResponse(List(FormstackForm("123"), FormstackForm("234"), FormstackForm("345")), 3))

  val formSubmissionsForGivenPageSuccess =
    Right(FormstackFormSubmissionsResponse(
        List(
          FormstackSubmission("987", Map("fieldWithoutEmail"-> FormstackResponseValue("noEmail".asJson), "fieldWithEmail"-> FormstackResponseValue("email@test.com".asJson))),
          FormstackSubmission("876", Map("fieldWithInvalidEmail"-> FormstackResponseValue("email@test2com".asJson))),
          FormstackSubmission("765", Map("fieldWithEmail"-> FormstackResponseValue("email2@test.com".asJson))),
          FormstackSubmission("654", Map("fieldWithoutEmail"-> FormstackResponseValue("noEmail".asJson))),
        ),
  pages = 1))

  val submissionDataSuccess =
    Right(List(FormstackSubmissionQuestionAnswer(
      "123",
      "2019-01-01 00:00:00",
      List(FormstackQuestionAnswer("123", "2019-01-01 00:00:00", "What is your name?", "Test Testington")))))

  val genericFormstackError = Left(new Exception("Formstack API error"))


  def withFailedResponse = new FormstackSarServiceStub(genericFormstackError, genericFormstackError, genericFormstackError)
  def withSuccessResponse = new FormstackSarServiceStub(accountFormsForGivenPageSuccess, formSubmissionsForGivenPageSuccess, submissionDataSuccess)
}