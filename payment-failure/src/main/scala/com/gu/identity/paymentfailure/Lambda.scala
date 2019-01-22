package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import cats.syntax.either._

object Lambda extends StrictLogging {

  def getEnvironmentVariable(env: String): Either[Throwable, String] =
    Either.fromOption(
      Option(System.getenv(env)),
      new Exception(s"environment variable $env not defined")
    )

  def getConfig: Either[Throwable, Config] = {
    for {
      idapiHost <- getEnvironmentVariable("idapiHost")
      brazeApiHost <- getEnvironmentVariable("brazeApiHost")
      idapiAccessToken <- getEnvironmentVariable("idapiAccessToken")
      sqsQueueUrl <- getEnvironmentVariable("sqsQueueUrl")
      brazeApiKey <- getEnvironmentVariable("brazeApiKey")
    } yield {
      Config(idapiHost, brazeApiHost, idapiAccessToken, sqsQueueUrl, brazeApiKey)
    }
  }

  def handler(event: SQSEvent, context: Context): Unit = {

    LambdaService.setAWSRequestId(context)

    logger.info(s"received ${event.getRecords.size} messages - context: $context - event: $event")

    logger.info("initialising config and lambda service")
    val config = getConfig.valueOr { err =>
      logger.error("unable to get config", err)
      throw err
    }

    val lambdaService = LambdaService.fromConfig(config)

    logger.info("config and services successfully initialised - processing events")
    lambdaService.processEvent(event).foreach {
      case Left(err) =>
        logger.error(s"unable to process event $event", err)
        throw err
      case _ => logger.info("process successful")
    }
  }
}
