package com.gu.identity.formstackbatonrequests

import scala.collection.JavaConverters._
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.{InvokeRequest, InvokeResult}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.gu.identity.formstackbatonrequests.BatonModels.SarPerformRequest
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import circeCodecs._

import scala.util.Try

sealed trait S3StatusResponse
case class S3CompletedPathFound(resultLocations: List[String]) extends S3StatusResponse
case class S3FailedPathFound() extends S3StatusResponse
case class S3NoResultsFound() extends S3StatusResponse

package object aws {

  def credentials = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    new ProfileCredentialsProvider("identity"),
    new ProfileCredentialsProvider(),
    new InstanceProfileCredentialsProvider(false)
  )

  val region: Regions = Regions.EU_WEST_1
}

package object s3 {
  trait S3Client {
    def checkForResults(initiationId: String, config: SarLambdaConfig): Either[Throwable, S3StatusResponse]
  }

  object S3 extends S3Client with LazyLogging {

    private val s3Client = AmazonS3Client
      .builder()
      .withRegion(aws.region)
      .withCredentials(aws.credentials)
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
  }
}

package object lambda {
  trait LambdaClient {
    def invokeLambda(sarPerformRequest: SarPerformRequest, config: SarLambdaConfig): Either[Throwable, InvokeResult]
  }

  object Lambda extends LambdaClient with LazyLogging {
    private val lambdaClient = AWSLambdaClient
      .builder()
      .withRegion(aws.region)
      .withCredentials(aws.credentials)
      .build()

    override def invokeLambda(sarPerformRequest: SarPerformRequest, config: SarLambdaConfig): Either[Throwable, InvokeResult] = {
      val invokeRequest = new InvokeRequest()
        .withFunctionName(config.performSarFunctionName)
        .withPayload(sarPerformRequest.asJson.toString)

      Try(lambdaClient.invoke(invokeRequest)).toEither.left.map { err =>
        logger.error("unable to invoke FormstackPerformSarLambda", err)
        err
      }
    }
  }
}
