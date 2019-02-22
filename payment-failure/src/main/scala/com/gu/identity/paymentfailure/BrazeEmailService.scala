package com.gu.identity.paymentfailure

import BrazeClient.TriggerProperties
import cats.syntax.either._
import com.gu.identity.paymentfailure.EncryptedEmailTest.Variant
import com.gu.identity.paymentfailure.IdentityClient.{AutoSignInLinkRequestBody, IdentityEmailTokenRequest}
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

// (modulo errors) half of the emails sent will include the encrypted email token;
// the other half will include no sign-in tokens.
// There will be additional meta data (abName and abVariant) to facilitate tracking which segment a user is in.
class BrazeEmailServiceWithEncryptedBrazeEmailTest(
    brazeClient: BrazeClient,
    encryptedEmailTest: EncryptedEmailTest,
    config: Config
) extends BrazeEmailService with StrictLogging {

  import BrazeEmailServiceWithEncryptedBrazeEmailTest._

  private def sendEmailWithCustomFields(emailData: IdentityBrazeEmailData, customFields: Map[String, String]): Either[Throwable, BrazeResponse] = {
    val request = BrazeEmailService.brazeSendRequest(config.brazeApiKey, emailData, customFields)
    brazeClient.sendEmail(request)
  }

  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    (for {
      variant <- encryptedEmailTest.generateVariant(emailData.externalId, emailData.emailAddress)
      customFields = testVariantToCustomFields(variant)
      response <- sendEmailWithCustomFields(emailData, customFields)
    } yield {
      logger.info("braze email sent with encrypted email test data")
      response
    }).recoverWith { case err =>
      // Failure to send an email with the encrypted email test should not prevent the email being sent,
      // so retry without the test data.
      logger.error("failed to send email with encrypted email test data, retrying without", err)
      sendEmailWithCustomFields(emailData, customFields = Map.empty)
    }
  }
}

object BrazeEmailServiceWithEncryptedBrazeEmailTest {

  def testVariantToCustomFields(variant: Variant): Map[String, String] = {
    val customFields = Map(TriggerProperties.abName -> variant.testName, TriggerProperties.abVariant -> variant.name)
    variant match {
      case Variant.Control => customFields
      case Variant.EncryptedEmail(token) => customFields + (TriggerProperties.emailToken -> token)
    }
  }
}