package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import cats.syntax.either._
import io.circe.syntax._
import scalaj.http.Http


class BrazeClient(config: Config) extends StrictLogging {

  def sendEmail(emailData: IdentityBrazeEmailData, emailToken: String) : Either[Throwable, BrazeResponse] = {

    logger.info(s"sending email via Braze - email data: $emailData")

    val sendRequest = BrazeSendRequest(config.brazeApiKey, emailData.templateId, List(BrazeRecipient(emailData.externalId, emailData.customFields + ("emailToken" -> emailToken))))

    Either.catchNonFatal(
      Http(s"${config.brazeApiHost}/campaigns/trigger/send")
      .header("content-type", "application/json")
      .postData(sendRequest.asJson.toString)
      .asString
    ).flatMap(postResponse =>
        if (postResponse.isSuccess) {
          logger.info(s"Successfully sent email from Braze for email: ${emailData.emailAddress} with templateId ${emailData.templateId}")
          io.circe.parser.decode[BrazeResponse](postResponse.body)
        } else {
          logger.error(s"Failed to send email from Braze, error with status ${postResponse.code} - error ${postResponse.body}")
          Left( new Exception(s"sendEmail error with status ${postResponse.code} - error ${postResponse.body}"))
        }
    )
  }
}
