package com.gu.identity.paymentfailure

import com.gu.identity.paymentfailure.IdentityClient.{AutoSignInLinkRequestBody, AutoSignInLinkResponseBody, IdentityEmailTokenRequest, IdentityEmailTokenResponse}
import com.gu.identity.paymentfailure.abtest.{AutoSignInTest, Variant, VariantGenerator}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._

class DefaultBrazeEmailServiceTest extends WordSpec with Matchers with MockitoSugar {

  trait TestFixture {
    val config = mock[Config]
    val identityClient = mock[IdentityClient]
    val brazeClient = mock[BrazeClient]
    val sendEmailService = new DefaultBrazeEmailService(identityClient, brazeClient, config)

    when(config.brazeApiKey).thenReturn("braze-api-key")
  }

  "The DefaultBrazeEmailService" when {

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


class BrazeEmailServiceWithAbTestTest extends WordSpec with Matchers with MockitoSugar {

  trait TestFixture {
    val config: Config = mock[Config]
    val autoSignInTest: VariantGenerator = mock[AutoSignInTest]
    val brazeClient: BrazeClient = mock[BrazeClient]
    val sendEmailService = new BrazeEmailServiceWithAbTest(brazeClient, autoSignInTest, config)

    when(config.brazeApiKey).thenReturn("braze-api-key")
  }

  "The BrazeEmailServiceWithAbTest " when {

    "it is used to send an email" should {

      "include the meta data in the AB test variant" in new TestFixture {

        when(autoSignInTest.generateVariant("identity-id", "email"))
          .thenReturn(
            Right(
              Variant(
                testName = "ab-name",
                variantName = "ab-variant",
                metadata = Map(
                  BrazeClient.TriggerProperties.autoSignInToken -> "example-auto-sign-in-token",
                  BrazeClient.TriggerProperties.emailToken -> "example-email-token"
                )
              )
            )
          )

        when(brazeClient.sendEmail(any[BrazeSendRequest]))
          .thenReturn(Right(BrazeResponse("ok")))

        sendEmailService.sendEmail(
          IdentityBrazeEmailData(
            externalId = "identity-id",
            emailAddress = "email",
            templateId = "template-id",
            customFields = Map.empty
          )
        )

        verify(brazeClient, times(1))
          .sendEmail(
            BrazeSendRequest(
              api_key = "braze-api-key",
              campaign_id = "template-id",
              recipients = List(
                BrazeRecipient(
                  external_user_id = "identity-id",
                  trigger_properties = Map(
                    "abName" -> "ab-name",
                    "abVariant" -> "ab-variant",
                    BrazeClient.TriggerProperties.autoSignInToken -> "example-auto-sign-in-token",
                    BrazeClient.TriggerProperties.emailToken -> "example-email-token"
                  )
                )
              )
            )
          )
      }
    }
  }
}