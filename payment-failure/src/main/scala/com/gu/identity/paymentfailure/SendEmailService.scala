package com.gu.identity.paymentfailure
import com.gu.identity.paymentfailure.Model.{BrazeResponse, IdentityBrazeEmailData}

class SendEmailService (identityClient: IdentityClient, brazeClient: BrazeClient){

  def sendEmail(emailData: IdentityBrazeEmailData): Either[Throwable, BrazeResponse] = {
    for {
      encryptedTokenResponse <- identityClient.encryptEmail(emailData.emailAddress)
      brazeResponse <- brazeClient.sendEmail(emailData, encryptedTokenResponse.encryptedEmail)
    } yield brazeResponse
  }
}
