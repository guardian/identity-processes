package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import scala.collection.JavaConverters._


object Lambda extends StrictLogging {

  def handler(event: SQSEvent, context: Context): Unit = {

    logger.info(s"context :  $context")
    logger.info("=========SHAPE OF INPUT=========")
    logger.info(inputFromQueue(event))
  }

  private def inputFromQueue(sqsEvent: SQSEvent): String = {
    val messages = sqsEvent.getRecords.asScala.map(mes => mes).toList
    logger.info("messages")
    logger.info(messages.toString)
    messages.toString
  }
}
