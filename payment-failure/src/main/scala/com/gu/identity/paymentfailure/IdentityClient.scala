package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.syntax._
import scalaj.http._

case class IdentityConfig(identityApiHost: String = "https://idapi.thegulocal.com", clientAccessToken: String)

case class IdentityEmailTokenRequest(email: String)
case class IdentityEmailTokenResponse(status: String, encryptedEmail: String)

class IdentityClient extends StrictLogging {

  implicit val identityEmailTokenEncoder: Encoder[IdentityEmailTokenRequest] = deriveEncoder[IdentityEmailTokenRequest]
  implicit val identityEmailTokenDecoder: Decoder[IdentityEmailTokenResponse] = deriveDecoder[IdentityEmailTokenResponse]

  def encryptEmail(email: String, config: Config): Either[Throwable, IdentityEmailTokenResponse] = {
    logger.info(s"retrieving encrypted token for email : $email")
    val identityApiHost = config.idapiHost
    val identityClientToken = config.idapiAccessToken

    val identityEmailTokenRequest: HttpRequest = Http(s"$identityApiHost/signin-token/email")

    val postResponse = identityEmailTokenRequest
      .postData(IdentityEmailTokenRequest(email).asJson.toString())
      .header("X-GU-ID-Client-Access-Token", s"Bearer $identityClientToken")
      .header("content-type", "application/json")
      .asString

    if(postResponse.isSuccess) {
      logger.info(s"Sucesfully retrieved an encrypted token from Identity - body: ${postResponse.body}")
      io.circe.parser.decode[IdentityEmailTokenResponse](postResponse.body)
    } else {
      Left(new Exception( s"Failed to retrieve an encrypted token from Identity - error ${postResponse.code}   ${postResponse.statusLine}"))
    }
  }
}