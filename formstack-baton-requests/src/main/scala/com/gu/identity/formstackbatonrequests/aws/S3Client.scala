package com.gu.identity.formstackbatonrequests.aws

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{CannedAccessControlList, S3ObjectSummary}
import com.gu.identity.formstackbatonrequests.{FormstackSubmissionQuestionAnswer, PerformSarLambdaConfig, SarLambdaConfig}
import com.typesafe.scalalogging.LazyLogging
import cats.implicits._

import scala.collection.JavaConverters._
import scala.util.Try

sealed trait S3StatusResponse
case class S3CompletedPathFound(resultLocations: List[String]) extends S3StatusResponse
case class S3FailedPathFound() extends S3StatusResponse
case class S3NoResultsFound() extends S3StatusResponse

case class S3WriteSuccess()


trait S3Client {
  def checkForResults(initiationId: String, config: SarLambdaConfig): Either[Throwable, S3StatusResponse]
  def writeSuccessResult(initiationId: String, results: List[FormstackSubmissionQuestionAnswer], config: PerformSarLambdaConfig): Either[Throwable, S3WriteSuccess]
  def writeFailedResults(initiationId: String, err: String, config: PerformSarLambdaConfig): Either[Throwable, S3WriteSuccess]
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
  ): S3StatusResponse = {
    val failedResultsExist = failedResults.nonEmpty
    val completedResultsExist = completedResults
      .exists(obj => obj.getKey.contains("ResultsCompleted") | obj.getKey.contains("NoResultsFoundForUser"))

    (failedResultsExist, completedResultsExist) match {
      case (true, _) => S3FailedPathFound()
      case (false, true) =>
        val completedResultsPaths = completedResults
        .filterNot(obj => obj.getKey.contains("ResultsCompleted"))
        .map(obj => s"s3://$resultsBucket/${obj.getKey}")
        S3CompletedPathFound(completedResultsPaths)
      case _ => S3NoResultsFound()
    }
  }

  override def checkForResults(initiationId: String, config: SarLambdaConfig): Either[Throwable, S3StatusResponse] = {
    val completedPath = s"${config.resultsPath}/$initiationId/completed/"
    val failedPath = s"${config.resultsPath}/$initiationId/failed/"

    for {
      completedResults <- listFolderContents(config.resultsBucket, completedPath)
      failedResults <- listFolderContents(config.resultsBucket, failedPath)
    } yield s3StatusResponse(failedResults, completedResults, config.resultsBucket)
  }

  private def writeToS3(resultsBucket: String, filePath: String, contents: String): Either[Throwable, S3WriteSuccess] = Try {
    s3Client.putObject(resultsBucket, filePath, contents)
    s3Client.setObjectAcl(resultsBucket, filePath, CannedAccessControlList.BucketOwnerRead)
  }.toEither.map(_ => S3WriteSuccess())

  private def formatResults(results: List[FormstackSubmissionQuestionAnswer]): String = {
    results.map { submission =>
      val meta = List(s"Submission: ${submission.id}", s"Timestamp: ${submission.timestamp}")
      val responses = submission.fields.map(field => s"${field.label}: ${field.value}")
      (meta ::: responses).mkString("\n")
    }.mkString("\n-------------------------\n")
  }

  override def writeSuccessResult(
    initiationId: String,
    results: List[FormstackSubmissionQuestionAnswer],
    config: PerformSarLambdaConfig): Either[Throwable, S3WriteSuccess] = {
    val resultsPath = s"${config.resultsPath}/$initiationId/completed"
    if (results.nonEmpty) {
      val filePath = s"$resultsPath/formstackSarResponse"
      logger.info("Writing SAR result to s3.")
      val contents = formatResults(results)
      writeToS3(config.resultsBucket, filePath, contents)
    } else {
      val filePath = s"$resultsPath/noResultsFoundForUser"
      logger.info(s"No results found for request $initiationId. Creating NoResultsFoundForUser object.")
      writeToS3(config.resultsBucket, filePath, "")
    }
  }

  override def writeFailedResults(
    initiationId: String,
    err: String,
    config: PerformSarLambdaConfig): Either[Throwable, S3WriteSuccess] = {
    val filePath = s"${config.resultsPath}/$initiationId/failed/formstackSarFailed"
    logger.info("Writing to failed path in s3.")
    writeToS3(config.resultsBucket, filePath, err)
  }
}
