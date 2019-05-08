package com.gu.identity.paymentfailure.abtest

import cats.syntax.either._
import com.gu.identity.paymentfailure.{BrazeClient, IdentityClient}
import com.gu.identity.paymentfailure.IdentityClient.{ApiError, IdentityClientError, IdentityEmailTokenRequest}

import scala.util.Random

class EncryptedEmailTest(identityClient: IdentityClient) extends VariantGenerator {
  import EncryptedEmailTest._

  override val abTest: String = testName

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

  override def generateVariant(identityId: String, email: String): Either[Throwable, Variant] = {
    if (Random.nextDouble() < 1d / 2d) {
      Right(controlVariant)
    } else {
      val requestBody = IdentityEmailTokenRequest(email)
      identityClient.encryptEmail(requestBody)
        .bimap(
          err => handleIdentityClientError(err),
          response => emailTokenVariant(response.encryptedEmail)
        )
    }
  }
}

object EncryptedEmailTest {

  val testName = "auto-sign-in-test"

  val controlVariant = Variant(testName, variantName = "control")

  def emailTokenVariant(token: String): Variant =
    Variant(
      testName ,
      variantName = "email-token",
      metadata = Map(
        BrazeClient.TriggerProperties.emailToken -> token
      )
    )
}
