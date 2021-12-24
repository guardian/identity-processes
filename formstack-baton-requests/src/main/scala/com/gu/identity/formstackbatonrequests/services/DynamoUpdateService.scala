package com.gu.identity.formstackbatonrequests.services

import java.time.Instant

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, SubmissionTableUpdateDate}
import com.gu.identity.formstackbatonrequests.circeCodecs.{Form, FormSubmission}
import com.gu.identity.formstackbatonrequests.sar.SubmissionIdEmail
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

case class UpdateStatus(completed: Boolean, formsPage: Option[Int], count: Option[Int], token: FormstackAccountToken)

case class DynamoUpdateService(
  formstackClient: FormstackRequestService,
  dynamoClient: DynamoClient,
  config: PerformLambdaConfig) extends LazyLogging {

  def submissionsWithEmailAndAccount(submissions: List[FormSubmission], accountNumber: Int): List[SubmissionIdEmail] = {
    val emailReg = """(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}\b""".r
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
          case Right(batchWriteItemsResults) if batchWriteItemsResults.exists { result =>
            val unprocessedItems = result.getUnprocessedItems
            !unprocessedItems.isEmpty
          } => Left(new Exception(s"Some items could not be written to DynamoDB: $batchWriteItemsResults"))
          case Right(_) if submissionPage < response.pages => writeSubmissions(form, lastUpdate, submissionPage + 1, token)
          case Right(_) => Right(())
          case Left(err) => Left(err)
        }
    }
  }


  def updateSubmissionsTable(formsPage: Int, lastUpdate: SubmissionTableUpdateDate, count: Int, token: FormstackAccountToken, context: Context): Either[Throwable, UpdateStatus] = {
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
        } else if (count < response.total) {
          updateSubmissionsTable(formsPage + 1, lastUpdate, count + FormstackService.resultsPerPage, token, context)
        } else Right(UpdateStatus(completed = true, None, None, token))
    }
  }
}
