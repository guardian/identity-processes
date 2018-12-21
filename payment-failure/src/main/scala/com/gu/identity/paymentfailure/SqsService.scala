package com.gu.identity.paymentfailure

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, DeleteMessageResult}
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.generic.semiauto.deriveDecoder
import cats.syntax.either._

class SqsService extends StrictLogging {

  def parseSingleMessage(sqsMessage: SQSMessage): Either[Throwable, IdentityBrazeEmailData] = {
    implicit val identityBrazeEmailDataDecoder: Decoder[IdentityBrazeEmailData] = deriveDecoder[IdentityBrazeEmailData]
    decode[IdentityBrazeEmailData](sqsMessage.getBody)
  }

  def deleteMessage(message: SQSEvent.SQSMessage, config: Config): Either[Throwable, DeleteMessageResult] = {

    val credentialsProvider = new AWSCredentialsProviderChain(
      new ProfileCredentialsProvider("identity"),
      new InstanceProfileCredentialsProvider(false)
    )
    val sqsClient = AmazonSQSClientBuilder.standard().withCredentials(credentialsProvider).build()
    Either.catchNonFatal(sqsClient.deleteMessage(new DeleteMessageRequest(config.queueURL, message.getReceiptHandle)))
  }

  def processDeleteMessageResult(deleteMessageResult : DeleteMessageResult): Either[Throwable, Unit] = {
    val statusCode = deleteMessageResult.getSdkHttpMetadata.getHttpStatusCode
    if(statusCode == 200) Right(()) else Left(new Exception(s"Invalid status code, status code : $statusCode"))
  }
}
