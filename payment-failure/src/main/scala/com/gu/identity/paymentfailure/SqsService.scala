package com.gu.identity.paymentfailure

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{DeleteMessageRequest, DeleteMessageResponse}
import com.typesafe.scalalogging.StrictLogging
import io.circe.parser.decode
import cats.syntax.either._
import io.circe.Decoder
import io.circe.parser._

class SqsService(config: Config) extends StrictLogging {

  val sqsClient = SqsClient.builder()
    .credentialsProvider(DefaultCredentialsProvider.create())
    .region(Region.EU_WEST_1)
    .build()

  def parseMessage[A : Decoder](sqsMessage: SQSMessage): Either[Throwable, A] = {
    logger.info(s"attempting to parse message body ${sqsMessage.getBody}")
    for {
      jsonMessage <- parse(sqsMessage.getBody)
      body <- jsonMessage.hcursor.downField("Message").as[String]
      data <- decode[A](body)
    } yield data
  }

  def deleteMessage(message: SQSEvent.SQSMessage): Either[Throwable, DeleteMessageResponse] = {
    Either.catchNonFatal(
      sqsClient.deleteMessage(
        DeleteMessageRequest.builder()
          .queueUrl(config.queueURL)
          .receiptHandle(message.getReceiptHandle)
          .build()
      )
    )
  }

  def processDeleteMessageResult(deleteMessageResponse: DeleteMessageResponse): Either[Throwable, Unit] = {
    val statusCode = deleteMessageResponse.sdkHttpResponse().statusCode()
    if(statusCode == 200) Right(()) else Left(new Exception(s"Invalid status code, status code : $statusCode"))
  }
}
