package com.gu.identity.paymentfailure

object Model {

  case class IdentityBrazeEmailData(externalId: String, emailAddress: String, templateId: String, customFields: Map[String, String])

  case class BrazeResponse(msg: String)

  case class Config(idapiHost: String, brazeApiHost: String, idapiAccessToken: String)

}
