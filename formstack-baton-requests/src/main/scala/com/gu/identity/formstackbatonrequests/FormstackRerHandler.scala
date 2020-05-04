package com.gu.identity.formstackbatonrequests

import java.util.UUID.randomUUID

import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.aws._
import com.typesafe.scalalogging.LazyLogging

case class FormstackRerHandler(s3Client: S3Client, lambdaClient: LambdaClient, rerHandlerConfig: InitLambdaConfig)
  extends LazyLogging
    with FormstackHandler[RerRequest, RerResponse] {

  private def initiate(request: RerInitiateRequest): Either[Throwable, RerInitiateResponse] = {
    val initiationReference = randomUUID.toString

    val performRerRequest = RerPerformRequest(
      initiationReference,
      request.subjectEmail,
      "formstack"
    )

    logger.info(s"invoking FormstackPerformRerLambda with initiation reference: $initiationReference")
    lambdaClient.invokeLambda(performRerRequest, rerHandlerConfig)
      .map(_ => RerInitiateResponse(initiationReference))
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

  override def handle(request: RerRequest): Either[Throwable, RerResponse] =
    request match {
      case r: RerInitiateRequest => initiate(r)
      case RerStatusRequest(initiationReference) => status(initiationReference)
    }
}
