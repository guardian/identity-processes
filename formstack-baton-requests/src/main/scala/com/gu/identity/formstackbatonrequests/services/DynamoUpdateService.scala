package com.gu.identity.formstackbatonrequests.services

import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult
import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, SubmissionTableUpdateDate}
import com.gu.identity.formstackbatonrequests.circeCodecs.{Form, FormSubmission, FormSubmissions}
import com.gu.identity.formstackbatonrequests.sar.SubmissionIdEmail
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

import java.time.Instant


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

  private def skipDecryptionError(formSubmissionError: Throwable): Either[Throwable, FormSubmissions] = {
    formSubmissionError match {
      case _: FormstackDecryptionError => Right(FormSubmissions(
        submissions = List.empty,
        pages = 0
      ))
      case err => Left(err)
    }
  }

  private def failUnprocessedItems(batchWriteItemsResults: Seq[BatchWriteItemResult]): Either[Throwable, Seq[BatchWriteItemResult]] = {
    val unprocessedItems = batchWriteItemsResults.exists {
      !_.getUnprocessedItems.isEmpty
    }

    if (unprocessedItems) {
      Left(new Exception(s"Some items could not be written to DynamoDB: $batchWriteItemsResults"))
    } else {
      Right(batchWriteItemsResults)
    }
  }

  private def writeSubmissionsPage(
                                    form: Form,
                                    lastUpdate: SubmissionTableUpdateDate,
                                    token: FormstackAccountToken
                                  )(
                                    submissionPage: Int = 1
                                  ): Either[Throwable, Int] = for {
    response <- formstackClient.formSubmissionsForGivenPage(
      page = submissionPage,
      formId = form.id,
      minTime = lastUpdate,
      encryptionPassword = config.encryptionPassword,
      accountToken = token
    ).left.flatMap(skipDecryptionError)

    _ = logger.info(
      s"Received page $submissionPage of submissions out of ${response.pages} pages for form ${form.id}."
    )

    submissionsIdsWithEmails = submissionsWithEmailAndAccount(
      submissions = response.submissions,
      accountNumber = token.account
    )

    _ = logger.info(
      s"Writing ${submissionsIdsWithEmails.length} submission id(s) and emails to Dynamo"
    )

    _ <- dynamoClient.writeSubmissions(
      submissionIdsAndEmails = submissionsIdsWithEmails,
      salt = config.bcryptSalt,
      submissionsTableName = config.submissionTableName
    ).flatMap(failUnprocessedItems)

    _ = logger.info(
      s"Wrote ${submissionsIdsWithEmails.length} submission id(s) and emails to Dynamo"
    )

  } yield response.pages

  private def writeSubmissions(
                                form: Form,
                                lastUpdate: SubmissionTableUpdateDate,
                                submissionPage: Int = 1,
                                token: FormstackAccountToken
                              ): Either[Throwable, Unit] = {

    val writeSubmissionsPageFunction = writeSubmissionsPage(
      form: Form,
      lastUpdate: SubmissionTableUpdateDate,
      token: FormstackAccountToken
    ) _

    logger.info(
      s"Requesting page $submissionPage for form ${form.id}."
    )

    val processedPages = writeSubmissionsPageFunction(submissionPage)

    processedPages match {
      case Right(pages) if submissionPage < pages =>
        (submissionPage + 1 to pages)
          .map(writeSubmissionsPageFunction(_))
          .collectFirst {
            case Left(err) => Left(err)
          }.getOrElse(Right(()))

      case Right(_) => Right(())
      case Left(err) => Left(err)
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
        } else if (count < response.total & context.getRemainingTimeInMillis > 300000) {
          updateSubmissionsTable(formsPage + 1, lastUpdate, count + FormstackService.formResultsPerPage, token, context)
        } else if (count < response.total) {
          Right(UpdateStatus(completed = false, Some(formsPage + 1), Some(count + FormstackService.formResultsPerPage), token))
        } else Right(UpdateStatus(completed = true, None, None, token))
    }
  }
}
