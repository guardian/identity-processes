package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.syntax._
import scalaj.http._

case class IdentityConfig(identityApiHost: String, clientAccessToken: String)

case class IdentityEmailTokenRequest(email: String)

object IdentityEmailTokenRequest {
  implicit val identityEmailTokenEncoder: Encoder[IdentityEmailTokenRequest] = deriveEncoder[IdentityEmailTokenRequest]
}

case class IdentityEmailTokenResponse(status: String, encryptedEmail: String)

object IdentityEmailTokenResponse {
  implicit val identityEmailTokenDecoder: Decoder[IdentityEmailTokenResponse] = deriveDecoder[IdentityEmailTokenResponse]
}

class IdentityClient(config: Config) extends StrictLogging {

  def encryptEmail(email: String, config:Config): Either[Throwable, IdentityEmailTokenResponse] = {
    logger.info(s"retrieving encrypted token for email : $email")

    val postResponse = Http(s"${config.idapiHost}/signin-token/email")
      .postData(IdentityEmailTokenRequest(email).asJson.toString())
      .header("X-GU-ID-Client-Access-Token", s"Bearer ${config.idapiAccessToken}")
      .header("content-type", "application/json")
      .asString

    if(postResponse.isSuccess) {
      logger.info(s"Successfully retrieved an encrypted token from Identity - body: ${postResponse.body}")
      io.circe.parser.decode[IdentityEmailTokenResponse](postResponse.body)
    } else {
      Left(new Exception( s"Failed to retrieve an encrypted token from Identity - error ${postResponse.code}   ${postResponse.statusLine}"))
    }
  }
}