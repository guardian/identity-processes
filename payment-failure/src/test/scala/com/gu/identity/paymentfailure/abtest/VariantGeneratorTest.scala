package com.gu.identity.paymentfailure.abtest

import org.scalatest.{EitherValues, Matchers, WordSpec}

class VariantGeneratorTest extends WordSpec with Matchers with EitherValues {
  import VariantGenerator._

  "The getSegmentId function" should {

    "return an error if the identity id is not an integer" in {
      getSegmentId("string", from = 0, to = 1).isLeft shouldBe true
    }

    "return an error if the identity id falls outside the test range" in {
      getSegmentId("499", from = 0.5, to = 1).isLeft shouldBe true
      getSegmentId("1022333", from = 0.4, to = 0.5).isLeft shouldBe true
    }

    "return a normalised ranged if the identity id falls within the test range" in {
      getSegmentId(identityId = "10000400", from = 0, to = 0.8).right.value shouldEqual 0.4
      getSegmentId(identityId = "202821050", from = 0, to = 0.2).right.value shouldEqual 0.05
    }
  }
}
