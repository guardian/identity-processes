package com.gu.identity.formstackbatonrequests

import com.typesafe.scalalogging.LazyLogging
import scalaj.http.Http
import io.circe.parser.decode
import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import io.circe.Json
import io.circe.generic.JsonCodec
import cats.implicits._

trait FormstackSar {
  def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse]
  def formSubmissionsForGivenPage(page: Int, formId: String, minTime: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions]
  def submissionData(submissionIdEmails: List[SubmissionIdEmail], config: PerformSarLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]]
}
/* Codecs for decoding accountFormsForGivenPage response */
@JsonCodec case class Form(id: String)
@JsonCodec case class FormsResponse(forms: List[Form], total: Int)

/* Codecs for decoding formSubmissionsForGivenPage response */
@JsonCodec case class ResponseValue(value: Json)
@JsonCodec case class FormSubmission(id: String, data: Map[String, ResponseValue])
@JsonCodec case class FormSubmissions(submissions: List[FormSubmission], pages: Int)

/* Codecs for decoding submissionsById response */
@JsonCodec case class SubmissionData(field: String, value: String)
@JsonCodec case class Submission(id: String, timestamp: String, data: List[SubmissionData])

/* Codecs for decoding retrieveSubmissionLabels*/
@JsonCodec case class SubmissionLabelField(label: String)

case class FormstackDecryptionError(message: String) extends Throwable

object FormstackSarService extends FormstackSar with LazyLogging {

  val resultsPerPage = 25

  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse] = {
    val response = Http(s"https://www.formstack.com/api/v2/form.json")
      .header("Authorization", accountToken.secret)
      .params(
        Seq(
          ("page", page.toString),
          ("per_page", resultsPerPage.toString)
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
          ("per_page", resultsPerPage.toString),
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
    if(response.body.contains("An error occurred while decrypting the submissions"))
      Left(FormstackDecryptionError(response.body))
    else
      decode[FormSubmissions](response.body)
  }

  private def getSubmissions(
    submissionIdEmails: List[SubmissionIdEmail],
    accountToken: FormstackAccountToken,
    encryptionPassword: String): Either[Throwable, List[Submission]] = {
    submissionIdEmails.traverse { submissionIdEmail =>
      val response =
        Http(s"https://www.formstack.com/api/v2/submission/${submissionIdEmail.submissionId}.json")
          .header("Authorization", accountToken.secret)
          .param("encryption_password", encryptionPassword)
          .asString

      if(!response.is2xx) {
        logger.error(response.body)
      }

      decode[Submission](response.body)
    }
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

  override def submissionData(submissionIdEmails: List[SubmissionIdEmail], config: PerformSarLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = {
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
}
