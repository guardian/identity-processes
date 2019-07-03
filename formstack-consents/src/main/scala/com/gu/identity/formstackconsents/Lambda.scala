package com.gu.identity.formstackconsents

import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.typesafe.scalalogging.StrictLogging
import io.circe.{HCursor, Json}
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec, JsonKey}
import io.circe.parser._
import com.gu.identity.formstackconsents.FormstackSubmissionDecoder.FormstackSubmission

object Lambda extends StrictLogging {

  case class Config(idapiHost: String, idapiAccessToken: String, formstackSharedSecret: String)

  def getEnvironmentVariable(env: String): Option[String] =
    Option(System.getenv(env))

  def getConfig: Option[Config] = {
    for {
      idapiHost <- getEnvironmentVariable("idapiHost")
      idapiAccessToken <- getEnvironmentVariable("idapiAccessToken")
      formstackSharedSecret <- getEnvironmentVariable("formstackSharedSecret")
    } yield Config(idapiHost, idapiAccessToken, formstackSharedSecret)
  }

  def verifySecretKey(formstackSharedSecret: String, secretKeyFromRequest: String): Option[Boolean] = {
    val isValid = formstackSharedSecret == secretKeyFromRequest
    if (isValid) { Some(true) } else { None }
  }

  def decodeFormstackSubmission(eventBody: String): Option[FormstackSubmission] = {
    FormstackSubmissionDecoder.decodeFormstackSubmission(eventBody) match {
      case Some(submission) =>
        logger.info(s"Successfully decoded formstack submission. FormId: ${submission.formId}, Email: ${submission.emailAddress}")
        Some(submission)
      case None =>
        logger.error("Unable to decode formstack submission")
        None
    }
  }

  def handler(event: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent = {
    logger.info(event.getBody)
    (for {
      formstackSubmission <- decodeFormstackSubmission(event.getBody)
      config <- getConfig
      _ <- verifySecretKey(config.formstackSharedSecret, formstackSubmission.handshakeKey)
      identityClient = new IdentityClient(config)
      response <- identityClient.sendConsentToIdentity(formstackSubmission)
    } yield response).getOrElse{
      val invalidResponse = new APIGatewayProxyResponseEvent
      invalidResponse.withStatusCode(404)
    }
  }
}

object FormstackSubmissionDecoder {

 case class FormstackSubmission(formId: String, emailAddress: String, handshakeKey: String)

  private def getEmailField(body: HCursor): Option[String] = {
    body.get[String]("email_address").toOption match {
      case Some(email) => Some(email)
      case None => body.get[String]("your_email_address").toOption
    }
  }

  def decodeFormstackSubmission(body: String): Option[FormstackSubmission] = {
    val jObj: Json = parse(body).getOrElse(Json.Null)
    val cursor: HCursor =  jObj.hcursor
    for {
      formId <- cursor.get[String]("FormID").toOption
      email <- getEmailField(cursor)
      handshakeKey <- cursor.get[String]("HandshakeKey").toOption
    } yield FormstackSubmission(formId, email, handshakeKey)
  }
}
