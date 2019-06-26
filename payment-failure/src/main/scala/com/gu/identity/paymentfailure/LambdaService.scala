package com.gu.identity.paymentfailure

import cats.data.ValidatedNel
import cats.implicits._
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.gu.identity.paymentfailure.abtest.VariantGenerator
import org.slf4j.MDC

import scala.collection.JavaConverters._

class LambdaService(sqsService: SqsService, brazeEmailService: BrazeEmailService) {

  def processMessage(message: SQSMessage): Either[Throwable, BrazeResponse] =
    for {
      emailData <- sqsService.parseMessage[IdentityBrazeEmailData](message)
      brazeResponse <- brazeEmailService.sendEmail(emailData)
      // Deleting a message from the queue will mean that even if the lambda throws an error
      // to signify that not all messages in the event have been processed successfully,
      // messages that have been processed successfully will not be put back on the queue.
      result <- sqsService.deleteMessage(message)
      _ <- sqsService.processDeleteMessageResult(result)
    } yield brazeResponse

  // Returns a list of errors if there has been at least one error processing the messages.
  // Otherwise returns a list of the successful Braze responses.
  def processEvent(event: SQSEvent): ValidatedNel[Throwable, List[BrazeResponse]] =
    event.getRecords.asScala.toList
      .traverse[ValidatedNel[Throwable, ?], BrazeResponse] { message =>
        processMessage(message).toValidatedNel
      }
}

object LambdaService {

  def default(config: Config): LambdaService = {
    val identityClient = new IdentityClient(config)
    val sqsService = new SqsService(config)
    val brazeClient = new BrazeClient(config)
    val sendEmailService = new DefaultBrazeEmailService(identityClient, brazeClient, config)
    new LambdaService(sqsService, sendEmailService)
  }

  // Creates a lambda service which can be used to run an AB test.
  // Test that is run is dependant on the VariantGenerator that is injected.
  def withAbTest(config: Config, variantGenerator: VariantGenerator): LambdaService = {
    val sqsService = new SqsService(config)
    val brazeClient = new BrazeClient(config)
    val sendEmailService = new BrazeEmailServiceWithAbTest(brazeClient, variantGenerator, config)
    new LambdaService(sqsService, sendEmailService)
  }

  // Utility method to set the value of AWSRequestId in the mapped diagnostic context.
  // Means that the AWS request id will be included in any log entries.
  // See logback.xml and https://logback.qos.ch/manual/layouts.html for more details.
  def setAWSRequestId(context: Context): Unit =
    MDC.put("AWSRequestId", context.getAwsRequestId)
}

