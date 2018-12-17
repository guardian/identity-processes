package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import io.circe.{Decoder, Encoder, Json, ParsingFailure}

import scala.collection.JavaConverters._
import io.circe.parser._
import io.circe.generic.semiauto._


object Lambda extends StrictLogging {

  case class IdentityBrazeEmailData(externalId: String, emailAddress: String, templateId: String, customFields: Map[String, String])

  def handler(event: SQSEvent, context: Context): Unit = {

    logger.info(s"context :  $context")
    logger.info(s"event :  $event")

    parseMessage(event).fold(error => {
      logger.info(s"problem parsing, failed with errors ${error.map(_.message)}")
    }, r => {
      logger.info(s"success with $r")
    })
  }

  private def parseMessage(sqsEvent: SQSEvent): Either[List[io.circe.DecodingFailure], List[IdentityBrazeEmailData]] = {

    implicit val identityBrazeEmailDataDecoder: Decoder[IdentityBrazeEmailData] = deriveDecoder[IdentityBrazeEmailData]
    implicit val identityBrazeEmailDataEncoder: Encoder[IdentityBrazeEmailData] = deriveEncoder[IdentityBrazeEmailData]

    val messages = sqsEvent.getRecords.asScala.map(mes => mes).toList

    val parseMessageJson: List[Either[ParsingFailure, Json]] = messages.map(mes => {
      logger.info(s"attempting to parse message body : ${mes.getBody}")
      parseJson(mes.getBody)
    })

    val successfullyParsedJson = parseMessageJson.collect{ case Right(v) => v}

    val jsonToEmailData  = successfullyParsedJson.map(json => json.as[IdentityBrazeEmailData])

    val emailData = jsonToEmailData.collect{ case Right(v) => v}
    val decodeFailures = jsonToEmailData.collect{ case Left(failure) => failure }

    decodeFailures match {
      case Nil => Right(emailData)
      case _ => Left(decodeFailures)
    }
  }

  def parseJson(messageBody: String): Either[io.circe.ParsingFailure, Json] = parse(messageBody) match {
    case Left(failure) => {
      logger.info(s"invalid json in message : $messageBody failed with error :  ${failure.message}")
      Left(failure)
    }
    case Right(json) =>
      logger.info(s"succesfuly parsed json:  $json")
      Right(json)
  }
}
