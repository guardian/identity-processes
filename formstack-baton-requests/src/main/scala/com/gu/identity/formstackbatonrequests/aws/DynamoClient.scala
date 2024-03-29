package com.gu.identity.formstackbatonrequests.aws

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZoneOffset}
import com.typesafe.scalalogging.LazyLogging
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import com.gu.scanamo.Scanamo
import com.github.t3hnar.bcrypt._
import com.gu.scanamo.syntax._
import cats.implicits._
import com.amazonaws.services.dynamodbv2.model.{BatchWriteItemResult, DeleteItemResult, ProvisionedThroughput, UpdateTableRequest, UpdateTableResult}
import com.gu.identity.formstackbatonrequests.sar.SubmissionIdEmail

import scala.util.Try

trait DynamoClient {
  def writeSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): Either[Throwable, List[BatchWriteItemResult]]
  def mostRecentTimestamp(lastUpdatedTableName: String, accountNumber: Int): Either[Throwable, SubmissionTableUpdateDate]
  def updateMostRecentTimestamp(lastUpdatedTableName: String, accountNumber: Int, newTimestamp: LocalDateTime): Either[Throwable, Unit]
  def userSubmissions(email: String, salt: String, submissionsTableName: String): Either[Throwable, List[SubmissionIdEmail]]
  def deleteUserSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): Either[Throwable, List[DeleteItemResult]]
}

case class SubmissionTableUpdateDate(formstackSubmissionTableMetadata: String, date: String) {
  def asLocalTimestamp: LocalDateTime = LocalDateTime.parse(date, DateHelpers.formatter)

}

object DateHelpers {
  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  /**
   * Converts an utc localdatetime into the time zone and format that the formstack API expects
   * @param utcLocalDateTime a local datetime that is assumed to be in UTC time zone
   * @return the provided utc datetime but translated into eastern time zone and formatted as a string in the format "yyyy-MM-dd HH:mm:ss". This is what the formstack API expects as an input
   */
  def toEasternTimeString(utcLocalDateTime: LocalDateTime): String = {
    val utcZonedDateTime = utcLocalDateTime.atZone(ZoneOffset.UTC)
    utcZonedDateTime.withZoneSameInstant(ZoneId.of("America/New_York")).format(formatter)
  }
}

case class Dynamo(dynamoClient: AmazonDynamoDB = Dynamo.defaultDynamoClient) extends DynamoClient with LazyLogging {

  override def writeSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): Either[Throwable, List[BatchWriteItemResult]] = {
    val hashedEmailsOrError = submissionIdsAndEmails.traverse { submissionIdAndEmail =>
      for {
        hashedEmail <- submissionIdAndEmail.email.bcryptSafe(salt).toEither
        submissionWithHashedEmail = submissionIdAndEmail.copy(email = hashedEmail)
      } yield submissionWithHashedEmail
    }

    hashedEmailsOrError.flatMap { submissionsWithHashedEmails =>
      Try(Scanamo.putAll[SubmissionIdEmail](dynamoClient)(submissionsTableName)(submissionsWithHashedEmails.toSet))
        .toEither
    }
  }

  override def mostRecentTimestamp(lastUpdatedTableName: String, accountNumber: Int): Either[Throwable, SubmissionTableUpdateDate] = {
    logger.info(s"retrieving most recent timestamp from $lastUpdatedTableName")
    Scanamo.get[SubmissionTableUpdateDate](dynamoClient)(lastUpdatedTableName)(
      'formstackSubmissionTableMetadata -> s"account${accountNumber}LastUpdated"
    ).map(dynamoResponse => dynamoResponse
      .left
      .map(err => new Exception(err.toString)))
      .getOrElse(Right(SubmissionTableUpdateDate(s"account${accountNumber}LastUpdated", "1970-01-01 00:00:00")))
  }

  override def updateMostRecentTimestamp(lastUpdatedTableName: String, accountNumber: Int, newTimestamp: LocalDateTime): Either[Throwable, Unit] = {
    val formattedDateTime = newTimestamp.format(DateHelpers.formatter)
    logger.info(s"updating most recent timestamp in $lastUpdatedTableName to $formattedDateTime")
    val recentUpdate = SubmissionTableUpdateDate(s"account${accountNumber}LastUpdated", formattedDateTime)
    Scanamo.put[SubmissionTableUpdateDate](dynamoClient)(lastUpdatedTableName)(recentUpdate)
      .getOrElse(Right())
      .fold(dynamoReadError => Left(new Exception(dynamoReadError.toString)), _ => Right(()))
  }

  override def userSubmissions(email: String, salt: String, submissionsTableName: String): Either[Throwable, List[SubmissionIdEmail]] = {
    logger.info(s"retrieving submissions from $submissionsTableName")
    for {
      hashedEmail <- email.bcryptSafe(salt).toEither
      queryResult <- Scanamo.query[SubmissionIdEmail](dynamoClient)(submissionsTableName)('email -> hashedEmail)
        .sequence.left.map(err => new Exception(err.toString))
    } yield queryResult
  }

  override def deleteUserSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): Either[Throwable, List[DeleteItemResult]] = {
    logger.info(s"deleting ${submissionIdsAndEmails.length} submissions from $submissionsTableName")
    submissionIdsAndEmails.traverse { submissionIdAndEmail =>
      Try(Scanamo
        .delete(dynamoClient)(submissionsTableName)
        ('email -> submissionIdAndEmail.email and 'submissionId -> submissionIdAndEmail.submissionId)).toEither
    }
  }
}

object Dynamo {
  val defaultDynamoClient: AmazonDynamoDB = AmazonDynamoDBClient
    .builder()
    .withRegion(AwsCredentials.region)
    .withCredentials(AwsCredentials.credentials)
    .build()
}
