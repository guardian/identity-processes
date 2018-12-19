package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import scala.collection.JavaConverters._
import com.gu.identity.paymentfailure.Model.BrazeResponse

object Lambda extends StrictLogging {

  case class Config(idapiHost: String, brazeApiHost: String, idapiAccessToken: String)

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

//    val config = configFromEnvVariables

//    if(config.isDefined) {
    if(true) {
      process(event).fold(err => throw err, _ => logger.info("process successful"))
    } else {
      logger.error("Missing or incorrect config. Please check environment variables")
      System.exit(1)
    }
  }

  def process(event: SQSEvent): Either[Throwable, BrazeResponse]= {
    val messages = event.getRecords.asScala.map(mes => mes).toList

    val identityClient = new IdentityClient
    val sqsParsingService = new SqsParsingService
    val brazeClient = new BrazeClient
    val sendEmailService = new SendEmailService(identityClient,brazeClient)

    messages.map( mes => {
      for {
        emailData <- sqsParsingService.parseSingleMessage(mes)
        brazeResponse <- sendEmailService.sendEmail(emailData)
      } yield brazeResponse
    }).head
  }
}
