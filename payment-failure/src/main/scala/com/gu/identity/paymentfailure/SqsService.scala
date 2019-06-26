package com.gu.identity.paymentfailure

import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, DeleteMessageResult}
import com.typesafe.scalalogging.StrictLogging
import io.circe.parser.decode
import cats.syntax.either._
import io.circe.Decoder
import io.circe.parser._

class SqsService(config: Config) extends StrictLogging {

  val credentialsProvider = new AWSCredentialsProviderChain(
    new DefaultAWSCredentialsProviderChain,
    new InstanceProfileCredentialsProvider(false)
  )

  val sqsClient = AmazonSQSClientBuilder.standard().withCredentials(credentialsProvider).withRegion("eu-west-1").build()

  def parseMessage[A : Decoder](sqsMessage: SQSMessage): Either[Throwable, A] = {
    logger.info(s"attempting to parse message body ${sqsMessage.getBody}")
    for {
      jsonMessage <- parse(sqsMessage.getBody)
      body <- jsonMessage.hcursor.downField("Message").as[String]
      data <- decode[A](body)
    } yield data
  }

  def deleteMessage(message: SQSEvent.SQSMessage): Either[Throwable, DeleteMessageResult] = {
    Either.catchNonFatal(sqsClient.deleteMessage(new DeleteMessageRequest(config.queueURL, message.getReceiptHandle)))
  }

  def processDeleteMessageResult(deleteMessageResult : DeleteMessageResult): Either[Throwable, Unit] = {
    val statusCode = deleteMessageResult.getSdkHttpMetadata.getHttpStatusCode
    if(statusCode == 200) Right(()) else Left(new Exception(s"Invalid status code, status code : $statusCode"))
  }
}
