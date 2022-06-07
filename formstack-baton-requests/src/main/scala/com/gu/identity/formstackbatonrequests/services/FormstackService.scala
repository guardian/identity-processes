package com.gu.identity.formstackbatonrequests.services

import cats.implicits._
import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import com.gu.identity.formstackbatonrequests.circeCodecs._
import com.gu.identity.formstackbatonrequests.sar.{FormstackLabelValue, FormstackSubmissionQuestionAnswer, SubmissionIdEmail}
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder
import io.circe.parser.decode
import scalaj.http.{Http, HttpResponse}

trait FormstackRequestService {
  def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse]
  def formSubmissionsForGivenPage(page: Int, formId: String, minTime: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions]
  def submissionData(submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]]
  def deleteUserData(submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]]
}

case class FormstackDecryptionError(message: String) extends Throwable

object FormstackService extends FormstackRequestService with LazyLogging {

  val formResultsPerPage = 25
  val submissionResultsPerPage = 100

  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse] = {
    val response = Http(s"https://www.formstack.com/api/v2/form.json")
      .header("Authorization", accountToken.secret)
      .params(
        Seq(
          ("page", page.toString),
          ("per_page", formResultsPerPage.toString)
        )
      ).asString

    if(!response.is2xx) {
      logger.error(response.body)
    }
    decode[FormsResponse](response.body)
  }

  override def formSubmissionsForGivenPage(
    page: Int,
    formId: String,
    minTime: SubmissionTableUpdateDate,
    encryptionPassword: String,
    accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions] = {
    val response = Http(s"https://www.formstack.com/api/v2/form/$formId/submission.json")
      .headers(
        Seq(
          ("Authorization", accountToken.secret),
          ("X-FS-ENCRYPTION-PASSWORD", encryptionPassword)
        )
      )
      .params(
        Seq(
          ("page", page.toString),
          ("per_page", submissionResultsPerPage.toString),
          ("data", "true"),
          ("expand_data", "true"),
          ("sort", "DESC"),
          ("min_time", minTime.date)
        )
      ).asString

    if(!response.is2xx) {
      logger.error(response.body)
    }

    /* There are a couple of forms that the Formstack API can't seem to decrypt. There seems to be no way around this
     *  so we capture this specific error and skip these forms. */
    // We also skip responses from Formstack that contain "Incorrect password". These are undocumented, and we do not know why they happen.
    if(response.body.contains("An error occurred while decrypting the submissions") || response.body.contains("Incorrect password"))
      Left(FormstackDecryptionError(s"${response.body} | form id: ${formId}"))
    else
      decode[FormSubmissions](response.body)
  }
  
  val skippableErrorMessages = List(
    "A valid submission id was not supplied",
    "A valid form could not be found"
  )

  def isSkippableError(response: HttpResponse[String] ) : Boolean = !response.is2xx && skippableErrorMessages.exists(response.body.contains)

  def decodeIfNotSkippableError[T: Decoder](response: HttpResponse[String]): Either[Throwable, Option[T]] = decode[T](response.body) match {
    case Left(_) if isSkippableError(response) =>
      logger.info(s"skipping response with status: ${response.statusLine}")
      Right(None)
    case Left(e) => Left(e)
    case Right(submission) => Right(Some(submission))
  }
  
  private def getSubmissions(
    submissionIdEmails: List[SubmissionIdEmail],
    accountToken: FormstackAccountToken,
    encryptionPassword: String): Either[Throwable, List[Submission]] = {
    val submissionResults: Either[Throwable, List[Option[Submission]]] = submissionIdEmails.traverse { submissionIdEmail =>
      val response =
        Http(s"https://www.formstack.com/api/v2/submission/${submissionIdEmail.submissionId}.json")
          .header("Authorization", accountToken.secret)
          .param("encryption_password", encryptionPassword)
          .asString
          
      if(!response.is2xx) {
        logger.error(response.body)
      }
      
          decodeIfNotSkippableError[Submission](response)
    }
    submissionResults.map(_.flatten)
   }

  private def getSubmissionQuestionsAnswers(
    submissions: List[Submission],
    accountToken: FormstackAccountToken
  ): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = {
    submissions.traverse { submission =>
      val labelsAndValuesOrError = submission.data.map { responseValues =>
        val fieldId = responseValues.field
        val response = Http(s"https://www.formstack.com/api/v2/field/$fieldId")
          .header("Authorization", accountToken.secret)
          .asString

        if(!response.is2xx) {
          logger.error(response.body)
        }

        decode[SubmissionLabelField](response.body)
          .map(label => FormstackLabelValue(label.label, responseValues.value.toString))
      }.sequence

      labelsAndValuesOrError.map(labelsAndValues => FormstackSubmissionQuestionAnswer(submission.id, submission.timestamp, labelsAndValues))
    }
  }

  override def submissionData(submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = {
    logger.info(s"retrieving submission data for ${submissionIdEmails.length} submissions")
    val tokens = List(config.accountOneToken, config.accountTwoToken)
    tokens.traverse { token =>
      val accountSubmissions = submissionIdEmails.filter(sub => sub.accountNumber == token.account)
      for {
        fieldsAndValues <- getSubmissions(accountSubmissions, token, config.encryptionPassword)
        labelsAndValues <- getSubmissionQuestionsAnswers(fieldsAndValues, token)
      } yield labelsAndValues
    }.map(accountSubmissions => accountSubmissions.flatten)
  }

  override def deleteUserData(submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]] = {
    logger.info(s"deleting ${submissionIdEmails.length} submissions.")
    val tokens = List(config.accountOneToken, config.accountTwoToken)
    val deletionResponses: Either[Throwable, List[Option[SubmissionDeletionReponse]] ]= submissionIdEmails.traverse { submissionIdEmail =>
      val token = tokens.find( token => token.account == submissionIdEmail.accountNumber).get
      val response =
        Http(s"https://www.formstack.com/api/v2/submission/${submissionIdEmail.submissionId}.json")
          .method("DELETE")
          .header("Authorization", token.secret)
          .asString

      if(!response.is2xx) {
        logger.error(response.body)
      }

      decodeIfNotSkippableError[SubmissionDeletionReponse](response)
    }

    deletionResponses.map(_.flatten) 
  }
}
