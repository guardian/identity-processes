package com.gu.identity.formstackbatonrequests
import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import io.circe.syntax._

class FormstackClientStub(
  accountFormsForGivenPageResponse: Either[Throwable, FormstackFormsResponse],
  formSubmissionsForGivenPageResponse: Either[Throwable, FormstackFormSubmissionsResponse]) extends FormstackSar {
  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormstackFormsResponse] = accountFormsForGivenPageResponse
  override def formSubmissionsForGivenPage(page: Int, formId: String, minTime: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormstackFormSubmissionsResponse] = formSubmissionsForGivenPageResponse
}

object FormstackClientStub {
  val accountFormsForGivenPageSuccess =
    Right(FormstackFormsResponse(List(FormstackForm("123"), FormstackForm("234"), FormstackForm("345")), 3))
  val accountFormsForGivenPageFailure = Left(new Exception("Formstack API error"))

  val formSubmissionsForGivenPageSuccess =
    Right(FormstackFormSubmissionsResponse(
        List(
          FormstackSubmission("987", Map("fieldWithoutEmail"-> FormstackResponseValue("noEmail".asJson), "fieldWithEmail"-> FormstackResponseValue("email@test.com".asJson))),
          FormstackSubmission("876", Map("fieldWithoutEmail"-> FormstackResponseValue("noEmail".asJson), "fieldWithEmail"-> FormstackResponseValue("email@test2.com".asJson)))
        ),
      pages = 1))
  val formSubmissionsForGivenPageFailure = Left(new Exception("Formstack API error"))


  def withFailedResponse = new FormstackClientStub(accountFormsForGivenPageFailure, formSubmissionsForGivenPageFailure)
  def withSuccessResponse = new FormstackClientStub(accountFormsForGivenPageSuccess, formSubmissionsForGivenPageSuccess)
}