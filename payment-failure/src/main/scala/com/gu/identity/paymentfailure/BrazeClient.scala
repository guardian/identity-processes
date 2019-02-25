package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import cats.syntax.either._
import io.circe.parser.decode
import io.circe.syntax._
import scalaj.http.Http


class BrazeClient(config: Config) extends StrictLogging {

  def sendEmail(request: BrazeSendRequest) : Either[Throwable, BrazeResponse] = {

    logger.info(s"sending email via Braze - request data: $request")

    Either.catchNonFatal(
      Http(s"${config.brazeApiHost}/campaigns/trigger/send")
      .header("content-type", "application/json")
      .postData(request.asJson.toString)
      .asString
    ).flatMap { response =>
      val body = response.body
      if (response.isSuccess) {
        logger.info(s"successfully executed braze request: $request - response body: $body")
        decode[BrazeResponse](body)
      } else {
        val message = s"failed to send email from Braze, error with status ${response.code} - error $body"
        logger.error(message)
        Left(new Exception(message))
      }
    }
  }
}

object BrazeClient {

  // Valid keys of trigger properties that are utilised in Braze templates.
  object TriggerProperties {
    val emailToken = "emailToken"
    val autoSignInToken = "autoSignInToken"
    val abName = "abName"
    val abVariant = "abVariant"
  }
}