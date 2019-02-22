package com.gu.identity.paymentfailure

import cats.syntax.either._
import com.gu.identity.paymentfailure.IdentityClient.{AutoSignInLinkRequestBody, IdentityEmailTokenRequest}

import scala.util.Random

class EncryptedEmailTest(identityClient: IdentityClient) {

  import EncryptedEmailTest._

  def generateVariant(identityId: String, email: String): Either[Throwable, Variant] = {
    if (Random.nextDouble() < 1d / 2d) {
      Right(Variant.Control)
    } else {
      val requestBody = IdentityEmailTokenRequest(email)
      identityClient.encryptEmail(requestBody)
        .bimap(
          err => new RuntimeException("unable to create email token", err),
          response => Variant.EncryptedEmail(response.encryptedEmail)
        )
    }
  }
}

object EncryptedEmailTest {

  sealed trait Variant {
    final val testName: String = "auto-sign-in-test"
    def name: String
  }

  object Variant {

    case object Control extends Variant {
      override val name: String = "control"
    }

    case class EncryptedEmail private(token: String) extends Variant {
      override val name: String = "email-token"
    }
  }
}
