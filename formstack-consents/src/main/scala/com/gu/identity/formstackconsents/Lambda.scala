package com.gu.identity.formstackconsents

import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec, JsonKey}
import io.circe.parser.decode

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

  implicit val circeConfig: Configuration = Configuration.default
  // this will change depending on form
  @ConfiguredJsonCodec case class FormstackSubmission(@JsonKey("FormID") formId: String, @JsonKey("email_address") emailAddress: String, @JsonKey("HandshakeKey") handshakeKey: String)

  def decodeFormstackSubmission(eventBody: String): Option[FormstackSubmission] = {
    decode[FormstackSubmission](eventBody).toOption match {
      case Some(submission) =>
        logger.info(s"Successfully decoded formstack submission: $submission")
        Some(submission)
      case None =>
        logger.error("Unable to decode formstack submission")
        None
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
      invalidResponse.withStatusCode(404)
    }
  }
}

