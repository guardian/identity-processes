package com.gu.identity.formstackbatonrequests.aws

import java.time.LocalDateTime

import com.amazonaws.services.dynamodbv2.model.{BatchWriteItemResult, DeleteItemResult, UpdateTableResult}
import com.gu.identity.formstackbatonrequests.sar.SubmissionIdEmail

class DynamoClientStub(
  mostRecentTimestampResponse: Either[Throwable, SubmissionTableUpdateDate],
  updateMostRecentTimestampResponse: Either[Throwable, Unit],
  writeSubmissionsResponse: Either[Throwable, List[BatchWriteItemResult]],
  userSubmissionsResponse: Either[Throwable, List[SubmissionIdEmail]],
  deleteUserSubmissionsResponse: Either[Throwable, List[DeleteItemResult]],
  updateTableResponse: Either[Throwable, UpdateTableResult]
) extends DynamoClient {
  override def mostRecentTimestamp(lastUpdatedTableName: String, accountNumber: Int): Either[Throwable, SubmissionTableUpdateDate] = mostRecentTimestampResponse
  override def updateMostRecentTimestamp(lastUpdatedTableName: String, accountNumber: Int, currentDateTime: LocalDateTime): Either[Throwable, Unit] = updateMostRecentTimestampResponse
  override def writeSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): Either[Throwable, List[BatchWriteItemResult]] = writeSubmissionsResponse
  override def userSubmissions(email: String, salt: String, submissionsTableName: String): Either[Throwable, List[SubmissionIdEmail]] = userSubmissionsResponse
  override def deleteUserSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): Either[Throwable, List[DeleteItemResult]] = deleteUserSubmissionsResponse
  override def updateWriteCapacity(units: Long, submissionsTableName: String): Either[Throwable, UpdateTableResult] = updateTableResponse
}

object DynamoClientStub {
  val mostRecentTimestampSuccess = Right(SubmissionTableUpdateDate("lastUpdated", "2020-01-01 00:00:00"))
  val updateMostRecentTimestampSuccess = Right(())
  val writeSubmissionsSuccess = Right(List.empty[BatchWriteItemResult])
  val userSubmissionsSuccess = Right(List(SubmissionIdEmail("test@test.com", "submissionId", 12345678, 1)))
  val deleteUserSubmissionsSuccess = Right(List.empty[DeleteItemResult])
  val updateWriteCapacitySuccess = Right(new UpdateTableResult())
  val genericDynamoFailure = Left(new Exception("DynamoDB error"))

  def withFailedResponse = new DynamoClientStub(genericDynamoFailure, genericDynamoFailure, genericDynamoFailure, genericDynamoFailure, genericDynamoFailure, genericDynamoFailure)
  def withSuccessResponse = new DynamoClientStub(mostRecentTimestampSuccess, updateMostRecentTimestampSuccess, writeSubmissionsSuccess, userSubmissionsSuccess, deleteUserSubmissionsSuccess, updateWriteCapacitySuccess)
}
