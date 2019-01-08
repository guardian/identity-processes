package com.gu.identity.paymentfailure

class SendEmailService (identityClient: IdentityClient, brazeClient: BrazeClient, config: Config){

  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    for {
      encryptedTokenResponse <- identityClient.encryptEmail(emailData.emailAddress)
      brazeResponse <- brazeClient.sendEmail(emailData, encryptedTokenResponse.encryptedEmail)
    } yield brazeResponse
  }
}
