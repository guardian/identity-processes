package com.gu.identity.formstackbatonrequests

import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, SubmissionTableUpdateDate}

class DynamoClientStub(
  mostRecentTimestampResponse: Either[Throwable, SubmissionTableUpdateDate],
  updateMostRecentTimestampResponse: Either[Throwable, Unit],
  writeSubmissionsResponse: Either[Throwable, List[BatchWriteItemResult]]
) extends DynamoClient {
  override def mostRecentTimestamp(lastUpdatedTableName: String): Either[Throwable, SubmissionTableUpdateDate] = mostRecentTimestampResponse
  override def updateMostRecentTimestamp(lastUpdatedTableName: String): Either[Throwable, Unit] = updateMostRecentTimestampResponse
  override def writeSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): Either[Throwable, List[BatchWriteItemResult]] = writeSubmissionsResponse
}

object DynamoClientStub {
  val mostRecentTimestampSuccess = Right(SubmissionTableUpdateDate("lastUpdated", "2020-01-01 00:00:00"))
  val mostRecentTimestampFailure = Left(new Exception("formstackSubmissionTableMetadata not found."))
  val updateMostRecentTimestampSuccess = Right(())
  val updateMostRecentTimestampFailure = Left(new Exception("unable to update most recent timestamp."))
  val writeSubmissionsSuccess = Right(List.empty[BatchWriteItemResult])
  val writeSubmissionsFailure = Left(new Exception("Invalid table name"))

  def withFailedResponse = new DynamoClientStub(mostRecentTimestampFailure, updateMostRecentTimestampFailure, writeSubmissionsFailure)
  def withSuccessResponse = new DynamoClientStub(mostRecentTimestampSuccess, updateMostRecentTimestampSuccess, writeSubmissionsSuccess)
}
