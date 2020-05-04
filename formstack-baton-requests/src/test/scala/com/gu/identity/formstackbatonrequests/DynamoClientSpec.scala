package com.gu.identity.formstackbatonrequests

import java.time.LocalDateTime

import com.amazonaws.services.dynamodbv2.model.DeleteItemResult
import com.gu.identity.formstackbatonrequests.aws.{Dynamo, DynamoClient, SubmissionTableUpdateDate}
import org.scalatest.FreeSpec
import com.github.t3hnar.bcrypt._
import com.gu.identity.formstackbatonrequests.sar.SubmissionIdEmail
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, fixture}


class DynamoClientSpec extends FreeSpec with Matchers with BeforeAndAfterAll {
  val client: DynamoClient = Dynamo(LocalDynamoDB.client())
  val salt: String = generateSalt
  val submissionsTable = "submissions-table"
  val lastUpdatedTable = "last-updated-table"
  val submissionIdEmails =
    List(
      SubmissionIdEmail("test@test.com", "submissionId1", 1577836800, 1),
      SubmissionIdEmail("test2@test.com", "submissionId2", 1577836800, 1),
      SubmissionIdEmail("test3@test.com", "submissionId3", 1577836800, 1),
      SubmissionIdEmail("test@test.com", "submissionId4", 1577836800, 2)
    )

  def matchingSubmissions(email: String): Either[Throwable, List[SubmissionIdEmail]] =
    client.userSubmissions(email, salt, submissionsTable)

  "DynamoClient" - {

    "should successfully write `SubmissionIdEmail` to Dynamo" in {

      val response = client.writeSubmissions(submissionIdEmails, salt, submissionsTable)
      response.isRight shouldBe true
    }

    "should return an error when invalid salt provided to write `SubmissionIdEmail` to Dynamo" in {
      val invalidSalt = "invalidSalt"

      val response = client.writeSubmissions(submissionIdEmails, invalidSalt, submissionsTable)
      response.isLeft shouldBe true
      response.left.get.getMessage shouldBe "Invalid salt version"
    }

    "should get all `SubmissionIdEmail`s that match a given email address" in {

      val emailAddressForSar = "test@test.com"

      val submissions = matchingSubmissions(emailAddressForSar)

      submissions.isRight shouldBe true
      submissions.getOrElse(List.empty).length shouldBe 2
      submissions.getOrElse(List.empty).head.submissionId shouldBe "submissionId1"
    }

    "should return an empty list if no `SubmissionIddEmail`s match a given email address" in {

      val emailAddressForSar = "test4@test.com"

      val submissions = matchingSubmissions(emailAddressForSar)

      submissions.isRight shouldBe true
      submissions.getOrElse(List.empty).length shouldBe 0
    }

    "should delete all `SubmissionIdEmail`s that match a given email address" in {

      val emailAddressForRer = "test@test.com"
      val submissionsBeforeDeletion = matchingSubmissions(emailAddressForRer)

      submissionsBeforeDeletion.isRight shouldBe true
      submissionsBeforeDeletion.getOrElse(List.empty).length shouldBe 2

      val deleteSubmissions = client.deleteUserSubmissions(submissionsBeforeDeletion.getOrElse(List.empty), salt, submissionsTable)
      deleteSubmissions.isRight shouldBe true

      val submissionsAfterDeletion = matchingSubmissions(emailAddressForRer)
      submissionsAfterDeletion.isRight shouldBe true
      submissionsAfterDeletion.getOrElse(List.empty).length shouldBe 0
    }

    "should return an error when invalid salt provided to read `SubmissionIdEmail` from Dynamo" in {
      val invalidSalt = "invalidSalt"
      val emailAddressForSar = "test4@test.com"

      val matchingSubmissions = client.userSubmissions(emailAddressForSar, invalidSalt, submissionsTable)

      matchingSubmissions.isLeft shouldBe true
      matchingSubmissions.left.get.getMessage shouldBe "Invalid salt version"
    }

    "should update most recent timestamp in Dynamo" in {
      val dateTime = LocalDateTime.of(2020, 1, 1, 0, 0)
      val response = client.updateMostRecentTimestamp(lastUpdatedTable, dateTime)
      response.isRight shouldBe true
    }

    "should get mostRecentTimestamp" in {
      val lastUpdatedTimestamp = client.mostRecentTimestamp(lastUpdatedTable)

      lastUpdatedTimestamp.isRight shouldBe true
    }

    "should return an updated timestamp that is more recent that the last updated timestamp" in {
      val newDateTime = LocalDateTime.of(2020, 2, 1, 0, 0)
      val oldUpdatedTimestamp = client.mostRecentTimestamp(lastUpdatedTable)
      val updateTimestamp = client.updateMostRecentTimestamp(lastUpdatedTable, newDateTime)
      val newUpdatedTimestamp = client.mostRecentTimestamp(lastUpdatedTable)

      oldUpdatedTimestamp.isRight shouldBe true
      updateTimestamp.isRight shouldBe true
      oldUpdatedTimestamp.right.get shouldBe SubmissionTableUpdateDate("lastUpdated", "2020-01-01 00:00:00")
      newUpdatedTimestamp.isRight shouldBe true
      newUpdatedTimestamp.right.get shouldBe SubmissionTableUpdateDate("lastUpdated", "2020-02-01 00:00:00")
    }
  }

  override protected def beforeAll(): Unit = {
  import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
    val client = LocalDynamoDB.client()
    LocalDynamoDB.createTable(client)(submissionsTable)(
      'email -> S,
      'submissionId -> S
    )
    LocalDynamoDB.createTable(client)(lastUpdatedTable)(
      'formstackSubmissionTableMetadata -> S
    )
  }

  override protected def afterAll(): Unit = {
    LocalDynamoDB.client().deleteTable(submissionsTable)
    LocalDynamoDB.client().deleteTable(lastUpdatedTable)
  }
}

