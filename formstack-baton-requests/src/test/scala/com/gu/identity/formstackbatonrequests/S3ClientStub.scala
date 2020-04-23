package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.aws.{S3Client, S3CompletedPathFound, S3FailedPathFound, S3NoResultsFound, S3StatusResponse, S3WriteSuccess}

class S3ClientStub (
  checkForResultsResponse: Either[Throwable, S3StatusResponse],
  writeSuccessResultsResponse: Either[Throwable, S3WriteSuccess],
  writeFailedResultsResponse: Either[Throwable, S3WriteSuccess],
  copyResultsResponse: Either[Throwable, S3WriteSuccess]
) extends S3Client {
  override def checkForResults(initiationId: String, config: SarLambdaConfig): Either[Throwable, S3StatusResponse] = checkForResultsResponse
  override def writeSuccessResult(initiationId: String, results: List[FormstackSubmissionQuestionAnswer], config: PerformSarLambdaConfig): Either[Throwable, S3WriteSuccess] = writeSuccessResultsResponse
  override def writeFailedResults(initiationId: String, err: String, config: PerformSarLambdaConfig): Either[Throwable, S3WriteSuccess] = writeFailedResultsResponse
  override def copyResultsToCompleted(initiationId: String, config: PerformSarLambdaConfig): Either[Throwable, S3WriteSuccess] = copyResultsResponse
}

object S3ClientStub {
  val successfulPathFoundResponse: Either[Throwable, S3StatusResponse] = Right(S3CompletedPathFound(List("s3Location")))
  val failedPathFoundResponse: Either[Throwable, S3StatusResponse] = Right(S3FailedPathFound())
  val noResultFoundResponse: Either[Throwable, S3StatusResponse] = Right(S3NoResultsFound())

  val successfullyWroteToS3Response = Right(S3WriteSuccess())
  val failedToWriteToS3Response = Left(new Exception("S3 error"))

  def withSuccessResponse = new S3ClientStub(successfulPathFoundResponse, successfullyWroteToS3Response, successfullyWroteToS3Response, successfullyWroteToS3Response)
  def withFailedResponse = new S3ClientStub(failedPathFoundResponse, failedToWriteToS3Response, failedToWriteToS3Response, failedToWriteToS3Response)
  def withPendingStatusResponse = new S3ClientStub(noResultFoundResponse, successfullyWroteToS3Response, successfullyWroteToS3Response, successfullyWroteToS3Response)
}