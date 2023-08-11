package com.gu.identity.formstackbatonrequests.updatedynamo

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, SAR, UpdateDynamoRequest, UpdateDynamoResponse}
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, DynamoClientStub, S3ClientStub, SubmissionTableUpdateDate}
import com.gu.identity.formstackbatonrequests.services.{DynamoUpdateService, UpdateStatus}
import java.time.Duration
import com.gu.identity.formstackbatonrequests.{FormstackAccountToken, PerformLambdaConfig}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}

import java.time.LocalDateTime


class UpdateDynamoHandlerSpec extends FreeSpec with Matchers with MockFactory {
  trait Fixture {
    val currentDate = LocalDateTime.of(2023,8,13,13,45,0)
    // the last updated date before execution is "yesterday"
    val DynamoLastUpdatedBeforeExecution = SubmissionTableUpdateDate("metadata", "2023-08-12 13:45:00")


    val tenMinutesInMillis = 900000

    val context = mock[Context]

    val mockConfig: PerformLambdaConfig =
    PerformLambdaConfig(
      "resultsBucket",
      "resultsPath",
      "encryptionPassword",
      FormstackAccountToken(1, "accountOneToken"),
      "bcryptSalt",
      "submissions-table-name",
      "last-updated-table-name"
    )

    val dynamoClient = mock[DynamoClient]


    val dynamoUpdateService = mock[DynamoUpdateService]
    val handler = UpdateDynamoHandler(
        dynamoClient,
        S3ClientStub.withSuccessResponse,
        dynamoUpdateService,
        mockConfig,
        getCurrentTimestamp = {() => currentDate}
        )

  val request = UpdateDynamoRequest(
    requestType = SAR,
    initiationReference = "someInitiationReferenceValue",
    subjectEmail = "some@email.com",
    dataProvider = "someProvider",
    accountNumber = Some(1),
    formPage = 1,
    count = 25,
    timeOfStart =  currentDate,
    //how many seconds worth of data since the last updated time we will process None means no limit, just bring dynamodb up to date
    maxUpdateSeconds = None
    )
  }


  "should throw exception if account is not provided" in new Fixture {
    val requestWithoutAccount = request.copy(accountNumber = None)
     val thrownException = intercept[RuntimeException] {
       handler.handle(requestWithoutAccount, null)
     }
    thrownException.getMessage shouldBe("Unable to retrieve account number from UpdateDynamoRequest")
  }
  //this is legacy code from when there were 2 formstack accounts, now it just expect the account to always be one
  // ideally we will refactor the code to remove all remaining references to account number as we only have one
  "should throw exception if account provided is not 1" in new Fixture{
    val requestWithWrongAccount = request.copy(accountNumber = Some(7))
     val thrownException = intercept[RuntimeException] {
       handler.handle(requestWithWrongAccount, null)
     }
    thrownException.getMessage shouldBe("Unexpected account number: 7")
  }
  //As far as I can tell startime represents the time the process starts, but for some reason we trust the client to provide this as an input param
  "should throw exception if provided timeofStart is more than 10 seconds in the future " in new Fixture{
    val futureDate = currentDate.plusMinutes(2)
    val requestWithFutureTimeOfStart = request.copy(timeOfStart = futureDate)
     val thrownException = intercept[RuntimeException] {
       handler.handle(requestWithFutureTimeOfStart, null)
     }
    thrownException.getMessage shouldBe(s"Invalid timeOfStart: $futureDate is in the future")
  }

  "should run formstack api calls without maxtime and use timeofstart as last update time if maxseconds not provided in request" in new Fixture {
    //not strictly necessary because it is already None but just for clarity
    val requestWithoutMaxSeconds = request.copy(maxUpdateSeconds = None)
    (dynamoClient.mostRecentTimestamp _).expects("last-updated-table-name", 1).returning(Right(DynamoLastUpdatedBeforeExecution))
    (dynamoClient.updateMostRecentTimestamp _).expects(mockConfig.lastUpdatedTableName, 1, currentDate).returning(Right(()))

    (dynamoUpdateService.updateSubmissionsTable _).expects(1, DynamoLastUpdatedBeforeExecution.asLocalTimestamp, None, 25, mockConfig.accountOneToken, context).returning(Right(UpdateStatus(completed = true, None, None, mockConfig.accountOneToken)))

    handler.handle(requestWithoutMaxSeconds, context) shouldBe Right(
      UpdateDynamoResponse(Completed, requestWithoutMaxSeconds.initiationReference, requestWithoutMaxSeconds.subjectEmail, requestWithoutMaxSeconds.dataProvider, 1, None, None, requestWithoutMaxSeconds.requestType, requestWithoutMaxSeconds.timeOfStart)
    )
  }


    "should calculate maxdate, use to limit formstack response and save it as most recent timestamp if maxSeconds is provided in request" in new Fixture {
    val request5MinUpdate = request.copy(maxUpdateSeconds = Some(300) )

    val expectedMaxDate = DynamoLastUpdatedBeforeExecution.asLocalTimestamp.plusMinutes(5)

    (dynamoClient.mostRecentTimestamp _).expects("last-updated-table-name", 1).returning(Right(DynamoLastUpdatedBeforeExecution))
    (dynamoClient.updateMostRecentTimestamp _).expects(mockConfig.lastUpdatedTableName, 1, expectedMaxDate).returning(Right(()))

    (dynamoUpdateService.updateSubmissionsTable _).expects(1, DynamoLastUpdatedBeforeExecution.asLocalTimestamp, Some(expectedMaxDate), 25, mockConfig.accountOneToken, context).returning(Right(UpdateStatus(completed = true, None, None, mockConfig.accountOneToken)))

    handler.handle(request5MinUpdate, context) shouldBe Right(
      UpdateDynamoResponse(Completed, request5MinUpdate.initiationReference, request5MinUpdate.subjectEmail, request5MinUpdate.dataProvider, 1, None, None, request5MinUpdate.requestType, request5MinUpdate.timeOfStart)
    )
  }
  // If the maxseconds applied to the last updated dates is a date in the future then we should ignore it: it will not limit anything and we should never save a future date as our last updated date
  "should ignore the maxSeconds param if the last updated date + maxseconds is a date after the execution started" in new Fixture {

    val secsBetweenLastUpdateAndExecutionStart = Duration.between(DynamoLastUpdatedBeforeExecution.asLocalTimestamp,request.timeOfStart).toSeconds.toInt

    val invalidMaxSecondsRequest = request.copy(maxUpdateSeconds = Some(secsBetweenLastUpdateAndExecutionStart + 1))
    (dynamoClient.mostRecentTimestamp _).expects("last-updated-table-name", 1).returning(Right(DynamoLastUpdatedBeforeExecution))
    (dynamoClient.updateMostRecentTimestamp _).expects(mockConfig.lastUpdatedTableName, 1, currentDate).returning(Right(()))

    (dynamoUpdateService.updateSubmissionsTable _).expects(1, DynamoLastUpdatedBeforeExecution.asLocalTimestamp, None, 25, mockConfig.accountOneToken, context).returning(Right(UpdateStatus(completed = true, None, None, mockConfig.accountOneToken)))

    handler.handle(invalidMaxSecondsRequest, context) shouldBe Right(
      UpdateDynamoResponse(
        Completed,
        invalidMaxSecondsRequest.initiationReference, invalidMaxSecondsRequest.subjectEmail, invalidMaxSecondsRequest.dataProvider, 1, None, None, invalidMaxSecondsRequest.requestType, invalidMaxSecondsRequest.timeOfStart)
    )
  }

}
