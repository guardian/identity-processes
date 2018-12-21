package com.gu.identity.paymentfailure

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, DeleteMessageResult}
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.generic.semiauto.deriveDecoder
import cats.syntax.either._
import io.circe.parser._

class SqsService extends StrictLogging {

  def parseSingleMessage(sqsMessage: SQSMessage): Either[Throwable, IdentityBrazeEmailData] = {
    logger.info(s"attempting to parse message body ${sqsMessage.getBody}")
    implicit val identityBrazeEmailDataDecoder: Decoder[IdentityBrazeEmailData] = deriveDecoder[IdentityBrazeEmailData]
    for {
      jsonMessage <- parse(sqsMessage.getBody)
      body <- jsonMessage.hcursor.downField("Message").as[String]
      identityEmailData <- decode[IdentityBrazeEmailData](body)
    } yield identityEmailData
  }

  def deleteMessage(message: SQSEvent.SQSMessage, config: Config): Either[Throwable, DeleteMessageResult] = {

    val credentialsProvider = new AWSCredentialsProviderChain(
      new DefaultAWSCredentialsProviderChain,
      new InstanceProfileCredentialsProvider(false)
    )
    val sqsClient = AmazonSQSClientBuilder.standard().withCredentials(credentialsProvider).withRegion("eu-west-1").build()
    Either.catchNonFatal(sqsClient.deleteMessage(new DeleteMessageRequest(config.queueURL, message.getReceiptHandle)))
  }

  def processDeleteMessageResult(deleteMessageResult : DeleteMessageResult): Either[Throwable, Unit] = {
    val statusCode = deleteMessageResult.getSdkHttpMetadata.getHttpStatusCode
    if(statusCode == 200) Right(()) else Left(new Exception(s"Invalid status code, status code : $statusCode"))
  }
}
