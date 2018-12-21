package com.gu.identity.paymentfailure

import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder

case class IdentityBrazeEmailData(externalId: String, emailAddress: String, templateId: String, customFields: Map[String, String])

object IdentityBrazeEmailData {
  implicit val identityBrazeEmailDataDecoder: Decoder[IdentityBrazeEmailData] = deriveDecoder[IdentityBrazeEmailData]
}

case class BrazeResponse(msg: String)

case class Config(idapiHost: String, brazeApiHost: String, idapiAccessToken: String, queueURL: String)

