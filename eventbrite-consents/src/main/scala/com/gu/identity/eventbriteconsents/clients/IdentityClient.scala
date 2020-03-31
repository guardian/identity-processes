package com.gu.identity.eventbriteconsents.clients

import io.circe.generic.auto._
import io.circe.syntax._
import scalaj.http.Http

case class IdapiConsentUpdate(email: String, `set-consents`: Vector[String])

class IdentityClient(idapiUrl: String, idapiAccessToken: String) {
  def updateEventConsent(emailAddress: String): Unit = {
    val result = Http(s"$idapiUrl/consent-email")
      .headers(
        "X-GU-ID-Client-Access-Token" -> s"Bearer $idapiAccessToken",
        "Content-type" -> "application/json"
      )
      .postData(IdapiConsentUpdate(emailAddress, Vector("events")).asJson.noSpaces)
      .asString

    if (result.code != 200) throw new RuntimeException(s"Unexpected response from idapi ${result.code} ${result.body}")
  }
}