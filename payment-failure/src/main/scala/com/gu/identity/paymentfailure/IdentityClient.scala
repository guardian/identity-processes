package com.gu.identity.paymentfailure

import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.JsonCodec
import io.circe.parser.decode
import io.circe.syntax._
import scalaj.http._
import io.circe.{Decoder, Error => CirceError, Encoder}

class IdentityClient(config: Config) extends StrictLogging {
  import IdentityClient._

  // Given a way of serialising the request body (Req) as JSON
  // and deserialising the response body to the target case class (Res)
  // execute a POST request against the given path.
  private def executePostRequest[Req: Encoder, Res: Decoder](path: String, requestBody: Req): Either[IdentityClientError, Res] = {
    logger.info(s"executing POST $path")

    val request = Http(s"${config.idapiHost}$path")
      .postData(requestBody.asJson.noSpaces)
      .header("X-GU-ID-Client-Access-Token", s"Bearer ${config.idapiAccessToken}")
      .header("content-type", "application/json")

    Either.catchNonFatal(request.asString)
      .leftMap(err => IdentityClientError.fromThrowable(err))
      .flatMap {
        case res if res.is2xx => decode[Res](res.body).leftMap(err => IdentityClientError.fromCirceError(res.body, err))
        case res => decode[ApiError](res.body) match {
          case Right(apiError) => Left(IdentityClientError.fromApiError(apiError))
          case Left(err) => Left(IdentityClientError.fromCirceError(res.body, err))
        }
      }
  }

  def encryptEmail(requestBody: IdentityEmailTokenRequest): Either[IdentityClientError, IdentityEmailTokenResponse] =
    executePostRequest[IdentityEmailTokenRequest, IdentityEmailTokenResponse]("/signin-token/email", requestBody)

  def createAutoSignInToken(requestBody: AutoSignInLinkRequestBody): Either[IdentityClientError, AutoSignInLinkResponseBody] =
    executePostRequest[AutoSignInLinkRequestBody, AutoSignInLinkResponseBody]("/auto-signin-token", requestBody)
}

object IdentityClient {

  @JsonCodec case class IdentityEmailTokenRequest(email: String)

  @JsonCodec case class IdentityEmailTokenResponse(status: String, encryptedEmail: String)

  @JsonCodec case class AutoSignInLinkRequestBody(identityId: String, email: String)

  @JsonCodec case class AutoSignInLinkResponseBody(token: String)

  @JsonCodec case class ApiError(errors: List[ApiError.Single]) extends IdentityClientError {
    def invalidUser: Boolean = errors.exists(_.invalidUser)
  }

  object ApiError {
    @JsonCodec case class Single(message: String) {
      def invalidUser: Boolean = message == "Invalid user"
    }
  }

  // Models an error created by a request to the identity client; either:
  // - the API reported an error in JSON format which was successfully deserialized.
  // - an 'underlying' error occurred e.g. network error
  // - the client failed to correctly deserialize the JSON result
  sealed trait IdentityClientError extends Exception {
    override def getMessage: String = this match {
      case ApiError(errors) => errors.map(_.message).mkString(" and ")
      case UnderlyingError(err) => err.getMessage
      case UnknownJsonFormat(json, err) => s"unable to decode json: $json - ${err.getMessage}"
    }
  }

  object IdentityClientError {
    def fromThrowable(err: Throwable): IdentityClientError = UnderlyingError(err)
    def fromCirceError(json: String, err: CirceError): IdentityClientError = UnknownJsonFormat(json, err)
    def fromApiError(err: ApiError): IdentityClientError = err
  }


  case class UnderlyingError(err: Throwable) extends IdentityClientError

  case class UnknownJsonFormat(json: String, err: CirceError) extends IdentityClientError
}

