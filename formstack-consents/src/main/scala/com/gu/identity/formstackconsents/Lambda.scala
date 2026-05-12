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
    if (isValid) { 
      logger.info(s"verifySecretKey: isValid is true")
      Some(true) 
    } else { 
      logger.info(s"verifySecretKey: isValid is false")
      None 
    }
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

case class FormstackSubmission(
  formId: String,
  emailAddress: String,
  handshakeKey: String,
  opt_in: Option[Boolean] = None // if required, request the formstack checkbox hidden label be "opt_in"
)

object FormstackSubmission {

  private def getEmailField(cursor: HCursor): Decoder.Result[String] = {
    cursor.downField("email_address").as[String]
      .orElse(cursor.downField("your_email_address").as[String])
        .orElse(cursor.downField("email").as[String])
  }

  // Form submissions of type MarketingConsent require the user to 'opt in' to consents, not just submit.
  // While we can conditionally trigger lambda webhook within Formstack, these settings are not exclusively
  // under our control so we perform an additional checks in code to ensure we are never collecting consent and user data inappropriately.
  // Any forms with required opt ins should be added in Newsletter.scala. Request the formstack checkbox hidden label be "opt_in"
  private def getConsentOptInField(cursor: HCursor): Option[Boolean] = {
    val field = cursor.downField("opt_in").as[String]
      .orElse(cursor.downField("supporter_consent_opt_in").as[String])

    field match {
      case Right(_) => Some(true) // assumes single opt in checkbox so value is irrelevant
      case Left(error) if error.message == "Attempt to decode value on failed cursor" => None // field does not exist b/c no opt in required eg. newsletters
      case Left(_) => Some(false) // field is null
      }
  }

  implicit val formstackDecoder: Decoder[FormstackSubmission] = new Decoder[FormstackSubmission] {
    final def apply(cursor: HCursor): Decoder.Result[FormstackSubmission] =
      for {
        formId <- cursor.downField("FormID").as[String]
        email <- getEmailField(cursor)
        handshakeKey <- cursor.downField("HandshakeKey").as[String]
        opt_in = getConsentOptInField(cursor)
      } yield FormstackSubmission(formId, email, handshakeKey, opt_in)
  }
}
