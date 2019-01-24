package com.gu.identity.paymentfailure

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class Config(idapiHost: String, brazeApiHost: String, idapiAccessToken: String, queueURL: String, brazeApiKey: String)

case class IdentityBrazeEmailData(externalId: String, emailAddress: String, templateId: String, customFields: Map[String, String])

object IdentityBrazeEmailData {
  implicit val identityBrazeEmailDataDecoder: Decoder[IdentityBrazeEmailData] = deriveDecoder[IdentityBrazeEmailData]
}

case class BrazeRecipient(external_user_id: String, trigger_properties: Map[String, String])

object BrazeRecipient {
  implicit val brazeRecipientEncoder: Encoder[BrazeRecipient] = deriveEncoder[BrazeRecipient]
}

case class BrazeSendRequest(api_key: String, campaign_id: String, recipients: Seq[BrazeRecipient])

object BrazeSendRequest {

  implicit val BrazeSendRequestEncoder: Encoder[BrazeSendRequest] = deriveEncoder[BrazeSendRequest]
}

case class BrazeResponse(message: String)

object BrazeResponse {
  implicit val BrazeUnitResponseDecoder: Decoder[BrazeResponse] = deriveDecoder[BrazeResponse]
}
