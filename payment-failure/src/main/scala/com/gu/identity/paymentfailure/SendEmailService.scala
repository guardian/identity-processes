package com.gu.identity.paymentfailure

import com.gu.identity.paymentfailure.IdentityClient.{AutoSignInLinkRequestBody, IdentityEmailTokenRequest}
import com.typesafe.scalalogging.StrictLogging

class SendEmailService(identityClient: IdentityClient, brazeClient: BrazeClient, config: Config) extends StrictLogging {

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
          logger.error(s"unable to auto sign in token for identity id $identityId and email $email", err)
          Option.empty[String]
        },
        response => Some(response.token)
      )

  // Whilst we migrate from encrypted email tokens to auto sign-in tokens,
  // send both tokens to Braze to be embedded into payment failure links.
  // Identity frontend can then decide which piece of information to utilise.
  // TODO: remove encrypted email token once migration is complete.
  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    val encryptedEmailToken = encryptEmail(emailData.emailAddress)
    val autoSignInToken = createAutoSignInToken(emailData.externalId, emailData.emailAddress)
    val request = SendEmailService.brazeSendRequest(config.brazeApiKey, emailData, encryptedEmailToken, autoSignInToken)
    brazeClient.sendEmail(request)
  }
}

object SendEmailService {

  def brazeSendRequest(
      brazeApiKey: String,
      emailData: IdentityBrazeEmailData,
      encryptedEmail: Option[String],
      autoSignInToken: Option[String]
  ): BrazeSendRequest = {
    // If encryptedEmail or autoSignInToken are defined,
    // add them to the trigger_properties so they can be utilised by templates in Braze.
    // The keys of the respective values are what is expected in Braze e.g. emailToken
    val tokenFields = List(
      encryptedEmail.map("emailToken" -> _),
      autoSignInToken.map("autoSignInToken" -> _)
    ).flatten.toMap

    val recipient = BrazeRecipient(emailData.externalId, emailData.customFields ++ tokenFields)
    BrazeSendRequest(brazeApiKey, emailData.templateId, List(recipient))
  }
}