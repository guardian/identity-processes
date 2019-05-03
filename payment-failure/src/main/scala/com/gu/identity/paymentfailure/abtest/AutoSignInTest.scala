package com.gu.identity.paymentfailure.abtest

import cats.syntax.either._
import com.gu.identity.paymentfailure.{BrazeClient, IdentityClient}
import com.gu.identity.paymentfailure.IdentityClient.AutoSignInLinkRequestBody

class AutoSignInTest(identityClient: IdentityClient) extends VariantGenerator {
  import AutoSignInTest._

  override def abTest: String = testName

  override def generateVariant(identityId: String, email: String): Either[Throwable, Variant] =
    VariantGenerator.getSegmentId(identityId, from = 0, to = 0.9)
      .flatMap {
        case id if id < 0.1 => Right(controlVariant)
        case _ =>
          val body = AutoSignInLinkRequestBody(identityId, email)
          identityClient.createAutoSignInToken(body)
            .bimap(
              err => new RuntimeException("unable to create auto sign-in token", err),
              response => autoSignInTokenVariant(response.token)
            )
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

// Test to run before releasing AutoSignInTest.
// By only send emails with the auto sign-in token to specific identity ids,
// we can test end-to-end in production, before releasing to Guardian readers.
class AutoSignInTestPreRelease(identityClient: IdentityClient) extends VariantGenerator {
  import AutoSignInTestPreRelease._

  override def abTest: String = testName

  override def generateVariant(identityId: String, email: String): Either[Throwable, Variant] =
    if (!autoSignInIdentityIds.contains(identityId)) {
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

object AutoSignInTestPreRelease {

  val autoSignInIdentityIds = Set(
    "101284047",
    "100717027"
  )

  val testName = "auto-sign-in-token-test-pre-release"

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
