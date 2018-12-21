package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import scala.collection.JavaConverters._

object Lambda extends StrictLogging {

  //GET CONFIG = EIther[Error, Config]
  def configFromEnvVariables: Option[Config] = {
    for {
      idapiHost <- Option(System.getenv("idapiHost"))
      brazeApiHost <- Option(System.getenv("brazeApiHost"))
      idapiAccessToken <- Option(System.getenv("idapiAccessToken"))
      sqsQueueUrl <- Option(System.getenv("sqsQueueUrl"))
    } yield {
      Config(idapiHost, brazeApiHost, idapiAccessToken, sqsQueueUrl)
    }
  }

  def handler(event: SQSEvent, context: Context): Unit = {

    logger.info(s"context :  $context")
    logger.info(s"event :  $event")
    logger.info(s"received message batch of size: ${event.getRecords.size()}")

    val config = configFromEnvVariables

    config.fold({
      logger.error("Missing or incorrect config. Please check environment variables")
      System.exit(1)
    }) { configuration =>
      process(event, configuration).foreach{
        case Left(throwable) => throw throwable
        case _ => logger.info("process successful")
      }
    }
  }

  def process(event: SQSEvent, config: Config): List[Either[Throwable, BrazeResponse]]= {
    val messages = event.getRecords.asScala.map(mes => mes).toList

    val identityClient = new IdentityClient
    val sqsService = new SqsService
    val brazeClient = new BrazeClient
    val sendEmailService = new SendEmailService(identityClient,brazeClient)

    messages.map( mes => {
      for {
        emailData <- sqsService.parseSingleMessage(mes)
        brazeResponse <- sendEmailService.sendEmail(emailData, config)
        result <- sqsService.deleteMessage(mes,config)
        _ <- sqsService.processDeleteMessageResult(result)
      } yield brazeResponse
    })
  }
}
