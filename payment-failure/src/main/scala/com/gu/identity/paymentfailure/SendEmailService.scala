package com.gu.identity.paymentfailure

import BrazeClient.TriggerProperties
import cats.syntax.either._
import com.gu.identity.paymentfailure.EncryptedEmailTest.Variant
import com.gu.identity.paymentfailure.IdentityClient.{AutoSignInLinkRequestBody, IdentityEmailTokenRequest}
import com.typesafe.scalalogging.StrictLogging

class SendEmailService(
    identityClient: IdentityClient,
    brazeClient: BrazeClient,
    config: Config,
    encryptedEmailTest: EncryptedEmailTest
) extends StrictLogging {

  import SendEmailService._

  // Recover from error by allowing for the encrypted email token to be optional.
  // Would rather send an email without a token than not at all.
  def encryptEmail(email: String): Option[String] =
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
  def createAutoSignInToken(identityId: String, email: String): Option[String] =
    identityClient.createAutoSignInToken(AutoSignInLinkRequestBody(identityId, email))
      .fold(
        err => {
          // TODO: monitor these errors
          logger.error(s"unable to create auto sign in token for identity id $identityId and email $email", err)
          Option.empty[String]
        },
        response => Some(response.token)
      )

  def sendEmailWithCustomFields(emailData: IdentityBrazeEmailData, customFields: Map[String, String]): Either[Throwable, BrazeResponse] = {
    val request = SendEmailService.brazeSendRequest(config.brazeApiKey, emailData, customFields)
    brazeClient.sendEmail(request)
  }

  // Whilst we migrate from encrypted email tokens to auto sign-in tokens,
  // send both tokens to Braze to be embedded into payment failure links.
  // Identity frontend can then decide which piece of information to utilise.
  // This method is currently be called by the lambda since an AB test is being run.
  // Keep it as it will most likely be used once the AB test is finished.
  def sendEmailSignInTokens(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    val encryptedEmailToken = encryptEmail(emailData.emailAddress)
    val autoSignInToken = createAutoSignInToken(emailData.externalId, emailData.emailAddress)
    val tokenFields = List(
      encryptedEmailToken.map(TriggerProperties.emailToken -> _),
      autoSignInToken.map(TriggerProperties.autoSignInToken -> _)
    ).flatten.toMap
    sendEmailWithCustomFields(emailData, tokenFields)
  }

  def sendEmailWithEncryptedEmailTest(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    (for {
      variant <- encryptedEmailTest.generateVariant(emailData.externalId, emailData.emailAddress)
      customFields = testVariantToCustomFields(variant)
      response <- sendEmailWithCustomFields(emailData, customFields)
    } yield {
      response
    }).recoverWith { case _ =>
      // Failure to send an email with the encrypted email test should not prevent the email being sent,
      // so retry without the test data.
      sendEmailWithCustomFields(emailData, customFields = Map.empty)
    }
  }
}

object SendEmailService {

  def testVariantToCustomFields(variant: Variant): Map[String, String] = {
    val customFields = Map(TriggerProperties.abName -> variant.testName, TriggerProperties.abVariant -> variant.name)
    variant match {
      case Variant.Control => customFields
      case Variant.EncryptedEmail(token) => customFields + (TriggerProperties.emailToken -> token)
    }
  }

  def brazeSendRequest(
      brazeApiKey: String,
      emailData: IdentityBrazeEmailData,
      customFields: Map[String, String]
  ): BrazeSendRequest = {
    val recipient = BrazeRecipient(emailData.externalId, emailData.customFields ++ customFields)
    BrazeSendRequest(brazeApiKey, emailData.templateId, List(recipient))
  }
}