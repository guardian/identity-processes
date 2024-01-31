package com.gu.identity.formstackbatonrequests.services

import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult
import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.aws.DynamoClient
import com.gu.identity.formstackbatonrequests.circeCodecs.{Form, FormSubmission, FormSubmissions}
import com.gu.identity.formstackbatonrequests.sar.SubmissionIdEmail
import com.gu.identity.formstackbatonrequests.services.Util.extractEmails
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

import java.time.{Instant, LocalDateTime}


case class UpdateStatus(completed: Boolean, formsPage: Option[Int], count: Option[Int], token: FormstackAccountToken)

case class DynamoUpdateService(
                                formstackClient: FormstackRequestService,
                                dynamoClient: DynamoClient,
                                config: PerformLambdaConfig) extends LazyLogging {

  def submissionsWithEmailAndAccount(submissions: List[FormSubmission], accountNumber: Int): List[SubmissionIdEmail] = {

    submissions.foldLeft(List.empty[SubmissionIdEmail]) { (acc, submission) =>
      val submissionValues = submission.data.map(field => field._2.value).toList
      val emailList = submissionValues.collect { case jsonValue => extractEmails(jsonValue.toString) }.flatten
      val receivedByLambdaTimestamp = Instant.now.getEpochSecond
      val submissionsIdEmails = emailList.map(email => SubmissionIdEmail(email.toLowerCase, submission.id, receivedByLambdaTimestamp, accountNumber))
      submissionsIdEmails ::: acc
    }
  }

  private def skipSafeErrors(formSubmissionError: Throwable): Either[Throwable, FormSubmissions] = {
    formSubmissionError match {
      case err: FormstackSkippableError => {
        logger.warn("Found skippable error while processing form submissions!", err)

        Right(FormSubmissions(
          submissions = List.empty,
          pages = 0
        ))
      }
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
                                    minTimeUTC: LocalDateTime,
                                    maxTimeUTC: Option[LocalDateTime],
                                    token: FormstackAccountToken
                                  )(
                                    submissionPage: Int = 1
                                  ): Either[Throwable, Int] = {
    logger.info(
      s"(Form ${form.id} / SubmissionPage $submissionPage): Requesting submissionPage"
    )

    for {
      response <- formstackClient.formSubmissionsForGivenPage(
        page = submissionPage,
        formId = form.id,
        minTimeUTC = minTimeUTC,
        maxTimeUTC = maxTimeUTC,
        encryptionPassword = config.encryptionPassword,
        accountToken = token
      ).left.flatMap(skipSafeErrors)

      _ = logger.info(
        s"(Form ${form.id} / SubmissionPage $submissionPage): Received submissionPage of ${response.pages} pages"
      )

      submissionsIdsWithEmails = submissionsWithEmailAndAccount(
        submissions = response.submissions,
        accountNumber = token.account
      )

      _ = logger.info(
        s"(Form ${form.id} / SubmissionPage $submissionPage): Writing ${submissionsIdsWithEmails.length} submission id(s) and emails to Dynamo"
      )

      _ <- dynamoClient.writeSubmissions(
        submissionIdsAndEmails = submissionsIdsWithEmails,
        salt = config.bcryptSalt,
        submissionsTableName = config.submissionTableName
      ).flatMap(failUnprocessedItems)

      _ = logger.info(
        s"(Form ${form.id} / SubmissionPage $submissionPage): Wrote ${submissionsIdsWithEmails.length} submission id(s) and emails to Dynamo"
      )

    } yield response.pages
  }

  private def writeSubmissions(
                                form: Form,
                                minTimeUTC: LocalDateTime,
                                maxTimeUTC: Option[LocalDateTime],
                                submissionPage: Int = 1,
                                token: FormstackAccountToken
                              ): Either[Throwable, Unit] = {

    val writeSubmissionsPageFunction = writeSubmissionsPage(
      form: Form,
      minTimeUTC: LocalDateTime,
      maxTimeUTC: Option[LocalDateTime],
      token: FormstackAccountToken
    ) _

    val processedPages = writeSubmissionsPageFunction(submissionPage)

    processedPages match {
      case Right(pages) if submissionPage < pages =>
        (submissionPage + 1 to pages)
          .par
          .map(writeSubmissionsPageFunction(_))
          .toList
          .collectFirst {
            case Left(err) => Left(err)
          }.getOrElse(Right(()))

      case Right(_) => Right(())
      case Left(err) => Left(err)
    }
  }

  /**
   * Update the submissions table with submissions that happened between minTimeUTC and optionally maxTimeUTC.
   * In normal operation minTimeUTC will be the last updated timestamp and maxTime will be None in order to bring the table fully up to date.
   */
  def updateSubmissionsTable(formsPage: Int, minTimeUTC: LocalDateTime, maxTimeUTC: Option[LocalDateTime], count: Int, token: FormstackAccountToken, context: Context): Either[Throwable, UpdateStatus] = {
    logger.info(s"----Getting page $formsPage of forms----")
    formstackClient.accountFormsForGivenPage(formsPage, token) match {
      case Left(err) => Left(err)
      case Right(response) =>
        val forms = response.forms
        val formResults: Seq[Either[Throwable, Unit]] = forms.map { form =>
          // The amount of new submissions to these forms often exceeds an apparent formstack limit on how many can be fetched in a single query (even if the api call is paginated)
          // The submissions would be ignored anyway since the form doesn't have any relevant data, so we just skip them at the form level to avoid the problem for now.
          if (form.name.toLowerCase.startsWith("ybtj")) {
            logger.info(s"skipping ybtj form id: ${form.id} name: ${form.name}")
            Right(())
          } else {
            logger.info(s"Processing results for form ${form.id}")
            writeSubmissions(form = form, minTimeUTC = minTimeUTC, maxTimeUTC = maxTimeUTC, token = token)
          }
        }
        val errors = formResults.collect { case Left(err) => err }
        if (errors.nonEmpty) {
          Left(new Exception(errors.toString))
//        } else if (count < response.total & context.getRemainingTimeInMillis > 600000) {
//          updateSubmissionsTable(formsPage + 1, minTimeUTC, maxTimeUTC, count + FormstackService.formResultsPerPage, token, context)
        } else if (count < response.total) {
          Right(UpdateStatus(completed = false, Some(formsPage + 1), Some(count + FormstackService.formResultsPerPage), token))
        } else Right(UpdateStatus(completed = true, None, None, token))
    }
  }
}
