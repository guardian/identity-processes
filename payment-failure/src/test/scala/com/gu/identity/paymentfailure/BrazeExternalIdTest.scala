package com.gu.identity.paymentfailure

import io.circe.parser.decode
import org.scalatest.{EitherValues, Matchers, WordSpecLike}

class BrazeExternalIdTest extends WordSpecLike with Matchers with EitherValues {

  "BrazeExternalId" when {

    "encoded as JSON" should {

      "be successfully de-serialised if the Braze external id is of type salesforce id" in {

        val json = """{"value":"id","externalIdType":"SalesforceId"}"""
        decode[BrazeExternalId](json).right.value shouldEqual BrazeExternalId.fromSalesforceId("id")
      }

      "be successfully de-serialised if the Braze external id is of type identity id" in {

        val json = """{"value":"id","externalIdType":"IdentityId"}"""
        decode[BrazeExternalId](json).right.value shouldEqual BrazeExternalId.fromIdentityId("id")
      }

      "be successfully de-serialised if the Braze external id is of type Braze uuid" in {

        val json = """{"value":"id","externalIdType":"BrazeUuid"}"""
        decode[BrazeExternalId](json).right.value shouldEqual BrazeExternalId.fromBrazeUuid("id")
      }
    }
  }
}
