package com.gu.identity.paymentfailure

import BrazeClient.TriggerProperties
import cats.syntax.either._
import com.gu.identity.paymentfailure.abtest._
import com.typesafe.scalalogging.StrictLogging

// Make this an abstract trait to allow different implementations of sending an email.
// See implementations for more context.
trait BrazeEmailService {
  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse]
}

object BrazeEmailService {

  // Utility method for constructing the body of a send email request.
  // Will typically be used by all implementations of the BrazeEmailService
  def brazeSendRequest(
    brazeApiKey: String,
    emailData: IdentityBrazeEmailData,
    customFields: Map[String, String]
  ): BrazeSendRequest = {
    val recipient = BrazeRecipient(emailData.externalId.value, emailData.customFields ++ customFields)
    BrazeSendRequest(brazeApiKey, emailData.templateId, List(recipient))
  }
}

// Sends an email with encrypted email and auto sign-in tokens included as trigger properties.
class DefaultBrazeEmailService(brazeClient: BrazeClient, config: Config) extends BrazeEmailService with StrictLogging {

  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    val request = BrazeEmailService.brazeSendRequest(config.brazeApiKey, emailData, customFields = Map.empty)
    brazeClient.sendEmail(request)
  }
}

// Used to send an email with additional trigger properties (aka Braze metadata) derived from a variant in an AB test.
// For example abName, abVariant and an additional token to facilitate sign-in.
// See e.g. AutoSignInTest for a concrete example.
class BrazeEmailServiceWithAbTest(
    brazeClient: BrazeClient,
    variantGenerator: VariantGenerator,
    config: Config
) extends BrazeEmailService with StrictLogging {

  import BrazeEmailServiceWithAbTest._

  private def sendEmailWithCustomFields(emailData: IdentityBrazeEmailData, customFields: Map[String, String]): Either[Throwable, BrazeResponse] = {
    val request = BrazeEmailService.brazeSendRequest(config.brazeApiKey, emailData, customFields)
    brazeClient.sendEmail(request)
  }

  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    logger.info(s"attempting to send email for test ${variantGenerator.abTest}")
    (for {
      variant <- variantGenerator.generateVariant(emailData.externalId.value, emailData.emailAddress)
      customFields = variantToCustomFields(variant)
      response <- sendEmailWithCustomFields(emailData, customFields)
    } yield {
      logger.info(s"braze email sent with variant data for test - variant data: $variant")
      response
    }).recoverWith {
      // If this is a UserIneligibleForAbTest error (which, for example, can be caused by a user not meeting the
      // requirements to be sent an auto sign in token), we still want to send the email but without the variant
      // meta data
      case err: UserIneligibleForAbTest =>
        logger.info("user ineligible for AB test, sending regular email", err)
        sendEmailWithCustomFields(emailData, customFields = Map.empty)
    }
  }
}

object BrazeEmailServiceWithAbTest {

  def variantToCustomFields(variant: Variant): Map[String, String] =
    variant.metadata ++ Map(
      TriggerProperties.abName -> variant.testName,
      TriggerProperties.abVariant -> variant.variantName
    )
}