package com.gu.identity.formstackbatonrequests.services

import cats.implicits._
import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import com.gu.identity.formstackbatonrequests.circeCodecs._
import com.gu.identity.formstackbatonrequests.sar.{FormstackLabelValue, FormstackSubmissionQuestionAnswer, SubmissionIdEmail}
import com.gu.identity.formstackbatonrequests.services.Util.extractEmails
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder
import io.circe.parser.decode
import scalaj.http.{BaseHttp, Http, HttpOptions, HttpResponse}

trait FormstackRequestService {
  def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse]
  def formSubmissionsForGivenPage(page: Int, formId: String, minTimeUTC: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions]
  def submissionData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]]
  def deleteUserData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]]
}
//this used to group results to multiple formstack calls in found and not found results.
//for not found results we basically have just the id and for the found results we have whatever the particular api call would return
case class FormstackResponses(found:List[Submission], notFound: List[SubmissionIdEmail])

sealed trait FormstackSkippableError extends Throwable
case class FormstackDecryptionError(message: String) extends FormstackSkippableError
case class FormstackAuthError(message: String) extends FormstackSkippableError

object CustomHttp extends BaseHttp(
  options = Seq(
    HttpOptions.connTimeout(2000),
    HttpOptions.readTimeout(10000),
    HttpOptions.followRedirects(false)
  )
)
class FormstackService(http:BaseHttp = CustomHttp) extends FormstackRequestService with LazyLogging {

import FormstackService._

  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse] = {
    val response = http(s"https://www.formstack.com/api/v2/form.json")
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
    minTimeUTC: SubmissionTableUpdateDate,
    encryptionPassword: String,
    accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions] = {
    val response = http(s"https://www.formstack.com/api/v2/form/$formId/submission.json")
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
          // this api call expects eastern time zone, see https://developers.formstack.com/reference/form-id-submission-get
          ("min_time", minTimeUTC.toEasternTime)
        )
      ).asString

    if(!response.is2xx) {
      logger.error(response.body)
    }

    /* There are a couple of forms that the Formstack API can't seem to decrypt. There seems to be no way around this
     *  so we capture this specific error and skip these forms. */
    // We also skip responses from Formstack that contain "Incorrect password". These are undocumented, and we do not know why they happen.

    response.body match {
      case message if message.contains("An error occurred while decrypting the submissions") =>
        Left(FormstackDecryptionError(s"${response.body} | form id: ${formId}"))
      case message if message.contains("Incorrect password") =>
        Left(FormstackAuthError(s"${response.body} | form id: ${formId}"))
      case _ => decode[FormSubmissions](response.body)
    }
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

  def validateEmail(expectedEmail:String, submission: Submission): Boolean = submission.data.exists{
    subData => {
      extractEmails(subData.value).exists(e => e.equalsIgnoreCase(expectedEmail))
    }
  }

  protected def getSubmissions(
    requestEmail: String,
    submissionIdEmails: List[SubmissionIdEmail],
    accountToken: FormstackAccountToken,
    encryptionPassword: String): Either[Throwable, FormstackResponses] = {

    val submissionResults: Either[Throwable, List[Either[SubmissionIdEmail, Submission]]] = submissionIdEmails.traverse { submissionIdEmail =>
      val response =
        http(s"https://www.formstack.com/api/v2/submission/${submissionIdEmail.submissionId}.json")
          .header("Authorization", accountToken.secret)
          .param("encryption_password", encryptionPassword)
          .asString

      if(!response.is2xx) {
        logger.error(response.body)
      }

      decodeIfNotSkippableError[Submission](response).map{
        case None => Left(submissionIdEmail)
        case Some(submission) =>
          //validate the submission we found in formstack actually contains references to the email we were looking for
          if (validateEmail(requestEmail, submission))
            Right(submission)
          else {
            logger.warn(s"found submission by id = ${submissionIdEmail.submissionId} but the requested email was not found, skipping")
            Left(submissionIdEmail)
          }
      }
    }

    submissionResults.map{ subResults =>
      FormstackResponses(
        found = subResults.collect{ case Right(submission) => submission},
        notFound = subResults.collect{ case Left(skippedSubmissionIdEmail) => skippedSubmissionIdEmail}
      )
    }
  }

  private def getSubmissionQuestionsAnswers(
    submissions: List[Submission],
    accountToken: FormstackAccountToken
  ): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = {
    submissions.traverse { submission =>
      val labelsAndValuesOrError = submission.data.map { responseValues =>
        val fieldId = responseValues.field
        val response = http(s"https://www.formstack.com/api/v2/field/$fieldId")
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

  override def submissionData(requestEmail: String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = {
    logger.info(s"retrieving submission data for ${submissionIdEmails.length} submissions")
    for {
      submissionsResponse <- getSubmissions(requestEmail, submissionIdEmails,  config.accountOneToken, config.encryptionPassword)
      labelsAndValues <- getSubmissionQuestionsAnswers(submissionsResponse.found,  config.accountOneToken)
    } yield labelsAndValues
  }
  //fix account number and remove submissions that don't exist in formstack with the correct email
  def validateAndFixSubmissionIdEmails(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionIdEmail]] = {
    val submissionIdEmailsById: Map[String, SubmissionIdEmail] = submissionIdEmails.map(sidEmail => sidEmail.submissionId -> sidEmail).toMap
    for {
      submissionsResponse <-  getSubmissions(requestEmail, submissionIdEmails,  config.accountOneToken, config.encryptionPassword)
      validatedSubmissionIdEmails <- Right(submissionsResponse.found.map(sub => submissionIdEmailsById(sub.id).copy(accountNumber = 1)))
    } yield validatedSubmissionIdEmails
  }

  override def deleteUserData(requestEmail: String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]] = {
    for {
      fixedSubmissionIdEmails <- validateAndFixSubmissionIdEmails(requestEmail, submissionIdEmails, config)
      deleteResponse <- deleteValidatedSubmissions(fixedSubmissionIdEmails, config)
    } yield deleteResponse

  }

  def deleteValidatedSubmissions(submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]] = {
    logger.info(s"deleting ${submissionIdEmails.length} submissions.")
    val token = config.accountOneToken
    submissionIdEmails.traverse { submissionIdEmail =>

      val response =
        http(s"https://www.formstack.com/api/v2/submission/${submissionIdEmail.submissionId}.json")
          .method("DELETE")
          .header("Authorization", token.secret)
          .asString

      if(!response.is2xx) {
        logger.error(response.body)
      }

      decode[SubmissionDeletionReponse](response.body)
    }
  }
}

object FormstackService{
  val formResultsPerPage = 25
  val submissionResultsPerPage = 100
}