package com.gu.identity.formstackbatonrequests.sar

import java.time.LocalDateTime
import java.util.UUID.randomUUID

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.aws._
import com.gu.identity.formstackbatonrequests.services.FormstackService
import com.gu.identity.formstackbatonrequests.{FormstackHandler, InitLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

case class FormstackSarHandler(s3Client: S3Client, lambdaClient: StepFunctionClient, sarHandlerConfig: InitLambdaConfig)
  extends LazyLogging
    with FormstackHandler[SarRequest, SarResponse] {

  private def initiate(request: SarInitiateRequest): Either[Throwable, SarInitiateResponse] = {
    val initiationReference = randomUUID.toString

    val updateDynamoRequest = UpdateDynamoRequest(
      SAR,
      initiationReference,
      request.subjectEmail,
      "formstack",
      None,
      1,
      FormstackService.formResultsPerPage,
      LocalDateTime.now
    )

    logger.info(s"invoking FormstackSar step function with initiation reference: $initiationReference")
    lambdaClient.startStepFunction(updateDynamoRequest, sarHandlerConfig)
      .map(_ => SarInitiateResponse(initiationReference))
  }

  private def status(initiationReference: String): Either[Throwable, SarStatusResponse] = {
    logger.info(s"checking Formstack SAR status for initiation reference: $initiationReference")
    s3Client.checkForResults(initiationReference, SAR, sarHandlerConfig).map {
      case CompletedPathFound(resultLocations) =>
        logger.info(s"SAR completed: completed SAR results for initiation reference $initiationReference found in s3: $resultLocations")
        SarStatusResponse(Completed, Some(resultLocations))
      case FailedPathFound() =>
        logger.info(s"SAR failed: failed path found in S3 for initiation reference $initiationReference. Please check FormstackPerformSarLambda logs")
        SarStatusResponse(Failed)
      case NoResultsFound() =>
        logger.info(s"SAR pending: no results found in S3 for initiation reference $initiationReference.")
        SarStatusResponse(Pending)
    }
  }

  override def handle(request: SarRequest, context: Context): Either[Throwable, SarResponse] =
    request match {
      case r: SarInitiateRequest => initiate(r)
      case SarStatusRequest(initiationReference) => status(initiationReference)
    }
}
