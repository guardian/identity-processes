package com.gu.identity.formstackbatonrequests.updatedynamo

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, Pending, UpdateDynamoRequest, UpdateDynamoResponse}
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, S3Client}
import com.gu.identity.formstackbatonrequests.services.{DynamoUpdateService, FormstackRequestService, UpdateStatus}
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, FormstackHandler, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

import java.time.{LocalDateTime, ZoneOffset}

object UpdateDynamoHandler {
  def apply(
             dynamoClient: DynamoClient,
             s3Client: S3Client,
             formstackClient: FormstackRequestService,
             config: PerformLambdaConfig
           ): UpdateDynamoHandler = {
    val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(formstackClient, dynamoClient, config)

    UpdateDynamoHandler(
      dynamoClient = dynamoClient,
      s3Client = s3Client,
      dynamoUpdateService = dynamoUpdateService,
      config = config,
      getCurrentTimestamp = { () => LocalDateTime.now(ZoneOffset.UTC) }
    )
  }
}

case class UpdateDynamoHandler(
  dynamoClient: DynamoClient,
  s3Client: S3Client,
  dynamoUpdateService: DynamoUpdateService,
  config: PerformLambdaConfig,
  getCurrentTimestamp: () => LocalDateTime
) extends LazyLogging with FormstackHandler[UpdateDynamoRequest, UpdateDynamoResponse]{

  def update(token: FormstackAccountToken, formPage: Int, count: Int, timeOfStart: LocalDateTime, maxUpdateSeconds: Option[Int], context: Context): Either[Throwable, UpdateStatus] = {

    dynamoClient.mostRecentTimestamp(config.lastUpdatedTableName, token.account).flatMap { lastUpdate =>
      val lastUpdateTimestamp = lastUpdate.asLocalTimestamp
      if (lastUpdateTimestamp.toLocalDate != getCurrentTimestamp().toLocalDate) {

        val maybeMaxUpdateTime: Option[LocalDateTime] = maxUpdateSeconds.flatMap { maxSecs =>
          val maxTimestamp = lastUpdateTimestamp.plusSeconds(maxSecs)
          if (maxTimestamp.isBefore(timeOfStart)) {
            Some(maxTimestamp)
          }
          else {
            logger.info(s"ignoring maxUpdateSeconds as last update timestamp (${lastUpdate.date}) + maxUpdateSeconds ($maxSecs) is  $maxTimestamp which is after the time of execution start $timeOfStart")
            None
          }
        }

        val withMaxLogMsg = maybeMaxUpdateTime.map(d=> s"Updating Dynamo table with requests between ${lastUpdate.date} and ${d}")

        logger.info(withMaxLogMsg.getOrElse(s"Updating Dynamo table with requests since $lastUpdate"))

        for {
          status <- dynamoUpdateService.updateSubmissionsTable(formPage, lastUpdateTimestamp,  maybeMaxUpdateTime,  count, token, context)
          _ <- if (status.completed) {
            val newLastUpdatedDate = maybeMaxUpdateTime.getOrElse(timeOfStart)
            dynamoClient.updateMostRecentTimestamp(config.lastUpdatedTableName, token.account, newLastUpdatedDate)
          } else Right(())
        } yield status
      } else {
        logger.info("skipping dynamodb update as it was already updated today")
        Right(UpdateStatus(completed = true, None, None, token))
      }
    }
  }

  override def handle(request: UpdateDynamoRequest, context: Context): Either[Throwable, UpdateDynamoResponse] = {
    val token = request.accountNumber match {
      case Some(account) if account == 1 => config.accountOneToken
      case Some(account) => throw new RuntimeException(s"Unexpected account number: $account")
      case None => throw new RuntimeException(s"Unable to retrieve account number from UpdateDynamoRequest")
    }
    val tenSecondsFromNow = getCurrentTimestamp().plusSeconds(10L)
    //the timestamp of when the step function was triggered should not be in the future
    if (request.timeOfStart.isAfter(tenSecondsFromNow)) {
      throw new RuntimeException(s"Invalid timeOfStart: ${request.timeOfStart} is in the future")
    }

    update(token, request.formPage, request.count, request.timeOfStart, request.maxUpdateSeconds, context) match {
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
