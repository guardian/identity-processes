package com.gu.identity.formstackbatonrequests.aws

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{CannedAccessControlList, S3ObjectSummary}
import com.gu.identity.formstackbatonrequests.{InitLambdaConfig, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging
import com.gu.identity.formstackbatonrequests.BatonModels.{BatonRequestType, RER, SAR}
import com.gu.identity.formstackbatonrequests.sar.FormstackSubmissionQuestionAnswer

import scala.collection.JavaConverters._
import scala.util.Try

sealed trait StatusResponse
case class CompletedPathFound(resultLocations: List[String]) extends StatusResponse
case class FailedPathFound() extends StatusResponse
case class NoResultsFound() extends StatusResponse

case class S3WriteSuccess()


trait S3Client {
  def checkForResults(initiationId: String, requestType: BatonRequestType, config: InitLambdaConfig): Either[Throwable, StatusResponse]
  def writeSuccessResult(initiationId: String, results: List[FormstackSubmissionQuestionAnswer], requestType: BatonRequestType, config: PerformLambdaConfig): Either[Throwable, S3WriteSuccess]
  def writeFailedResults(initiationId: String, err: String, requestType: BatonRequestType, config: PerformLambdaConfig): Either[Throwable, S3WriteSuccess]
}

object S3 extends S3Client with LazyLogging {

  private val s3Client = AmazonS3Client
    .builder()
    .withRegion(AwsCredentials.region)
    .withCredentials(AwsCredentials.credentials)
    .build()

  private def listFolderContents(resultsBucket: String, path: String): Either[Throwable, List[S3ObjectSummary]] =
    Try {
      s3Client
        .listObjects(resultsBucket, path)
        .getObjectSummaries
        .asScala
        .toList
    }.toEither

  private def s3StatusResponse(
    failedResults: List[S3ObjectSummary],
    completedResults: List[S3ObjectSummary],
    resultsBucket: String
  ): StatusResponse = {
    val failedResultsExist = failedResults.nonEmpty
    val completedResultsExist = completedResults.nonEmpty

    (failedResultsExist, completedResultsExist) match {
      case (true, _) => FailedPathFound()
      case (false, true) =>
        val completedResultsPaths = completedResults
        .map(obj => s"s3://$resultsBucket/${obj.getKey}")
        CompletedPathFound(completedResultsPaths)
      case _ => NoResultsFound()
    }
  }

  private def generateResultsPath(
    configResultsPath: String,
    requestType: BatonRequestType,
    initiationId: String,
    status: String,
    objectName: Option[String] = None): String =
    objectName match {
      case Some(obj) => s"$configResultsPath/$requestType/$initiationId/$status/$obj"
      case None => s"$configResultsPath/$requestType/$initiationId/$status/"
    }

  override def checkForResults(initiationId: String, requestType: BatonRequestType, config: InitLambdaConfig): Either[Throwable, StatusResponse] = {
    val completedPath = generateResultsPath(config.resultsPath, requestType, initiationId, "completed")
    val failedPath = generateResultsPath(config.resultsPath, requestType, initiationId, "failed")

    for {
      completedResults <- listFolderContents(config.resultsBucket, completedPath)
      failedResults <- listFolderContents(config.resultsBucket, failedPath)
    } yield s3StatusResponse(failedResults, completedResults, config.resultsBucket)
  }

  private def writeToS3(resultsBucket: String, filePath: String, contents: String): Either[Throwable, S3WriteSuccess] = Try {
    s3Client.putObject(resultsBucket, filePath, contents)
  }.toEither.map(_ => S3WriteSuccess())

  private def formatResults(results: List[FormstackSubmissionQuestionAnswer]): String = {
    if (results.nonEmpty) {
      results.map { submission =>
        val meta = List(s"Submission: ${submission.id}", s"Timestamp: ${submission.timestamp}")
        val responses = submission.fields.map(field => s"${field.label}: ${field.value}")
        (meta ::: responses).mkString("\n")
      }.mkString("\n-------------------------\n")
    } else ""
  }

  override def writeSuccessResult(
    initiationId: String,
    results: List[FormstackSubmissionQuestionAnswer],
    requestType: BatonRequestType,
    config: PerformLambdaConfig): Either[Throwable, S3WriteSuccess] = {

    logger.info(s"Writing $requestType result to s3. ${results.length} results found.")

    val objectName = requestType match {
      case SAR => if (results.nonEmpty) "formstackSarResponse" else "noResultsFoundForUser"
      case RER => "rerCompleted"
    }

    val filePath = generateResultsPath(config.resultsPath, requestType, initiationId, "completed", Some(objectName))
    writeToS3(config.resultsBucket, filePath, formatResults(results))
  }

  override def writeFailedResults(
    initiationId: String,
    err: String,
    requestType: BatonRequestType,
    config: PerformLambdaConfig): Either[Throwable, S3WriteSuccess] = {
    logger.info("Writing to failed path in s3.")
    val filePath = generateResultsPath(config.resultsPath, requestType, initiationId, "failed", Some(s"formstack${requestType}Failed"))
    writeToS3(config.resultsBucket, filePath, err)
  }
}
