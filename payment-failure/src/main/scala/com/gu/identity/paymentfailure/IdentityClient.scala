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
      .leftMap[Throwable](err => networkError(path, err))
      .flatMap {
        case res if res.is2xx => decode[Res](res.body).leftMap(err => decodingError(path, err))
        case res => Left(non2xxError(path, res.code, res.body))
      }
  }

  def encryptEmail(requestBody: IdentityEmailTokenRequest): Either[Throwable, IdentityEmailTokenResponse] =
    executePostRequest[IdentityEmailTokenRequest, IdentityEmailTokenResponse]("/signin-token/email", requestBody)

  def createAutoSignInToken(requestBody: AutoSignInLinkRequestBody): Either[Throwable, AutoSignInLinkResponseBody] =
    executePostRequest[AutoSignInLinkRequestBody, AutoSignInLinkResponseBody]("/auto-signin-token", requestBody)
}

object IdentityClient {

  @JsonCodec case class IdentityEmailTokenRequest(email: String)

  @JsonCodec case class IdentityEmailTokenResponse(status: String, encryptedEmail: String)

  @JsonCodec case class AutoSignInLinkRequestBody(identityId: String, email: String)

  @JsonCodec case class AutoSignInLinkResponseBody(token: String)

  // Utility methods for creating exceptions related to the identity client.
  // Since these are only getting called in the context of POST requests,
  // can augment the messages with this information.
  // Also, no point creating specific subtypes to model these errors at the moment,
  // since the lambda takes the same action, regardless of the error.

  def networkError(path: String, cause: Throwable): Exception =
    new Exception(s"POST $path failed", cause)

  def non2xxError(path: String, statusCode: Int, body: String): Exception =
    new Exception(s"POST $path failed with status code $statusCode and body $body")

  def decodingError(path: String, cause: io.circe.Error): Exception =
    new Exception(s"unable to decode response body for path $path", cause)
}
