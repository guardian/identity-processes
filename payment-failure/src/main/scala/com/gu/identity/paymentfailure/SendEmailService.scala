package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging

class SendEmailService(identityClient: IdentityClient, brazeClient: BrazeClient, config: Config) extends StrictLogging {

  // Recover from error by allowing for the token to be optional.
  // Would rather send an email without a token than not at all.
  def encryptEmail(email: String): Option[String] =
    identityClient.encryptEmail(email)
    .fold(
      err => {
        // TODO: monitor these errors
        // Log this error as otherwise on folding to an option, this information would be lost.
        logger.error(s"unable to encrypt email $email", err)
        Option.empty[String]
      },
      response => Some(response.encryptedEmail)
    )

  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    val encryptedEmail = encryptEmail(emailData.emailAddress)
    val request = SendEmailService.brazeSendRequest(config.brazeApiKey, emailData, encryptedEmail)
    brazeClient.sendEmail(request)
  }
}

object SendEmailService {

  def brazeSendRequest(brazeApiKey: String, emailData: IdentityBrazeEmailData, encryptedEmail: Option[String]): BrazeSendRequest = {
    val customFields = encryptedEmail.fold(emailData.customFields)(token => emailData.customFields + ("emailToken" -> token))
    val recipient = BrazeRecipient(emailData.externalId, customFields)
    BrazeSendRequest(brazeApiKey, emailData.templateId, List(recipient))
  }
}