package com.gu.identity.paymentfailure

import enumeratum.{CirceEnum, EnumEntry}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import scala.collection.immutable

case class Config(idapiHost: String, brazeApiHost: String, idapiAccessToken: String, queueURL: String, brazeApiKey: String)

sealed trait BrazeExternalIdType extends EnumEntry

object BrazeExternalIdType extends enumeratum.Enum[BrazeExternalIdType] with CirceEnum[BrazeExternalIdType] {

  override val values: immutable.IndexedSeq[BrazeExternalIdType] = findValues

  case object SalesforceId extends BrazeExternalIdType
  case object IdentityId extends BrazeExternalIdType
  case object BrazeUuid extends BrazeExternalIdType
}

case class BrazeExternalId(value: String, externalIdType: BrazeExternalIdType)

object BrazeExternalId {

  implicit val brazeExternalIdDecoder: Decoder[BrazeExternalId] = deriveDecoder[BrazeExternalId]

  def fromSalesforceId(salesforceId: String): BrazeExternalId =
    BrazeExternalId(salesforceId, externalIdType = BrazeExternalIdType.SalesforceId)

  def fromIdentityId(identityId: String): BrazeExternalId =
    BrazeExternalId(identityId, externalIdType = BrazeExternalIdType.IdentityId)

  def fromBrazeUuid(brazeUuid: String): BrazeExternalId =
    BrazeExternalId(brazeUuid, externalIdType = BrazeExternalIdType.BrazeUuid)
}

case class IdentityBrazeEmailData(externalId: BrazeExternalId, emailAddress: String, templateId: String, customFields: Map[String, String])

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
