package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import scalaj.http.Http
import io.circe.syntax._


class BrazeClient extends StrictLogging{

  def sendEmail(emailData: IdentityBrazeEmailData, emailToken: String, config: Config) : Either[Throwable, BrazeResponse] = {
    logger.info(s"send BrazeEmail for email ${emailData.emailAddress} with token $emailToken with templateId ${emailData.templateId}")

    val sendRequest = BrazeSendRequest(emailData.externalId, config.brazeApiKey, emailData.templateId, emailData.customFields + ("emailToken" -> emailToken))

    val postResponse = Http(s"${config.brazeApiHost}/campaigns/trigger/send")
        .header("content-type", "application/json")
        .postData(sendRequest.asJson.toString)
        .asString

    if (postResponse.isSuccess) {
      logger.info(s"Successfully sent email from Braze for email: ${emailData.emailAddress} with templateId ${emailData.templateId}")
      io.circe.parser.decode[BrazeResponse](postResponse.body)
    } else {
      logger.info(s"Failed to send email from Braze, error with status ${postResponse.code} - error ${postResponse.body}")
      Left( new Exception(s"sendEmail error with status ${postResponse.code} - error ${postResponse.body}"))
    }
  }
}
