package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import scala.collection.JavaConverters._

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
    } yield {
      Config(idapiHost, brazeApiHost, idapiAccessToken, sqsQueueUrl)
    }
  }

  def handler(event: SQSEvent, context: Context): Unit = {

    logger.info(s"context :  $context")
    logger.info(s"event :  $event")
    logger.info(s"received message batch of size: ${event.getRecords.size}")

    process(event).foreach {
      case Left(throwable) => throw throwable
      case _ => logger.info("process successful")
    }
  }

  def process(event: SQSEvent): List[Either[Throwable, BrazeResponse]]= {
    val messages = event.getRecords.asScala.toList

    messages.map( mes => {
      for {
        config <- getConfig

        identityClient = new IdentityClient(config)
        sqsService = new SqsService(config)
        brazeClient = new BrazeClient
        sendEmailService = new SendEmailService(identityClient, brazeClient)

        emailData <- sqsService.parseSingleMessage(mes)
        brazeResponse <- sendEmailService.sendEmail(emailData, config)
        result <- sqsService.deleteMessage(mes)
        _ <- sqsService.processDeleteMessageResult(result)
      } yield brazeResponse
    })
  }
}
