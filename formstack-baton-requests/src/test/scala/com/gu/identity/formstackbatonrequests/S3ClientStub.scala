package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.BatonRequestType
import com.gu.identity.formstackbatonrequests.aws.{CompletedPathFound, FailedPathFound, NoResultsFound, S3Client, S3WriteSuccess, StatusResponse}

class S3ClientStub (
  checkForResultsResponse: Either[Throwable, StatusResponse],
  writeSuccessResultsResponse: Either[Throwable, S3WriteSuccess],
  writeFailedResultsResponse: Either[Throwable, S3WriteSuccess],
) extends S3Client {
  override def checkForResults(initiationId: String, config: InitLambdaConfig): Either[Throwable, StatusResponse] = checkForResultsResponse
  override def writeSuccessResult(initiationId: String, results: List[FormstackSubmissionQuestionAnswer], requestType: BatonRequestType, config: PerformLambdaConfig): Either[Throwable, S3WriteSuccess] = writeSuccessResultsResponse
  override def writeFailedResults(initiationId: String, err: String, requestType: BatonRequestType, config: PerformLambdaConfig): Either[Throwable, S3WriteSuccess] = writeFailedResultsResponse
}

object S3ClientStub {
  val successfulPathFoundResponse: Either[Throwable, StatusResponse] = Right(CompletedPathFound(List("s3Location")))
  val failedPathFoundResponse: Either[Throwable, StatusResponse] = Right(FailedPathFound())
  val noResultFoundResponse: Either[Throwable, StatusResponse] = Right(NoResultsFound())

  val successfullyWroteToS3Response = Right(S3WriteSuccess())
  val failedToWriteToS3Response = Left(new Exception("S3 error"))

  def withSuccessResponse = new S3ClientStub(successfulPathFoundResponse, successfullyWroteToS3Response, successfullyWroteToS3Response)
  def withFailedResponse = new S3ClientStub(failedPathFoundResponse, failedToWriteToS3Response, failedToWriteToS3Response)
  def withPendingStatusResponse = new S3ClientStub(noResultFoundResponse, successfullyWroteToS3Response, successfullyWroteToS3Response)
}
