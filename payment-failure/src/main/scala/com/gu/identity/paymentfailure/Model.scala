package com.gu.identity.paymentfailure

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class Config(idapiHost: String, brazeApiHost: String, idapiAccessToken: String, queueURL: String, brazeApiKey:String)

case class IdentityBrazeEmailData(externalId: String, emailAddress: String, templateId: String, customFields: Map[String, String])

object IdentityBrazeEmailData {
  implicit val identityBrazeEmailDataDecoder: Decoder[IdentityBrazeEmailData] = deriveDecoder[IdentityBrazeEmailData]
}

case class BrazeRecipient(external_user_id: String, trigger_properties: Option[Map[String, String]])

object BrazeRecipient {
  implicit val brazeRecipientEncoder: Encoder[BrazeRecipient] = deriveEncoder[BrazeRecipient]
}

case class BrazeSendRequest(api_key: String,
                            campaign_id: String,
                            trigger_properties: Option[Map[String, String]],
                            recipients: Seq[BrazeRecipient])

object BrazeSendRequest {
  implicit val BrazeSendRequestEncoder: Encoder[BrazeSendRequest] = deriveEncoder[BrazeSendRequest]
  def apply(userId: String, apiKey: String, campaignId: String, customProperties: Map[String, String]): BrazeSendRequest = {
    BrazeSendRequest(apiKey, campaignId, None, List(BrazeRecipient(userId, Some(customProperties))))
  }
}

sealed trait BrazeResponse {
  def message: String
  // queued is only returned when Braze is performing maintenance
  def isSuccess: Boolean = message == "success" || message == "queued"
}

case class BrazeUnitResponse(message: String) extends BrazeResponse

object BrazeUnitResponse {
  implicit val BrazeUnitResponseDecoder: Decoder[BrazeUnitResponse] = deriveDecoder[BrazeUnitResponse]
}
