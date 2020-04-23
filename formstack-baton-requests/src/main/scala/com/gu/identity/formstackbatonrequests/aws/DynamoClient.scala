package com.gu.identity.formstackbatonrequests.aws

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

import com.gu.identity.formstackbatonrequests.SubmissionIdEmail
import com.typesafe.scalalogging.LazyLogging
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.gu.scanamo.Scanamo
import com.github.t3hnar.bcrypt._
import com.gu.scanamo.syntax._
import cats.implicits._
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult

import scala.util.Try

trait DynamoClient {
  def writeSubmissions(submissionIdsAndEmails: List[SubmissionIdEmail], salt: String, submissionsTableName: String): Either[Throwable, List[BatchWriteItemResult]]
  def mostRecentTimestamp(lastUpdatedTableName: String): Either[Throwable, SubmissionTableUpdateDate]
  def updateMostRecentTimestamp(lastUpdatedTableName: String): Either[Throwable, Unit]
  def userSubmissions(email: String, salt: String, submissionsTableName: String): Either[Throwable, List[SubmissionIdEmail]]
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

    override def userSubmissions(email: String, salt: String, submissionsTableName: String): Either[Throwable, List[SubmissionIdEmail]] = {
      logger.info(s"retrieving submissions from $submissionsTableName")
      for {
        hashedEmail <- email.bcryptSafe(salt).toEither
        queryResult <- Scanamo.query[SubmissionIdEmail](dynamoClient)(submissionsTableName)('email -> hashedEmail)
          .sequence.left.map(err => new Exception(err.toString))
      } yield queryResult
    }
}
