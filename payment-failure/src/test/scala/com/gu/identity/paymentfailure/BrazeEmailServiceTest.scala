package com.gu.identity.paymentfailure.sendEmailService

import com.gu.identity.paymentfailure.IdentityClient.{AutoSignInLinkRequestBody, AutoSignInLinkResponseBody, IdentityEmailTokenRequest, IdentityEmailTokenResponse}
import com.gu.identity.paymentfailure._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._

class BrazeEmailServiceTest extends WordSpec with Matchers with MockitoSugar {

  trait TestFixture {
    val config = mock[Config]
    val identityClient = mock[IdentityClient]
    val brazeClient = mock[BrazeClient]
    val sendEmailService = new DefaultBrazeEmailService(identityClient, brazeClient, config)

    when(config.brazeApiKey).thenReturn("braze-api-key")
  }

  "The SendEmailService" when {

    "it is used to send an email with auto sign-in and email tokens" should {

      "send the email with both tokens if they have both been created" in new TestFixture {

        when(identityClient.createAutoSignInToken(any[AutoSignInLinkRequestBody]))
          .thenReturn(Right(AutoSignInLinkResponseBody("auto-signin-token")))

        when(identityClient.encryptEmail(any[IdentityEmailTokenRequest]))
            .thenReturn(Right(IdentityEmailTokenResponse("ok", "email-token")))

        sendEmailService.sendEmail(
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
                    "autoSignInToken" -> "auto-signin-token",
                    "emailToken" -> "email-token"
                  )
                )
              )
            )
          )
      }

      "still send them email with the auto sign-in token if the email token hasn't been created" in new TestFixture {

        when(identityClient.createAutoSignInToken(any[AutoSignInLinkRequestBody]))
          .thenReturn(Right(AutoSignInLinkResponseBody("auto-signin-token")))

        when(identityClient.encryptEmail(any[IdentityEmailTokenRequest]))
          .thenReturn(Left(new Exception))

        sendEmailService.sendEmail(
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
                    "autoSignInToken" -> "auto-signin-token"
                  )
                )
              )
            )
          )
      }

      "still send the email with the email token if the auto sign-in token hasn't been created" in new TestFixture {

        when(identityClient.createAutoSignInToken(any[AutoSignInLinkRequestBody]))
          .thenReturn(Left(new Exception))

        when(identityClient.encryptEmail(any[IdentityEmailTokenRequest]))
          .thenReturn(Right(IdentityEmailTokenResponse("ok", "email-token")))

        sendEmailService.sendEmail(
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
                    "emailToken" -> "email-token"
                  )
                )
              )
            )
          )
      }

      "still send the email with no tokens if neither of them were created" in new TestFixture {

        when(identityClient.createAutoSignInToken(any[AutoSignInLinkRequestBody]))
          .thenReturn(Left(new Exception))

        when(identityClient.encryptEmail(any[IdentityEmailTokenRequest]))
          .thenReturn(Left(new Exception))

        sendEmailService.sendEmail(
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
