package com.gu.identity.eventbriteconsents.services

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.gu.identity.eventbriteconsents.clients.{EventbriteClient, EventbriteCredentials, IdentityClient}
import com.gu.identity.eventbriteconsents.config.LambdaConfig
import com.typesafe.scalalogging.LazyLogging

class ConsentsService(config: LambdaConfig, eventbriteClient: EventbriteClient, identitiyClient: IdentityClient) extends LazyLogging {

  def syncConsents(): Unit = {
    val lastRun = Instant.now().minus(config.syncFrequencyHours, ChronoUnit.HOURS)
    val emailAddresses = findConsentEmails(lastRun, config.masterclassesCredentials) ++ findConsentEmails(lastRun, config.eventsCredentials)
    logger.info(s"Email addresses to sync $emailAddresses, total ${emailAddresses.size}, last run $lastRun")
    for {
      email <- emailAddresses
    } yield if (config.isDebug) {
        logger.info(s"Debug flag is set, not syncing $email")
    } else {
      identitiyClient.updateEventConsent(email)
      logger.info(s"Synced $email")
    }
  }

  @scala.annotation.tailrec
  private def findConsentEmails(
    lastRun: Instant,
    eventbriteCredentials: EventbriteCredentials,
    continuation: String = "",
    accumulator: Set[String] = Set.empty
  ): Set[String] = {

    val eventbriteResponse = eventbriteClient.findConsents(eventbriteCredentials, lastRun, continuation)

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
        findConsentEmails(lastRun, eventbriteCredentials, continue, accumulated)
      case _ =>
        accumulated.map(_.toLowerCase)
    }
  }
}