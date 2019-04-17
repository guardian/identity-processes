package com.gu.identity.paymentfailure.abtest

import cats.syntax.either._
import com.gu.identity.paymentfailure.{BrazeClient, IdentityClient}
import com.gu.identity.paymentfailure.IdentityClient.AutoSignInLinkRequestBody

import scala.util.Random

class AutoSignInTest(identityClient: IdentityClient) extends VariantGenerator {
  import AutoSignInTest._

  override def generateVariant(identityId: String, email: String): Either[Throwable, Variant] = {
    if (Random.nextDouble() < 1d / 2d) {
      Right(controlVariant)
    } else {
      val body = AutoSignInLinkRequestBody(identityId, email)
      identityClient.createAutoSignInToken(body)
        .bimap(
          err => new RuntimeException("unable to create auto sign-in token", err),
          response => autoSignInTokenVariant(response.token)
        )
    }
  }
}

object AutoSignInTest {

  val testName = "auto-sign-in-token-test"

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
