package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.aws.{S3Client, S3CompletedPathFound, S3FailedPathFound, S3NoResultsFound, S3StatusResponse}

class S3ClientStub (
  checkForResultsResponse: Either[Throwable, S3StatusResponse]
) extends S3Client {
  override def checkForResults(initiationId: String, config: SarLambdaConfig): Either[Throwable, S3StatusResponse] = checkForResultsResponse
}

object S3ClientStub {
  val successfulPathFoundResponse: Either[Throwable, S3StatusResponse] = Right(S3CompletedPathFound(List("s3Location")))
  val failedPathFoundResponse: Either[Throwable, S3StatusResponse] = Right(S3FailedPathFound())
  val noResultFoundResponse: Either[Throwable, S3StatusResponse] = Right(S3NoResultsFound())

  def withSuccessResponse = new S3ClientStub(successfulPathFoundResponse)
  def withFailedResponse = new S3ClientStub(failedPathFoundResponse)
  def withPendingResponse = new S3ClientStub(noResultFoundResponse)
}
