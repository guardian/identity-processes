package com.gu.identity.paymentfailure

import cats.data.NonEmptyList
import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import cats.syntax.either._
import com.gu.identity.paymentfailure.abtest.AutoSignInTestPreRelease

object Lambda extends StrictLogging {

  def getEnvironmentVariable(env: String): Either[Throwable, String] =
    Either.fromOption(
      Option(System.getenv(env)),
      new Exception(s"environment variable $env not defined")
    )

  def getConfig: Either[Throwable, Config] = {
    for {
      idapiHost <- getEnvironmentVariable("idapiHost")
      brazeApiHost <- getEnvironmentVariable("brazeApiHost")
      idapiAccessToken <- getEnvironmentVariable("idapiAccessToken")
      sqsQueueUrl <- getEnvironmentVariable("sqsQueueUrl")
      brazeApiKey <- getEnvironmentVariable("brazeApiKey")
    } yield {
      Config(idapiHost, brazeApiHost, idapiAccessToken, sqsQueueUrl, brazeApiKey)
    }
  }

  def handler(event: SQSEvent, context: Context): Unit = {

    LambdaService.setAWSRequestId(context)

    logger.info(s"received ${event.getRecords.size} messages - context: $context - event: $event")

    logger.info("initialising config and lambda service")
    val config = getConfig.valueOr { err =>
      logger.error("unable to get config", err)
      throw err
    }

    // Currently running the encrypted email test
    // TODO: switch to auto sign token test when pre release is finished
    // TODO: switch back to using DefaultBrazeEmailService when auto sign in token test is finished
    val identityClient = new IdentityClient(config)
    val autoSignInTokenTestPreRelease = new AutoSignInTestPreRelease(identityClient)
    val lambdaService = LambdaService.withAbTest(config, autoSignInTokenTestPreRelease)

    logger.info("config and services successfully initialised - processing events")
    lambdaService.processEvent(event)
      .fold(
        // If there are any errors in processing the event, throw an exception.
        // In which case, AWS will automatically put any messages handled by the respective lambda invocation
        // back on the queue (so they can be retried), or dead letter queue if max retries have been exceeded.
        // Note that messages that have been successfully processed won't be put back on the queue,
        // since we explicitly delete them from the queue (see LambdaService::processMessage()).
        logAndThrowErrors,
        _ => logger.info("event successfully processed")
      )
  }

  // Utility class for wrapping any errors returned from LambdaService::processEvent()
  // Facilitates throwing an error which includes everything that has gone wrong.
  case class Error(messageErrors: NonEmptyList[Throwable]) extends Exception {
    override val getMessage: String = {
      val messages = messageErrors.toList.map(_.getMessage)
      s"Lambda.Error: ${messages.mkString(" and ")}"
    }
  }

  def logAndThrowErrors(errors: NonEmptyList[Throwable]): Unit = {
    logger.error("error(s) occurred whilst processing event")
    errors.zipWithIndex.toList.foreach { case (err, i) =>
      // Log each error separately rather than building composite error message as string.
      // Easier to get full information about each error (cause, stack trace etc).
      logger.error(s"error ${i + 1}", err)
    }
    throw Error(errors)
  }
}
