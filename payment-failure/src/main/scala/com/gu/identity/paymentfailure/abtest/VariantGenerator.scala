package com.gu.identity.paymentfailure.abtest

import cats.syntax.either._

// metadata in the variant is included in the metadata sent to Braze.
// See BrazeEmailServiceWithAbTest for more context.
case class Variant(testName: String, variantName: String, metadata: Map[String, String] = Map.empty) {
  override def toString: String = s"Test Name: $testName, Variant Name: $variantName"
}

case class UserNotInVariantRange(
  message: String,
  cause: Option[Throwable] = None) extends RuntimeException(message, cause.orNull)

// Used to generate a variant for a test.
trait VariantGenerator {

  // Description of AB test that variants are being generated for.
  def abTest: String

  def generateVariant(identityId: String, email: String): Either[Throwable, Variant]
}

object VariantGenerator {

  // Utility function to map users to variants.
  // Returns a number in the range [0, 1) based on identity id.
  // Takes the last 3 digits; divides by 1000; returns an error if the result isn't in the specified range.
  // See test suite VariantGeneratorTest for examples.
  def getSegmentId(identityId: String, from: Double, to: Double): Either[Throwable, Double] =
    Either.catchNonFatal(identityId.takeRight(3).toDouble / 1000d)
      .leftMap(err => UserNotInVariantRange(s"unable to derive test segment from identity id $identityId", Some(err)))
      .ensure(UserNotInVariantRange(s"user not in range [$from, $to)"))(id => id >= from && id < to)
}