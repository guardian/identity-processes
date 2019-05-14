package com.gu.identity.paymentfailure.abtest

import cats.syntax.either._
import com.gu.identity.paymentfailure.{BrazeClient, IdentityClient}
import com.gu.identity.paymentfailure.IdentityClient.{ApiError, AutoSignInLinkRequestBody, IdentityClientError}

class AutoSignInTest(identityClient: IdentityClient) extends VariantGenerator {
  import AutoSignInTest._

  override def abTest: String = testName

  def handleIdentityClientError(err: IdentityClientError): Throwable =
    err match {
      // If the user is invalid for an auto sign-in token,
      // return an UserIneligibleForAbTest,
      // so that the BrazeEmailServiceWithAbTest will send them a regular email
      case apiError: ApiError if apiError.isInvalidUser => UserIneligibleForAbTest("user not eligible for auto sign-in token", Some(apiError))
      // Otherwise return a RuntimeException
      // The BrazeEmailServiceWithAbTest will propagate this error,
      // meaning the lambda will be invoked again
      // i.e. another attempt will be made to send the email to the reader
      // with the variant meta data
      case _ => new RuntimeException("unable to create auto sign-in token", err)
    }

  override def generateVariant(identityId: String, email: String): Either[Throwable, Variant] =
    VariantGenerator.getSegmentId(identityId, from = 0, to = 0.2)
      .flatMap {
        case id if id < 0.1 => Right(controlVariant)
        case _ =>
          val body = AutoSignInLinkRequestBody(identityId, email)
          identityClient.createAutoSignInToken(body)
            .bimap(
              err => handleIdentityClientError(err),
              response => autoSignInTokenVariant(response.token)
            )
      }
}

object AutoSignInTest {

  val testName = "auto-sign-in-token-test-v2"

  val controlVariant = Variant(testName, variantName = "control")

  def autoSignInTokenVariant(token: String): Variant =
    Variant(
      testName,
      variantName = "auto-sign-in-token",
      metadata = Map(
        BrazeClient.TriggerProperties.autoSignInToken -> token
      )
    )
}