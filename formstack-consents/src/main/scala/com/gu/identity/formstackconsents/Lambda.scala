package com.gu.identity.formstackconsents

import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec, JsonKey}
import io.circe.parser.decode

object Lambda extends App {

  case class Config(idapiHost: String, idapiAccessToken: String)

  def getEnvironmentVariable(env: String): Option[String] =
    Option(System.getenv(env))

  def getConfig: Option[Config] = {
    for {
      idapiHost <- getEnvironmentVariable("idapiHost")
      idapiAccessToken <- getEnvironmentVariable("idapiAccessToken")
    } yield Config(idapiHost, idapiAccessToken)
  }

  implicit val circeConfig: Configuration = Configuration.default
  // this will change depending on form
  @ConfiguredJsonCodec case class FormstackSubmission(@JsonKey("FormID") formId: String, @JsonKey("email_address") emailAddress: String)

  def decodeFormstackSubmission(eventBody: String): Option[FormstackSubmission] = {
    decode[FormstackSubmission](eventBody).toOption
  }

  def handler(event: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent = {
//    Uncomment when ready to test
    (for {
      config <- getConfig
      identityClient = new IdentityClient(config)
      formstackSubmission <- decodeFormstackSubmission(event.getBody)
      response <- identityClient.sendConsentToIdentity(formstackSubmission)
    } yield response).getOrElse{
      val invalidResponse = new APIGatewayProxyResponseEvent
      invalidResponse.withStatusCode(404)
    }
  }
}

