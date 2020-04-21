package com.gu.identity.formstackbatonrequests.aws

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

import com.gu.identity.formstackbatonrequests.SubmissionIdEmail
import com.typesafe.scalalogging.LazyLogging
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.gu.scanamo.Scanamo
import com.github.t3hnar.bcrypt._
import com.gu.scanamo.syntax._

trait DynamoClient {
  def writeSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): List[Either[Throwable, Unit]]
  def mostRecentTimestamp(lastUpdatedTableName: String): Either[Throwable, SubmissionTableUpdateDate]
  def updateMostRecentTimestamp(lastUpdatedTableName: String): Either[Throwable, Unit]
}

case class SubmissionTableUpdateDate(formstackSubmissionTableMetadata: String, date: String)

object SubmissionTableUpdateDate {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
}

object Dynamo extends DynamoClient with LazyLogging {

  private val dynamoClient = AmazonDynamoDBClient
    .builder()
    .withRegion(AwsCredentials.region)
    .withCredentials(AwsCredentials.credentials)
    .build()

  override def writeSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): List[Either[Throwable, Unit]] = {
    submissionIdsAndEmails.map { submissionIdAndEmail =>
      for {
        hashedEmail <- submissionIdAndEmail.email.bcryptSafe(salt).toEither
        submissionWithHashedEmail = submissionIdAndEmail.copy(email = hashedEmail)
        _ <- Scanamo.put[SubmissionIdEmail](dynamoClient)(submissionsTableName)(submissionWithHashedEmail)
          .getOrElse(Right())
          .fold(dynamoReadError => Left(new Exception(dynamoReadError.toString)),  _ => Right(()))
      } yield ()
    }
  }

  override def mostRecentTimestamp(lastUpdatedTableName: String): Either[Throwable, SubmissionTableUpdateDate] = {
    logger.info(s"retrieving most recent timestamp from $lastUpdatedTableName")
    Scanamo.get[SubmissionTableUpdateDate](dynamoClient)(lastUpdatedTableName)(
      'formstackSubmissionTableMetadata -> "lastUpdated"
    ).map(dynamoResponse => dynamoResponse
      .left
      .map(err => new Exception(err.toString)))
      .getOrElse(Left(new Exception("formstackSubmissionTableMetadata not found.")))
  }

  override def updateMostRecentTimestamp(lastUpdatedTableName: String): Either[Throwable, Unit] = {
    val currentDateTime = LocalDateTime.now.format(SubmissionTableUpdateDate.formatter)
    logger.info(s"updating most recent timestamp in $lastUpdatedTableName to $currentDateTime")
    val recentUpdate = SubmissionTableUpdateDate("lastUpdated", currentDateTime)
    Scanamo.put[SubmissionTableUpdateDate](dynamoClient)(lastUpdatedTableName)(recentUpdate)
      .getOrElse(Right())
      .fold(dynamoReadError => Left(new Exception(dynamoReadError.toString)),  _ => Right(()))
  }
}
