package com.gu.identity.paymentfailure

import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import io.circe.CursorOp.DownField
import io.circe.DecodingFailure
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

class SqsServiceTest extends WordSpec with Matchers with MockitoSugar {

  trait TestFixture {
    val config = mock[Config]
    val sqsService = new SqsService(config)
    val sqsMessage = mock[SQSMessage]
  }

  "The parseSingleMessage method" should {
    "return a parsed IdentityBrazeEmailData when valid json in message body" in new TestFixture {
      when(sqsMessage.getBody).thenReturn(
        """
          |{
          |   "Type": "Notification",
          |   "Message" : "{\"externalId\":{\"value\":\"100001111\",\"externalIdType\":\"IdentityId\"},\"emailAddress\":\"test@theguardian.com\",\"templateId\":\"6ec82e61-b8b0-4d11-8e5f-9914c0700f38\",\"customFields\":{\"first_name\":\"test-user-first-name\"}}"
          |}
        """.stripMargin)
      sqsService.parseMessage[IdentityBrazeEmailData](sqsMessage) shouldBe
        Right(
          IdentityBrazeEmailData(
            externalId = BrazeExternalId.fromIdentityId(identityId = "100001111"),
            emailAddress = "test@theguardian.com",
            templateId = "6ec82e61-b8b0-4d11-8e5f-9914c0700f38",
            Map("first_name" -> "test-user-first-name")
          )
        )
    }

    "throw a decoder error when invalid json is passed in message body" in new TestFixture {
      when(sqsMessage.getBody).thenReturn(
        """
          |{
          |   "Type": "Notification",
          |   "NonValid": "No good"
          |}
        """.stripMargin
      )
      sqsService.parseMessage[IdentityBrazeEmailData](sqsMessage) shouldBe Left(DecodingFailure("Attempt to decode value on failed cursor", List(DownField("Message"))))
    }
  }
}