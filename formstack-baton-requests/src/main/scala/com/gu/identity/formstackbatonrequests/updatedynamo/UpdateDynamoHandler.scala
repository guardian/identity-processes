package com.gu.identity.formstackbatonrequests.updatedynamo

import java.time.{LocalDate, LocalDateTime}
import com.amazonaws.services.dynamodbv2.model.UpdateTableResult
import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Pending, UpdateDynamoRequest, UpdateDynamoResponse}
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, S3Client, SubmissionTableUpdateDate}
import com.gu.identity.formstackbatonrequests.services.{DynamoUpdateService, FormstackRequestService, UpdateStatus}
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, FormstackHandler, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

case class UpdateDynamoHandler(
  dynamoClient: DynamoClient,
  s3Client: S3Client,
  formstackClient: FormstackRequestService,
  config: PerformLambdaConfig
) extends LazyLogging with FormstackHandler[UpdateDynamoRequest, UpdateDynamoResponse]{

  val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(formstackClient, dynamoClient, config)

  def update(token: FormstackAccountToken, formPage: Int, count: Int, timeOfStart: LocalDateTime, context: Context): Either[Throwable, UpdateStatus] = {

    dynamoClient.mostRecentTimestamp(config.lastUpdatedTableName, token.account).flatMap { lastUpdate =>
      val timestampAsDate = LocalDateTime.parse(lastUpdate.date, SubmissionTableUpdateDate.formatter)
      if (timestampAsDate.toLocalDate != LocalDate.now) {
        logger.info(s"Updating Dynamo table with requests since $lastUpdate.")
        for {
          status <- dynamoUpdateService.updateSubmissionsTable(formPage, lastUpdate, count, token, context)
          _ <- if (status.completed) {
            dynamoClient.updateMostRecentTimestamp(config.lastUpdatedTableName, token.account, timeOfStart)
          } else Right(())
        } yield status
      } else Right(UpdateStatus(completed = true, None, None, token))
    }
  }

  override def handle(request: UpdateDynamoRequest, context: Context): Either[Throwable, UpdateDynamoResponse] = {
    val token = request.accountNumber match {
      case Some(account) if account == 1 => config.accountOneToken
      case Some(account) => throw new RuntimeException(s"Unexpected account number: $account")
      case None => throw new RuntimeException(s"Unable to retrieve account number from UpdateDynamoRequest")
    }

    update(token, request.formPage, request.count, request.timeOfStart, context) match {
      case Right(res) =>
        val status = if (res.completed) Completed else Pending
        Right(UpdateDynamoResponse(
          status,
          request.initiationReference,
          request.subjectEmail,
          request.dataProvider,
          res.token.account,
          res.formsPage,
          res.count,
          request.requestType,
          request.timeOfStart))
      case Left(err) =>
        s3Client.writeFailedResults(request.initiationReference, err.getMessage, request.requestType, config)
            .flatMap(_ => Left(err))
    }
  }
}
