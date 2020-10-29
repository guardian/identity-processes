package com.gu.identity.eventbriteconsents.clients

import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import scalaj.http.Http

case class IdapiConsentUpdate(email: String, `set-consents`: Vector[String])
case class IdapiErrorResponse(status: String, errors: List[IdapiError])
case class IdapiError(message: String, description: String)

class IdentityClient(idapiUrl: String, idapiAccessToken: String) extends LazyLogging {
  def updateEventConsent(emailAddress: String): Unit = {
    val result = Http(s"$idapiUrl/consent-email")
      .headers(
        "X-GU-ID-Client-Access-Token" -> s"Bearer $idapiAccessToken",
        "Content-type" -> "application/json"
      )
      .postData(IdapiConsentUpdate(emailAddress, Vector("events")).asJson.noSpaces)
      .asString

    if (result.code != 200) {
      handleErrorResponse(result.code, result.body, emailAddress)
    }
  }

  def handleErrorResponse(code: Int, body: String, emailAddress: String): Unit = {
    parse(body).flatMap(_.as[IdapiErrorResponse]) match {
      case Right(errorResponse: IdapiErrorResponse) => {
        for {
          error <- errorResponse.errors
        } yield if (error.description.contains("Bad email format")) {
          logger.error("Invalid email address, could not process consents: " + emailAddress)
        } else {
          throwRuntimeException
        }
      }
      case _ => throwRuntimeException
    }

    def throwRuntimeException: Nothing = {
      throw new RuntimeException(s"Unexpected response from idapi when syncing email $emailAddress $code $body")
    }
  }
}