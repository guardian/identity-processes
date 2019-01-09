package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import cats.syntax.either._

object Lambda extends StrictLogging {

  def getEnvironmentVariable(env: String): Either[Throwable, String] = {
    Option(System.getenv(env)) match {
      case Some(variable) => Right(variable)
      case _ => Left(new Exception(s"Missing or incorrect config. Please check environment variables"))
    }
  }

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

    logger.info(s"context :  $context")
    logger.info(s"event :  $event")
    logger.info(s"received message batch of size: ${event.getRecords.size}")

    logger.info("initialising config and lambda service")
    val config =  getConfig.valueOr(throw _)
    val lambdaService = LambdaService.fromConfig(config)

    logger.info("config and services successfully initialised - processing events")
    lambdaService.processEvent(event).foreach {
      case Left(throwable) => throw throwable
      case _ => logger.info("process successful")
    }
  }
}
