package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import scala.collection.JavaConverters._

object Lambda extends StrictLogging {

  def getConfig: Either[Throwable, Config] = {
    val optionConfig = for {
      idapiHost <- Option(System.getenv("idapiHost"))
      brazeApiHost <- Option(System.getenv("brazeApiHost"))
      idapiAccessToken <- Option(System.getenv("idapiAccessToken"))
      sqsQueueUrl <- Option(System.getenv("sqsQueueUrl"))
    } yield {
      Config(idapiHost, brazeApiHost, idapiAccessToken, sqsQueueUrl)
    }

    optionConfig match {
      case Some(config) => Right(config)
      case _ => Left(new Exception(s"Missing or incorrect config. Please check environment variables"))
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

    val identityClient = new IdentityClient
    val sqsService = new SqsService
    val brazeClient = new BrazeClient
    val sendEmailService = new SendEmailService(identityClient, brazeClient)

    messages.map( mes => {
      for {
        config <- getConfig
        emailData <- sqsService.parseSingleMessage(mes)
        brazeResponse <- sendEmailService.sendEmail(emailData, config)
        result <- sqsService.deleteMessage(mes,config)
        _ <- sqsService.processDeleteMessageResult(result)
      } yield brazeResponse
    })
  }
}
