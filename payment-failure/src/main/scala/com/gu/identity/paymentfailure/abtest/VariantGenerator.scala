package com.gu.identity.paymentfailure.abtest

// metadata in the variant is included in the metadata sent to Braze.
// See BrazeEmailServiceWithAbTest for more context.
case class Variant(testName: String, variantName: String, metadata: Map[String, String] = Map.empty)

// Used to generate a variant for a test.
trait VariantGenerator {
  def generateVariant(identityId: String, email: String): Either[Throwable, Variant]
}
