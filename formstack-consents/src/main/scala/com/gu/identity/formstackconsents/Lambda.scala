package com.gu.identity.formstackconsents

import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.typesafe.scalalogging.StrictLogging
import cats.implicits._
import io.circe.{Decoder, HCursor}
import io.circe.parser._

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
    decode[FormstackSubmission](eventBody) match {
      case Left(error) =>
        logger.error(s"Unable to decode formstack submission: $error")
        None
      case Right(submission) =>
        logger.info(s"Successfully decoded formstack submission. FormId: ${submission.formId}, Email: ${submission.emailAddress}")
        Some(submission)
    }
  }

  def handler(event: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent = {
    (for {
      formstackSubmission <- decodeFormstackSubmission(event.getBody)
      config <- getConfig
      _ <- verifySecretKey(config.formstackSharedSecret, formstackSubmission.handshakeKey)
      identityClient = new IdentityClient(config)
      response <- identityClient.sendConsentToIdentity(formstackSubmission)
    } yield response).getOrElse{
      val invalidResponse = new APIGatewayProxyResponseEvent
      logger.error("lambda unsuccessful")
      invalidResponse.withStatusCode(500)
    }
  }
}

case class FormstackSubmission(formId: String, emailAddress: String, handshakeKey: String)

object FormstackSubmission {

  private def getEmailField(cursor: HCursor): Decoder.Result[String] = {
    cursor.downField("email_address").as[String]
      .orElse(cursor.downField("your_email_address").as[String])
  }

  implicit val formstackDecoder: Decoder[FormstackSubmission] = new Decoder[FormstackSubmission] {
    final def apply(cursor: HCursor): Decoder.Result[FormstackSubmission] =
      for {
        formId <- cursor.downField("FormID").as[String]
        email <- getEmailField(cursor)
        handshakeKey <-  cursor.downField("HandshakeKey").as[String]
      } yield FormstackSubmission(formId, email, handshakeKey)
  }
}


