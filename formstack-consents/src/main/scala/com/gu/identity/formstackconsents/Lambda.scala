package com.gu.identity.formstackconsents

import com.gu.identity.globalConfig.DevConfig
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec, JsonKey}
import io.circe.parser.decode

object Lambda extends App {

  implicit val circeConfig: Configuration = Configuration.default
  // this will change depending on form
  @ConfiguredJsonCodec case class FormstackSubmission(@JsonKey("FormID") formId: String, @JsonKey("email_address") emailAddress: String)

  def decodeFormstackSubmission(eventBody: String): Option[FormstackSubmission] = {
    decode[FormstackSubmission](eventBody).toOption
  }

  def handler(event: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent = {
    val config = new DevConfig
    val identityClient = new IdentityClient(config)
//    Uncomment when ready to test
    (for {
     formstackSubmission <- decodeFormstackSubmission(event.getBody)
     response <- identityClient.sendConsentToIdentity(formstackSubmission)
   } yield response).getOrElse{
      val invalidResponse = new APIGatewayProxyResponseEvent
      invalidResponse.withStatusCode(404)
    }
  }
}

