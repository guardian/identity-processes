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
import scalaj.http.{BaseHttp, Http, HttpResponse}

trait FormstackRequestService {
  def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormsResponse]
  def formSubmissionsForGivenPage(page: Int, formId: String, minTime: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormSubmissions]
  def submissionData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]]
  def deleteUserData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]]
}
//this used to group results to multiple formstack calls in found and not found results.
//for not found results we basically have just the id and for the found results we have whatever the particular api call would return
case class FormstackResponses[T](found:List[T], notFound: List[SubmissionIdEmail])
//this just used to group results of fetching submissions by which account they were fetched from. This might not agree with the account number dynamodb might assign to the submission
case class ValidatedSubmissions(accountOneResponse:FormstackResponses[Submission], accountTwoResponse: FormstackResponses[Submission])

sealed trait FormstackSkippableError extends Throwable
case class FormstackDecryptionError(message: String) extends FormstackSkippableError
case class FormstackAuthError(message: String) extends FormstackSkippableError

case class FormstackService(http:BaseHttp = Http) extends FormstackRequestService with LazyLogging {

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
    minTime: SubmissionTableUpdateDate,
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
          ("min_time", minTime.date)
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

  private def getSubmissions(
    requestEmail: String,
    submissionIdEmails: List[SubmissionIdEmail],
    accountToken: FormstackAccountToken,
    encryptionPassword: String): Either[Throwable, FormstackResponses[Submission]] = {

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
      FormstackResponses[Submission](
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

  def getSubQandAForAccount(requestEmail:String, accountSubmissions: List[SubmissionIdEmail], token:FormstackAccountToken, encryptionPassword: String)= {
      for {
        submissionsResponse <- getSubmissions(requestEmail, accountSubmissions, token, encryptionPassword)
        labelsAndValues <- getSubmissionQuestionsAnswers(submissionsResponse.found, token)
      } yield FormstackResponses[FormstackSubmissionQuestionAnswer](found = labelsAndValues, notFound = submissionsResponse.notFound)
  }

  /**
   * This method gets a list of submission ids and the form they are expected to be in and returns a ValidatedSubmissions object
   * which details which submissions were found on each account.
   * This is useful to support forms migrating from one formstack account to the other as the account number recorded in dynamo would not be accurate anymore
   *
   */
  def getValidatedSubmissionData(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, ValidatedSubmissions] = {
    logger.info(s"retrieving submission data for ${submissionIdEmails.length} submissions to validate")
    val accountTwoSubmissions = submissionIdEmails.filter(_.accountNumber == config.accountTwoToken.account)
    for {
      accountTwoResults <- getSubmissions(requestEmail, accountTwoSubmissions, config.accountTwoToken, config.encryptionPassword)
      accountOneSubmissions = submissionIdEmails.filter(_.accountNumber == config.accountOneToken.account)
      submissionsToFetchFromAccountOne = accountOneSubmissions ++ accountTwoResults.notFound
      accountOneResults <- getSubmissions(requestEmail, submissionsToFetchFromAccountOne, config.accountOneToken, config.encryptionPassword)
    } yield ValidatedSubmissions(
      accountOneResponse = accountOneResults,
      accountTwoResponse = accountTwoResults)
  }

  override def submissionData(requestEmail: String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[FormstackSubmissionQuestionAnswer]] = {
    logger.info(s"retrieving submission data for ${submissionIdEmails.length} submissions")
    val accountTwoSubmissions = submissionIdEmails.filter(_.accountNumber == config.accountTwoToken.account)
    for {
      accountTwoResults <- getSubQandAForAccount(requestEmail, accountTwoSubmissions, config.accountTwoToken, config.encryptionPassword)
      accountOneSubmissions = submissionIdEmails.filter(_.accountNumber == config.accountOneToken.account)
      submissionsToFetchFromAccountOne = accountOneSubmissions ++ accountTwoResults.notFound
      accountOneResults <- getSubQandAForAccount(requestEmail, submissionsToFetchFromAccountOne, config.accountOneToken, config.encryptionPassword)
    } yield accountOneResults.found ++ accountTwoResults.found
  }

  def validateAndFixSubmissionIdEmails(requestEmail:String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionIdEmail]] = {
    val submissionIdEmailbyId: Map[String, SubmissionIdEmail] = submissionIdEmails.map(sidEmail => sidEmail.submissionId -> sidEmail).toMap

    def fixedSubmissionsFor(subResponse: FormstackResponses[Submission], originAccount: Int): List[SubmissionIdEmail] = {
      //for each submissions found in this account get the submission and make sure it refers to the correct id
      subResponse.found.map{ validatedSubmission =>
        submissionIdEmailbyId(validatedSubmission.id).copy(accountNumber = originAccount)
      }.toList

    }
    for {
      validatedSubmissions <- getValidatedSubmissionData(requestEmail, submissionIdEmails, config)
    } yield fixedSubmissionsFor(validatedSubmissions.accountOneResponse, 1) ++ fixedSubmissionsFor(validatedSubmissions.accountTwoResponse, 2)
  }

  override def deleteUserData(requestEmail: String, submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]] = {
    for {
      //We "fix" the submissions that referer to the wrong formstack account in dynamo
      fixedSubmissionIdEmails <- validateAndFixSubmissionIdEmails(requestEmail, submissionIdEmails, config)
      deleteResponse <- deleteValidatedSubmissions(fixedSubmissionIdEmails, config)
    } yield deleteResponse

  }

    def deleteValidatedSubmissions(submissionIdEmails: List[SubmissionIdEmail], config: PerformLambdaConfig): Either[Throwable, List[SubmissionDeletionReponse]] = {
    logger.info(s"deleting ${submissionIdEmails.length} submissions.")
    val tokens = List(config.accountOneToken, config.accountTwoToken)
    val deletionResponses: Either[Throwable, List[Option[SubmissionDeletionReponse]] ]= submissionIdEmails.traverse { submissionIdEmail =>
      val token = tokens.find( token => token.account == submissionIdEmail.accountNumber).get
      val response =
        http(s"https://www.formstack.com/api/v2/submission/${submissionIdEmail.submissionId}.json")
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

object FormstackService{
  val formResultsPerPage = 25
  val submissionResultsPerPage = 100
}