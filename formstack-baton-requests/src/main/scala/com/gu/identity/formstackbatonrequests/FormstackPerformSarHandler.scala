package com.gu.identity.formstackbatonrequests

import java.time.{Instant, LocalDate, LocalDateTime}

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Failed, SarPerformRequest, SarPerformResponse, SarRequest, SarResponse}
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, S3Client, S3WriteSuccess, SubmissionTableUpdateDate}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

case class SubmissionIdEmail(email: String, submissionId: String, receivedByLambdaTimestamp: Long, accountNumber: Int)
case class FormstackLabelValue(label: String, value: String)
case class FormstackSubmissionQuestionAnswer(id: String, timestamp: String, fields: List[FormstackLabelValue])

case class FormstackPerformSarHandler(
  dynamoClient: DynamoClient,
  formstackClient: FormstackSar,
  s3Client: S3Client,
  config: PerformSarLambdaConfig)
  extends LazyLogging with FormstackHandler[SarRequest, SarResponse] {

  implicit val ec: ExecutionContext = ExecutionContext.global

  def submissionsWithEmailAndAccount(submissions: List[FormSubmission], accountNumber: Int): List[SubmissionIdEmail] = {
    val emailReg = """(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b""".r
    submissions.foldLeft(List.empty[SubmissionIdEmail]) { (acc, submission) =>
      val submissionValues = submission.data.map(field => field._2.value).toList
      val emailList = submissionValues.collect { case jsonValue => emailReg.findAllIn(jsonValue.toString).toList }.flatten
      val receivedByLambdaTimestamp = Instant.now.getEpochSecond
      val submissionsIdEmails = emailList.map(email => SubmissionIdEmail(email.toLowerCase, submission.id, receivedByLambdaTimestamp, accountNumber))
      submissionsIdEmails ::: acc
    }
  }

  private def skipDecryptionError(err: Throwable): Either[Throwable, Unit] = {
    err match {
      case _: FormstackDecryptionError => Right(())
      case err => Left(err)
    }
  }

  private def writeSubmissions(form: Form, lastUpdate: SubmissionTableUpdateDate, submissionPage: Int = 1, token: FormstackAccountToken): Either[Throwable, Unit] = {
    formstackClient.formSubmissionsForGivenPage(submissionPage, form.id, lastUpdate, config.encryptionPassword, token) match {
      case Left(err) => skipDecryptionError(err)
      case Right(response) =>
        logger.info(s"Received page $submissionPage of submissions out of ${response.pages} pages for form ${form.id}.")
        val submissionsIdsWithEmails = submissionsWithEmailAndAccount(response.submissions, token.account)
        logger.info(s"Writing ${submissionsIdsWithEmails.length} submission id(s) and emails to Dynamo")
        dynamoClient.writeSubmissions(submissionsIdsWithEmails, config.bcryptSalt, config.submissionTableName) match {
          case Right(unprocessedItems) if unprocessedItems.nonEmpty =>
            Left(new Exception(s"Some items could not be written to DynamoDB: $unprocessedItems"))
          case Right(_) if submissionPage < response.pages => writeSubmissions(form, lastUpdate, submissionPage + 1, token)
          case Right(_) => Right(())
          case Left(err) => Left(err)
        }
    }
  }

  def updateSubmissionsTable(formsPage: Int, lastUpdate: SubmissionTableUpdateDate, count: Int, token: FormstackAccountToken): Either[Throwable, Unit] = {
    logger.info(s"----------Getting page $formsPage of forms.----------")
    formstackClient.accountFormsForGivenPage(formsPage, token) match {
      case Left(err) => Left(err)
      case Right(response) =>
        val forms = response.forms
        val formResults = forms.map { form =>
          logger.info(s"Processing results for form ${form.id}")
          writeSubmissions(form, lastUpdate, token = token)
        }
        val errors = formResults.collect { case Left(err) => err }
        if (errors.nonEmpty) {
          Left(new Exception(errors.toString))
        } else if ((count + FormstackSarService.resultsPerPage) <= response.total) {
          updateSubmissionsTable(formsPage + 1, lastUpdate, count + FormstackSarService.resultsPerPage, token)
        } else Right(())
    }
  }

  def updateDynamo(submissionsTableUpdateDate: SubmissionTableUpdateDate): Either[Throwable, Unit] = {
    val timestampAsDate = LocalDateTime.parse(submissionsTableUpdateDate.date, SubmissionTableUpdateDate.formatter)
    if (timestampAsDate.toLocalDate != LocalDate.now) {
      for {
        _ <- updateSubmissionsTable(1, submissionsTableUpdateDate, FormstackSarService.resultsPerPage, config.accountOneToken)
        _ <- updateSubmissionsTable(1, submissionsTableUpdateDate, FormstackSarService.resultsPerPage, config.accountTwoToken)
        _ <- dynamoClient.updateMostRecentTimestamp(config.lastUpdatedTableName, LocalDateTime.now)
      } yield ()
    } else Right(())
  }

  def initiateSar(request: SarPerformRequest): Either[Throwable, S3WriteSuccess] =
    for {
      submissionTableUpdateDate <- dynamoClient.mostRecentTimestamp(config.lastUpdatedTableName)
      _ <- updateDynamo(submissionTableUpdateDate)
      submissionIds <- dynamoClient.userSubmissions(request.subjectEmail, config.bcryptSalt, config.submissionTableName)
      submissionData <- formstackClient.submissionData(submissionIds, config)
      writeToS3Response <- s3Client.writeSuccessResult(request.initiationReference, submissionData, config)
    } yield writeToS3Response

  override def handle(request: SarRequest): Either[Throwable, SarResponse] =
    request match {
      case r: SarPerformRequest =>
        initiateSar(r) match {
          case Right(_) => Right(SarPerformResponse(Completed, r.initiationReference, r.subjectEmail, None))
          case Left(err) =>
            s3Client.writeFailedResults(r.initiationReference, err.getMessage, config)
            Right(SarPerformResponse(Failed, r.initiationReference, r.subjectEmail, Some(err.getMessage)))
        }
      case _ => Left(new Exception("Unable to retrieve email and initiation reference from request"))
    }
}
