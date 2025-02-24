package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.aws.{Dynamo, S3}
import com.gu.identity.formstackbatonrequests.circeCodecs._
import com.gu.identity.formstackbatonrequests.services.FormstackService
import com.gu.identity.formstackbatonrequests.updatedynamo.UpdateDynamoHandler
import io.circe.syntax._

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.time.{LocalDateTime, ZoneOffset}

/* This object can be used for local runs of the lambda, for end-to-end testing. */

object FormstackBatonUpdateDynamoLambdaLocalRun extends App {

  val formstackService = new FormstackService()
  case class InputOutputStreams(inputStream: ByteArrayInputStream, outputStream: ByteArrayOutputStream)

  private def requestStreams(request: BatonRequest): InputOutputStreams = {
    val jsonRequest = request match {
      case r: SarRequest => r.asJson.noSpaces
      case r: RerRequest => r.asJson.noSpaces
      case r: UpdateDynamoRequest => r.asJson.noSpaces
    }
    val testInputStream = new ByteArrayInputStream(jsonRequest.getBytes)
    val testOutputStream = new ByteArrayOutputStream()
    InputOutputStreams(testInputStream, testOutputStream)
  }

  def updateDynamoTestRun(request: UpdateDynamoRequest): Unit = {
    val updateConfig = FormstackConfig.getPerformHandlerConfig
    val updateLambda = UpdateDynamoHandler(Dynamo(), S3, formstackService, updateConfig)
     val streams = requestStreams(request)
    updateLambda.handleRequest(streams.inputStream, streams.outputStream, null)
    val responseString = new String(streams.outputStream.toByteArray)
    println("lambda output was: " + responseString)
  }

   val updateDynamoRequest =
    UpdateDynamoRequest(
      requestType = SAR,
      initiationReference = "initiationReference",
      subjectEmail = "example@test.com",
      dataProvider = "formstack",
      accountNumber = Some(1),
      formPage = 1,
      count = FormstackService.formResultsPerPage,
      timeOfStart = LocalDateTime.now(ZoneOffset.UTC)
    )


  updateDynamoTestRun(updateDynamoRequest)
}