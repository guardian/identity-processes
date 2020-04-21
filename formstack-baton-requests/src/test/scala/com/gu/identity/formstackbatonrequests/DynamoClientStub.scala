package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, SubmissionTableUpdateDate}

class DynamoClientStub(
  mostRecentTimestampResponse: Either[Throwable, SubmissionTableUpdateDate],
  updateMostRecentTimestampResponse: Either[Throwable, Unit],
  writeSubmissionsResponse: List[Either[Throwable, Unit]]
) extends DynamoClient {
  override def mostRecentTimestamp(): Either[Throwable, SubmissionTableUpdateDate] = mostRecentTimestampResponse
  override def updateMostRecentTimestamp(): Either[Throwable, Unit] = updateMostRecentTimestampResponse
  override def writeSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String): List[Either[Throwable, Unit]] = writeSubmissionsResponse
}

object DynamoClientStub {
  val mostRecentTimestampSuccess = Right(SubmissionTableUpdateDate("lastUpdated", "2020-01-01 00:00:00"))
  val mostRecentTimestampFailure = Left(new Exception("formstackSubmissionTableMetadata not found."))
  val updateMostRecentTimestampSuccess = Right(())
  val updateMostRecentTimestampFailure = Left(new Exception("unable to update most recent timestamp."))
  val writeSubmissionsSuccess = List(Right(()), Right(()))
  val writeSubmissionsFailure = List(Left(new Exception("unable to write submission to Dynamo.")), Left(new Exception("unable to write submission to Dynamo.")))

  def withFailedResponse = new DynamoClientStub(mostRecentTimestampFailure, updateMostRecentTimestampFailure, writeSubmissionsFailure)
  def withSuccessResponse = new DynamoClientStub(mostRecentTimestampSuccess, updateMostRecentTimestampSuccess, writeSubmissionsSuccess)
}
