package com.gu.identity.paymentfailure

import com.gu.identity.paymentfailure.abtest.{AutoSignInTest, Variant, VariantGenerator}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._

class DefaultBrazeEmailServiceTest extends WordSpec with Matchers with MockitoSugar {

  trait TestFixture {
    val config = mock[Config]
    val brazeClient = mock[BrazeClient]
    val sendEmailService = new DefaultBrazeEmailService(brazeClient, config)

    when(config.brazeApiKey).thenReturn("braze-api-key")
  }

  "The DefaultBrazeEmailService" should {

    "send an email to Braze" in new TestFixture {

      sendEmailService.sendEmail(
        IdentityBrazeEmailData(
          externalId = BrazeExternalId.fromIdentityId(identityId = "identity-id"),
          emailAddress = "email",
          templateId = "template-id",
          customFields = Map("name" -> "test-user")
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
                trigger_properties = Map("name" -> "test-user")
              )
            )
          )
        )
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
            externalId = BrazeExternalId.fromIdentityId(identityId = "identity-id"),
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