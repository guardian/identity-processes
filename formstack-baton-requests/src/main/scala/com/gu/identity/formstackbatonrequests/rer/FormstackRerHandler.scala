package com.gu.identity.formstackbatonrequests.rer

import java.time.LocalDateTime
import java.util.UUID.randomUUID

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.aws._
import com.gu.identity.formstackbatonrequests.services.FormstackService
import com.gu.identity.formstackbatonrequests.{FormstackHandler, InitLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

case class FormstackRerHandler(s3Client: S3Client, lambdaClient: StepFunctionClient, rerHandlerConfig: InitLambdaConfig)
  extends LazyLogging
    with FormstackHandler[RerRequest, RerResponse] {

  private def initiate(request: RerInitiateRequest): Either[Throwable, RerInitiateResponse] = {
    val initiationReference = randomUUID.toString

    val updateDynamoRequest = UpdateDynamoRequest(
      RER,
      initiationReference,
      request.subjectEmail,
      "formstack",
      None,
      1,
      FormstackService.resultsPerPage,
      LocalDateTime.now
    )

    logger.info(s"invoking FormstackRer step function with initiation reference: $initiationReference")
    lambdaClient.startStepFunction(updateDynamoRequest, rerHandlerConfig)
      .map(_ => RerInitiateResponse(initiationReference, "PerformRerLambda invoked", Pending))
  }

  private def status(initiationReference: String): Either[Throwable, RerStatusResponse] = {
    logger.info(s"checking Formstack RER status for initiation reference: $initiationReference")
    s3Client.checkForResults(initiationReference, RER, rerHandlerConfig).map {
      case CompletedPathFound(resultLocations) =>
        val message = s"RER completed: completed RER results for initiation reference $initiationReference found in s3: $resultLocations"
        logger.info(message)
        RerStatusResponse(initiationReference, Completed, message)
      case FailedPathFound() =>
        val message = s"RER failed: failed path found in S3 for initiation reference $initiationReference. Please check FormstackPerformRerLambda logs"
        logger.info(message)
        RerStatusResponse(initiationReference, Failed, message)
      case NoResultsFound() =>
        val message = s"RER pending: no results found in S3 for initiation reference $initiationReference."
        logger.info(message)
        RerStatusResponse(initiationReference, Pending, message)
    }
  }

  override def handle(request: RerRequest, context: Context): Either[Throwable, RerResponse] =
    request match {
      case r: RerInitiateRequest => initiate(r)
      case RerStatusRequest(initiationReference) => status(initiationReference)
    }
}
