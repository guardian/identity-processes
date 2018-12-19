package com.gu.identity.paymentfailure
import com.gu.identity.paymentfailure.Model.{BrazeResponse, IdentityBrazeEmailData, Config}

class SendEmailService (identityClient: IdentityClient, brazeClient: BrazeClient){

  def sendEmail(emailData: IdentityBrazeEmailData, config: Config): Either[Throwable, BrazeResponse] = {
    for {
      encryptedTokenResponse <- identityClient.encryptEmail(emailData.emailAddress, config)
      brazeResponse <- brazeClient.sendEmail(emailData, encryptedTokenResponse.encryptedEmail)
    } yield brazeResponse
  }
}
