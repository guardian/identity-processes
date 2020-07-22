package com.gu.identity.eventbriteconsents.services

import java.time.Instant

import com.gu.identity.eventbriteconsents.clients.{EventbriteClient, EventbriteCredentials, IdentityClient}
import com.gu.identity.eventbriteconsents.config.LambdaConfig
import com.gu.identity.eventbriteconsents.models.{EventbriteAnswer, EventbriteAttendee, EventbritePagination, EventbriteProfile, EventbriteResponse}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.Matchers.{eq => eql}

class ConsentsServiceTest extends FlatSpec with MockitoSugar {
  def createFixtures() = new {
    val config = mock[LambdaConfig]
    val eventbriteClient = mock[EventbriteClient]
    val identityClient = mock[IdentityClient]
    val masterclassesCredentials = EventbriteCredentials("masterclassesOrganisationId", "masterclassesToken")
    val eventsCredentials = EventbriteCredentials("eventsOrganisationId", "eventsToken")
    val consentsService = new ConsentsService(config, eventbriteClient, identityClient)

    when(config.eventsCredentials) thenReturn eventsCredentials
    when(config.masterclassesCredentials) thenReturn masterclassesCredentials
    when(config.idapiAccessToken) thenReturn "idapiAccessToken"
    when(config.idapiHost) thenReturn "idapiHost"
    when(config.syncFrequencyHours) thenReturn 2
  }

  def createAttendee(email: String) = EventbriteAttendee(Some(Vector(
    EventbriteAnswer(
      Some("Please tick for something else"),
      Some("Not events and masterclasses")
    ),
    EventbriteAnswer(
      Some("Please tick if you are interested in hearing about these products and services from Guardian News &amp; Media:"),
      Some("Events &amp; Masterclasses: Learn from leading minds at our Guardian live events, including discussions and debates, short courses and bespoke training. Please confirm your selection[s] by clicking the link in \u200Bthe email\u200B that \u200Byou will receive shortly\u200B.")
    ),
  )), Some(EventbriteProfile(Some(email))))

  val notAttending1= EventbriteAttendee(Some(Vector(
    EventbriteAnswer(
      Some("Please tick for something else"),
      Some("Not events and masterclasses")
    ),
  )), Some(EventbriteProfile(Some("notattending@email.com"))))

  val notAttending2= EventbriteAttendee(Some(Vector(
    EventbriteAnswer(
      Some("Please tick if you are interested in hearing about these products and services from Guardian News &amp; Media:"),
      None,
    ),
  )), Some(EventbriteProfile(Some("notattending2@email.com"))))

  "syncConsents" should "update event consents for users who are interested in events and masterclasses using events token and masterclasses token" in {
    val fixtures = createFixtures()
    import fixtures._

    when(config.isDebug) thenReturn false

    when(eventbriteClient.findConsents(eql(masterclassesCredentials), any[Instant], eql(""))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = true, continuation = Some("cont1"), 1, 1), Some(Vector(createAttendee("email1@email.com"), createAttendee("email2@email.com"))))

    when(eventbriteClient.findConsents(eql(masterclassesCredentials), any[Instant], eql("cont1"))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = false, continuation = None, 1, 1), Some(Vector(notAttending2, createAttendee("email3@email.com"))))

    when(eventbriteClient.findConsents(eql(eventsCredentials), any[Instant], eql(""))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = true, continuation = Some("cont2"), 1, 1), Some(Vector(notAttending1, notAttending2)))

    when(eventbriteClient.findConsents(eql(eventsCredentials), any[Instant], eql("cont2"))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = false, continuation = None, 1, 1), Some(Vector(createAttendee("email4@email.com"))))

    consentsService.syncConsents()
    verify(identityClient).updateEventConsent("email1@email.com")
    verify(identityClient).updateEventConsent("email2@email.com")
    verify(identityClient).updateEventConsent("email3@email.com")
    verify(identityClient).updateEventConsent("email4@email.com")
    verifyNoMoreInteractions(identityClient)
  }

  "syncConsents" should "not update consents if no users are interested" in {
    val fixtures = createFixtures()
    import fixtures._

    when(config.isDebug) thenReturn false

    when(eventbriteClient.findConsents(eql(masterclassesCredentials), any[Instant], eql(""))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = true, continuation = Some("cont1"), 1, 1), Some(Vector(notAttending2, notAttending1)))

    when(eventbriteClient.findConsents(eql(masterclassesCredentials), any[Instant], eql("cont1"))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = false, continuation = None, 1, 1), Some(Vector(notAttending2)))

    when(eventbriteClient.findConsents(eql(eventsCredentials), any[Instant], eql(""))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = false, continuation = None, 1, 1), Some(Vector(notAttending1, notAttending2)))


    consentsService.syncConsents()
    verifyNoMoreInteractions(identityClient)
  }



  "syncConsents" should "not update consents when debug flag is set to true" in {
    val fixtures = createFixtures()
    import fixtures._

    when(config.isDebug) thenReturn true

    when(eventbriteClient.findConsents(eql(masterclassesCredentials), any[Instant], eql(""))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = true, continuation = Some("cont1"), 1, 1), Some(Vector(createAttendee("email1@email.com"), createAttendee("email2@email.com"))))

    when(eventbriteClient.findConsents(eql(masterclassesCredentials), any[Instant], eql("cont1"))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = false, continuation = None, 1, 1), Some(Vector(notAttending2, createAttendee("email3@email.com"))))

    when(eventbriteClient.findConsents(eql(eventsCredentials), any[Instant], eql(""))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = true, continuation = Some("cont2"), 1, 1), Some(Vector(notAttending1, notAttending2)))

    when(eventbriteClient.findConsents(eql(eventsCredentials), any[Instant], eql("cont2"))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = false, continuation = None, 1, 1), Some(Vector(createAttendee("email4@email.com"))))

    consentsService.syncConsents()
    verifyZeroInteractions(identityClient)
  }

  "syncConsents" should "throw an error when there are additional pages to process but no continuation token is found" in {
    val fixtures = createFixtures()
    import fixtures._

    when(config.isDebug) thenReturn false

    when(eventbriteClient.findConsents(eql(masterclassesCredentials), any[Instant], eql(""))) thenReturn
      EventbriteResponse(EventbritePagination(has_more_items = false, continuation = None, 2, 1), None)

    assertThrows[RuntimeException] {
      consentsService.syncConsents()
    }
  }
}
