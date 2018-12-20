package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import scala.collection.JavaConverters._

object Lambda extends StrictLogging {

  def configFromEnvVariables: Option[Config] = {
    for {
      idapiHost <- Option(System.getenv("idapiHost"))
      brazeApiHost <- Option(System.getenv("brazeApiHost"))
      idapiAccessToken <- Option(System.getenv("idapiAccessToken"))
    } yield {
      Config(idapiHost, brazeApiHost, idapiAccessToken)
    }
  }

  def handler(event: SQSEvent, context: Context): Unit = {

    logger.info(s"context :  $context")
    logger.info(s"event :  $event")

    val config = configFromEnvVariables

    // do a fold?
    if(config.isDefined) {
      process(event, config.get).fold(err => throw err, _ => logger.info("process successful"))
    } else {
      logger.error("Missing or incorrect config. Please check environment variables")
      System.exit(1)
    }
  }

  def process(event: SQSEvent, config: Config): Either[Throwable, BrazeResponse]= {
    val messages = event.getRecords.asScala.map(mes => mes).toList

    val identityClient = new IdentityClient
    val sqsParsingService = new SqsParsingService
    val brazeClient = new BrazeClient
    val sendEmailService = new SendEmailService(identityClient,brazeClient)

    messages.map( mes => {
      for {
        emailData <- sqsParsingService.parseSingleMessage(mes)
        brazeResponse <- sendEmailService.sendEmail(emailData, config)
      } yield brazeResponse
    }).head
  }
}
