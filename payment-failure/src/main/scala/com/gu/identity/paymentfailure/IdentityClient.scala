package com.gu.identity.paymentfailure

import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec
import io.circe.parser.decode
import io.circe.syntax._
import scalaj.http._

class IdentityClient(config: Config) extends StrictLogging {
  import IdentityClient._

  // Given a way of serialising the request body (Req) as JSON
  // and deserialising the response body to the target case class (Res)
  // execute a POST request against the given path.
  private def executePostRequest[Req: Encoder, Res: Decoder](path: String, requestBody: Req): Either[Throwable, Res] = {
    logger.info(s"executing POST $path")

    val request = Http(s"${config.idapiHost}$path")
      .postData(requestBody.asJson.noSpaces)
      .header("X-GU-ID-Client-Access-Token", s"Bearer ${config.idapiAccessToken}")
      .header("content-type", "application/json")

    Either.catchNonFatal(request.asString)
      .leftMap[Throwable](err => new RuntimeException(s"POST $path failed", err))
      .flatMap {
        case res if res.isSuccess => decode[Res](res.body).leftMap(err => DecodingError(path, err))
        case res => Left(new RuntimeException(s"POST $path failed with status code ${res.code}"))
      }
  }

  def encryptEmail(requestBody: IdentityEmailTokenRequest): Either[Throwable, IdentityEmailTokenResponse] =
    executePostRequest[IdentityEmailTokenRequest, IdentityEmailTokenResponse]("/signin-token/email", requestBody)

  def createAutoSignInToken(requestBody: AutoSignInLinkRequestBody): Either[Throwable, AutoSignInLinkResponseBody] =
    executePostRequest[AutoSignInLinkRequestBody, AutoSignInLinkResponseBody]("/magic-link/generate", requestBody)
}

object IdentityClient {

  @JsonCodec case class IdentityEmailTokenRequest(email: String)

  @JsonCodec case class IdentityEmailTokenResponse(status: String, encryptedEmail: String)

  @JsonCodec case class AutoSignInLinkRequestBody(identityId: String, email: String)

  @JsonCodec case class AutoSignInLinkResponseBody(token: String)

  case class DecodingError(path: String, cause: io.circe.Error) extends Exception {
    override def getMessage: String = s"unable to decode response body for path $path - ${cause.getMessage}"
  }
}
