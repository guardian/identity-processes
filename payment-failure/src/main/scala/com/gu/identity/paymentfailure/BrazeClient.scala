package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging

class BrazeClient extends StrictLogging{

  def sendEmail(emailData: IdentityBrazeEmailData, emailToken: String) : Either[Throwable, BrazeResponse] = {
    val isSuccess = true
    logger.info(s"Sending payment failure request to braze for email ${emailData.emailAddress} with token $emailToken")
    isSuccess match {
      case true => Right(BrazeResponse("example success response"))
      case false => Left(new Exception( s"Failed to send email from Braze"))
    }
  }
}
