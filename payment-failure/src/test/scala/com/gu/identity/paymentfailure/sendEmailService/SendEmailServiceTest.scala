package com.gu.identity.paymentfailure.sendEmailService

import com.gu.identity.paymentfailure.IdentityClient.{AutoSignInLinkRequestBody, AutoSignInLinkResponseBody, IdentityEmailTokenRequest, IdentityEmailTokenResponse}
import com.gu.identity.paymentfailure._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._

class SendEmailServiceTest extends WordSpec with Matchers with MockitoSugar {

  trait TestFixture {
    val config = mock[Config]
    val identityClient = mock[IdentityClient]
    val brazeClient = mock[BrazeClient]
    val sendEmailService = new SendEmailService(identityClient, brazeClient, config)

    when(config.brazeApiKey).thenReturn("braze-api-key")
  }

  "The SendEmailService" when {

    "it is used to send an email with an encrypted email token" should {

      "trigger an email in Braze with the emailToken trigger property, if email is successfully encrypted" in new TestFixture {

        val emailData = IdentityBrazeEmailData("1111", "test@test.com", "templateIdMock", Map("first name" -> "test name"))

        when(identityClient.encryptEmail(IdentityEmailTokenRequest("test@test.com"))).thenReturn(Right(IdentityEmailTokenResponse("OK", "encryptedEmailString")))

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

        sendEmailService.sendEmailWithEncryptedEmail(emailData)

        verify(identityClient, times(1)).encryptEmail(IdentityEmailTokenRequest("test@test.com"))
        verify(brazeClient, times(1)).sendEmail(brazeRequest)
      }

      "trigger an email in Braze without the emailToken trigger property, if email encryption fails" in new TestFixture {

        val emailData = IdentityBrazeEmailData("1111", "test@test.com", "templateIdMock", Map("first name" -> "test name"))

        val mockException = mock[Exception]
        when(identityClient.encryptEmail(IdentityEmailTokenRequest("test@test.com"))).thenReturn(Left(mockException))

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

        sendEmailService.sendEmailWithEncryptedEmail(emailData)

        verify(identityClient, times(1)).encryptEmail(IdentityEmailTokenRequest("test@test.com"))
        verify(brazeClient, times(1)).sendEmail(brazeRequest)

      }
    }

    "it is used to send an email with an auth sign-in link" should {

      "trigger an email in Braze with the autoSignInToken trigger property if an auto sign in token is generated" in new TestFixture {

        when(identityClient.createAutoSignInToken(any[AutoSignInLinkRequestBody]))
          .thenReturn(Right(AutoSignInLinkResponseBody("token")))

        sendEmailService.sendEmailWithAutoSignInLink(
          IdentityBrazeEmailData(
            externalId = "identity-id",
            emailAddress = "email",
            templateId = "template-id",
            customFields = Map("name" -> "test-user")
          )
        )

        verify(identityClient, times(1))
          .createAutoSignInToken(AutoSignInLinkRequestBody("identity-id", "email"))

        verify(brazeClient, times(1))
          .sendEmail(
            BrazeSendRequest(
              api_key = "braze-api-key",
              campaign_id = "template-id",
              recipients = List(
                BrazeRecipient(
                  external_user_id = "identity-id",
                  trigger_properties = Map(
                    "name" -> "test-user",
                    "autoSignInToken" -> "token"
                  )
                )
              )
            )
          )
      }

      "trigger an email in Braze without the autoSignInToken trigger property if an auto sign in token isn't generated" in new TestFixture {

        when(identityClient.createAutoSignInToken(any[AutoSignInLinkRequestBody]))
          .thenReturn(Left(any[Throwable]))

        sendEmailService.sendEmailWithAutoSignInLink(
          IdentityBrazeEmailData(
            externalId = "identity-id",
            emailAddress = "email",
            templateId = "template-id",
            customFields = Map("name" -> "test-user")
          )
        )

        verify(identityClient, times(1))
          .createAutoSignInToken(AutoSignInLinkRequestBody("identity-id", "email"))

        verify(brazeClient, times(1))
          .sendEmail(
            BrazeSendRequest(
              api_key = "braze-api-key",
              campaign_id = "template-id",
              recipients = List(
                BrazeRecipient(
                  external_user_id = "identity-id",
                  trigger_properties = Map("name" -> "test-user")
                )
              )
            )
          )
      }
    }
  }
}
