package com.gu.identity.paymentfailure

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

class SendEmailServiceTest extends WordSpec with Matchers with MockitoSugar {

  trait TestFixture {
    val config = mock[Config]
    val identityClient = mock[IdentityClient]
    val brazeClient = mock[BrazeClient]
    val sendEmailService = new SendEmailService(identityClient, brazeClient, config)

    when(config.brazeApiKey).thenReturn("braze-api-key")
  }

  "The sendEmail method" should {

    "trigger an email in Braze with the emailToken trigger property, if email is successfully encrypted" in new TestFixture {

      val emailData = IdentityBrazeEmailData("1111", "test@test.com", "templateIdMock", Map("first name" -> "test name"))

      when(identityClient.encryptEmail("test@test.com")).thenReturn(Right(IdentityEmailTokenResponse("OK", "encryptedEmailString")))

      val brazeRequest = BrazeSendRequest(
        api_key = "braze-api-key",
        campaign_id = "templateIdMock",
        recipients = List(
          BrazeRecipient(
            external_user_id = "1111",
            trigger_properties = Map(
              "first name" -> "test name",
              "emailToken" -> "encryptedEmailString"
            )
          )
        )
      )

      sendEmailService.sendEmail(emailData)

      verify(identityClient, times(1)).encryptEmail("test@test.com")
      verify(brazeClient, times(1)).sendEmail(brazeRequest)
    }

    "trigger an email in Braze without the emailToken trigger property, if email encryption fails" in new TestFixture {

      val emailData = IdentityBrazeEmailData("1111", "test@test.com", "templateIdMock", Map("first name" -> "test name"))

      val mockException = mock[Exception]
      when(identityClient.encryptEmail("test@test.com")).thenReturn(Left(mockException))

      val brazeRequest = BrazeSendRequest(
        api_key = "braze-api-key",
        campaign_id = "templateIdMock",
        recipients = List(
          BrazeRecipient(
            external_user_id = "1111",
            trigger_properties = Map(
              "first name" -> "test name"
            )
          )
        )
      )

      sendEmailService.sendEmail(emailData)

      verify(identityClient, times(1)).encryptEmail("test@test.com")
      verify(brazeClient, times(1)).sendEmail(brazeRequest)

    }
  }
}
