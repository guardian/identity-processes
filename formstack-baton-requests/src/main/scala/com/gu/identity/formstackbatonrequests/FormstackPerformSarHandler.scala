package com.gu.identity.formstackbatonrequests

import java.time.{Instant, LocalDate, LocalDateTime}

import com.gu.identity.formstackbatonrequests.BatonModels.{SarRequest, SarResponse}
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, SubmissionTableUpdateDate}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

case class SubmissionIdEmail(email: String, submissionId: String, receivedByLambdaTimestamp: Long, accountNumber: Int)

case class FormstackPerformSarHandler(
  dynamoClient: DynamoClient,
  formstackClient: FormstackSar,
  config: PerformSarLambdaConfig)
  extends LazyLogging with FormstackHandler[SarRequest, SarResponse] {

  implicit val ec: ExecutionContext = ExecutionContext.global

  def submissionsWithEmailAndAccount(submissions: List[FormstackSubmission], accountNumber: Int): List[SubmissionIdEmail] = {
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

  private def handleSubs(form: FormstackForm, lastUpdate: SubmissionTableUpdateDate, submissionPage: Int = 1, token: FormstackAccountToken): Either[Throwable, Unit] = {
    formstackClient.formSubmissionsForGivenPage(submissionPage, form.id, lastUpdate, config.encryptionPassword, token) match {
      case Left(err) => skipDecryptionError(err)
      case Right(response) =>
        logger.info(s"Received page $submissionPage of submissions out of ${response.pages} pages for form ${form.id}.")
        val submissionsIdsWithEmails = submissionsWithEmailAndAccount(response.submissions, token.account)
        logger.info(s"Writing ${submissionsIdsWithEmails.length} submission id(s) and emails to Dynamo")
        dynamoClient.writeSubmissions(submissionsIdsWithEmails, config.bcryptSalt, config.submissionTableName) match {
          case Right(unprocessedItems) if unprocessedItems.nonEmpty =>
            Left(new Exception(s"Some items could not be written to DynamoDB: $unprocessedItems"))
          case Right(_) if submissionPage < response.pages => handleSubs(form, lastUpdate, submissionPage + 1, token)
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
          handleSubs(form, lastUpdate, token = token)
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
        _ <- dynamoClient.updateMostRecentTimestamp(config.lastUpdatedTableName)
      } yield ()
    } else Right(())
  }
//  This will be implemented in next PR
  override def handle(request: SarRequest): Either[Throwable, SarResponse] = ???
}
