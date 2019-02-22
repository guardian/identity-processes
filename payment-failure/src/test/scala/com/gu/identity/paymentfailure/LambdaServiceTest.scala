package com.gu.identity.paymentfailure


import java.util

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.sqs.model.DeleteMessageResult
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{EitherValues, Matchers, WordSpec}

class LambdaServiceTest extends WordSpec with Matchers with MockitoSugar with EitherValues {

  val sqsService = mock[SqsService]
  val sendEmailService = mock[SendEmailService]

  val lambdaService = new LambdaService(sqsService, sendEmailService)

  // Mock the flow of a message being successfully processed

  val message = mock[SQSMessage]

  val emailData = mock[IdentityBrazeEmailData]
  when(sqsService.parseSingleMessage(message)).thenReturn(Right(emailData))

  val brazeResponse = mock[BrazeResponse]
  when(sendEmailService.sendEmailSignInTokens(emailData)).thenReturn(Right(brazeResponse))

  val deleteMessageResult = mock[DeleteMessageResult]
  when(sqsService.deleteMessage(message)).thenReturn(Right(deleteMessageResult))

  when(sqsService.processDeleteMessageResult(deleteMessageResult)).thenReturn(Right(()))

  // Mock the flow of a message not being successfully processed

  val invalidMessage = mock[SQSMessage]

  val parseError = mock[Throwable]
  when(sqsService.parseSingleMessage(invalidMessage)).thenReturn(Left(parseError))

  "A Lambda service" when {

    "it processes all messages successfully" should {

      "return a list of all the Braze responses" in {

        val event = mock[SQSEvent]

        when(event.getRecords).thenReturn(util.Arrays.asList(message, message))

        lambdaService.processEvent(event).toEither.right.value.shouldEqual(List(brazeResponse, brazeResponse))
      }
    }

    "it processes no messages successfully" should {

      "return a list of all the errors" in {

        val event = mock[SQSEvent]

        when(event.getRecords).thenReturn(util.Arrays.asList(invalidMessage, invalidMessage, invalidMessage))

        lambdaService.processEvent(event).toEither.left.value.toList.shouldEqual(List(parseError, parseError, parseError))
      }
    }

    "it processes some messages unsuccessfully" should {

      "return a list with those errors" in {

        val event = mock[SQSEvent]

        when(event.getRecords).thenReturn(util.Arrays.asList(invalidMessage, message, invalidMessage))

        lambdaService.processEvent(event).toEither.left.value.toList.shouldEqual(List(parseError, parseError))
      }
    }
  }
}
