package com.gu.identity.eventbriteconsents.services

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.gu.identity.eventbriteconsents.clients.{EventbriteClient, IdapiClient}
import com.gu.identity.eventbriteconsents.config.LambdaConfig
import com.typesafe.scalalogging.LazyLogging

class ConsentsService(config: LambdaConfig, eventbriteClient: EventbriteClient, identitiyClient: IdapiClient) extends LazyLogging {

  def syncConsents(): Unit = {
    val lastRun = Instant.now().minus(config.syncFrequencyHours, ChronoUnit.HOURS)
    val emailAddresses = findConsentEmails(lastRun, config.masterclassesToken) ++ findConsentEmails(lastRun, config.eventsToken)
    for {
      email <- emailAddresses
    } yield {
      identitiyClient.updateEventConsent(email)
      logger.info(s"Synced $email")
    }
  }


  @scala.annotation.tailrec
  private def findConsentEmails(lastRun: Instant, token: String, continuation: String = "", accumulator: Set[String] = Set.empty): Set[String] = {
    val eventbriteResponse = eventbriteClient.findConsents(token, lastRun, continuation)

    val emailAddresses = for {
      attendee <- eventbriteResponse.attendeesList
      answer <- attendee.answersList if answer.isEventsAndMasterClassesAnswer
      profile <- attendee.profile
      email <- profile.email
    } yield email

    val accumulated = accumulator ++ emailAddresses.toSet

    eventbriteResponse.pagination.continuationToken match {
      case Some(continue) =>
        // Sleep to avoid rate limiting
        Thread.sleep(250)
        findConsentEmails(lastRun, token, continue, accumulated)
      case _ =>
        accumulated
    }
  }
}