package com.gu.identity.paymentfailure.abtest

import cats.syntax.either._
import com.gu.identity.paymentfailure.{BrazeClient, IdentityClient}
import com.gu.identity.paymentfailure.IdentityClient.IdentityEmailTokenRequest

import scala.util.Random

class EncryptedEmailTest(identityClient: IdentityClient) extends VariantGenerator {
  import EncryptedEmailTest._

  override val abTest: String = testName

  override def generateVariant(identityId: String, email: String): Either[Throwable, Variant] = {
    if (Random.nextDouble() < 1d / 2d) {
      Right(controlVariant)
    } else {
      val requestBody = IdentityEmailTokenRequest(email)
      identityClient.encryptEmail(requestBody)
        .bimap(
          err => new RuntimeException("unable to create email token", err),
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
