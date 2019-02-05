package com.gu.identity.paymentfailure

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import org.slf4j.MDC

import scala.collection.JavaConverters._

class LambdaService private (sqsService: SqsService, sendEmailService: SendEmailService) {

  def processEvent(event: SQSEvent): List[Either[Throwable, BrazeResponse]] =
    event.getRecords.asScala.toList.map { message =>
      for {
        emailData <- sqsService.parseSingleMessage(message)
        brazeResponse <- sendEmailService.sendEmailWithAutoSignInLink(emailData)
        result <- sqsService.deleteMessage(message)
        _ <- sqsService.processDeleteMessageResult(result)
      } yield brazeResponse
    }
}

object LambdaService {

  def fromConfig(config: Config): LambdaService = {
    val identityClient = new IdentityClient(config)
    val sqsService = new SqsService(config)
    val brazeClient = new BrazeClient(config)
    val sendEmailService = new SendEmailService(identityClient, brazeClient, config)
    new LambdaService(sqsService, sendEmailService)
  }

  // Utility method to set the value of AWSRequestId in the mapped diagnostic context.
  // Means that the AWS request id will be included in any log entries.
  // See logback.xml and https://logback.qos.ch/manual/layouts.html for more details.
  def setAWSRequestId(context: Context): Unit =
    MDC.put("AWSRequestId", context.getAwsRequestId)
}

