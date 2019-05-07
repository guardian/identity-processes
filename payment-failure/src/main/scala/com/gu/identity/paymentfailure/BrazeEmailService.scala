package com.gu.identity.paymentfailure

import BrazeClient.TriggerProperties
import cats.syntax.either._
import com.gu.identity.paymentfailure.IdentityClient.{AutoSignInLinkRequestBody, IdentityEmailTokenRequest}
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
    val recipient = BrazeRecipient(emailData.externalId, emailData.customFields ++ customFields)
    BrazeSendRequest(brazeApiKey, emailData.templateId, List(recipient))
  }
}

// Sends an email with encrypted email and auto sign-in tokens included as trigger properties.
class DefaultBrazeEmailService(
    identityClient: IdentityClient,
    brazeClient: BrazeClient,
    config: Config
) extends BrazeEmailService with StrictLogging {

  // Recover from error by allowing for the encrypted email token to be optional.
  // Would rather send an email without a token than not at all.
  private def encryptEmail(email: String): Option[String] =
    identityClient.encryptEmail(IdentityEmailTokenRequest(email))
    .fold(
      err => {
        // TODO: monitor these errors
        // Log this error as otherwise on folding to an option, this information would be lost.
        logger.error(s"unable to encrypt email $email", err)
        Option.empty[String]
      },
      response => Some(response.encryptedEmail)
    )

  // Recover from error by allowing for the auto sign in token to be optional.
  // Would rather send an email without a token than not at all.
  private def createAutoSignInToken(identityId: String, email: String): Option[String] =
    identityClient.createAutoSignInToken(AutoSignInLinkRequestBody(identityId, email))
      .fold(
        err => {
          // TODO: monitor these errors
          logger.error(s"unable to create auto sign in token for identity id $identityId and email $email", err)
          Option.empty[String]
        },
        response => Some(response.token)
      )

  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    val encryptedEmailToken = encryptEmail(emailData.emailAddress)
    val autoSignInToken = createAutoSignInToken(emailData.externalId, emailData.emailAddress)
    val tokenFields = List(
      encryptedEmailToken.map(TriggerProperties.emailToken -> _),
      autoSignInToken.map(TriggerProperties.autoSignInToken -> _)
    ).flatten.toMap
    val request = BrazeEmailService.brazeSendRequest(config.brazeApiKey, emailData, tokenFields)
    brazeClient.sendEmail(request)
  }
}

// Used to send an email with additional trigger properties (aka Braze metadata) derived from a variant in an AB test.
// For example abName, abVariant and an additional token to facilitate sign-in.
// See e.g. EncryptedEmailTest for a concrete example.
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
      variant <- variantGenerator.generateVariant(emailData.externalId, emailData.emailAddress)
      customFields = variantToCustomFields(variant)
      response <- sendEmailWithCustomFields(emailData, customFields)
    } yield {
      logger.info(s"braze email sent with variant data for test - variant data: $variant")
      response
    }).recoverWith {
      case err: UserIneligibleForAbTest => {
        logger.info("user ineligible for AB test, sending regular email", err)
        sendEmailWithCustomFields(emailData, customFields = Map.empty)
      }
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