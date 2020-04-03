package com.gu.identity.eventbriteconsents.clients

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import com.gu.identity.eventbriteconsents.models.EventbriteResponse
import io.circe.generic.auto._
import io.circe.parser._
import scalaj.http.Http

class EventbriteClient {
  def findConsents(token: String, lastRun: Instant, continuationToken: String = ""): EventbriteResponse = {
    val formattedLastRun = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
      .withZone(ZoneId.of("UTC"))
      .format(lastRun)

    val response = Http("https://www.eventbriteapi.com/v3/users/me/owned_event_attendees/")
      .headers(
        "Authorization" -> s"Bearer $token",
        "Accept" -> "application/json"
      )
      .params(
        "status" -> "attending",
        "changed_since" -> formattedLastRun,
        "continuation" -> continuationToken
      )
      .asString


    if (response.code == 200) {
      parse(response.body).flatMap(_.as[EventbriteResponse]) match {
        case Right(response) => response
        case Left(error) => throw new RuntimeException(s"unable to deserialise ${response.body}", error)
      }
    } else {
      throw new RuntimeException(s"unexpected response from eventbrite ${response.code} ${response.body}")
    }
  }
}
