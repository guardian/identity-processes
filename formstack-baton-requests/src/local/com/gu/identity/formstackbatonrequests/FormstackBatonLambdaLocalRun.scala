package com.gu.identity.formstackbatonrequests

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.time.LocalDateTime
import BatonModels.{BatonRequest, RerPerformRequest, RerRequest, RerStatusRequest, SAR, SarPerformRequest, SarRequest, SarStatusRequest, UpdateDynamoRequest}
import io.circe.syntax._
import circeCodecs._
import com.gu.identity.formstackbatonrequests.aws.{Dynamo, S3, StepFunction}
import com.gu.identity.formstackbatonrequests.sar.{FormstackPerformSarHandler, FormstackSarHandler}
import com.gu.identity.formstackbatonrequests.services.{FormstackService}
import com.gu.identity.formstackbatonrequests.updatedynamo.UpdateDynamoHandler

/* This object can be used for local runs of the lambda, for end-to-end testing. */

object FormstackBatonLambdaLocalRun extends App {

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

  def formstackSarTestRun(request: SarRequest): Unit = {
    val sarLambdaConfig = FormstackConfig.getInitHandlerConfig
    val sarLambda = FormstackSarHandler(S3, StepFunction, sarLambdaConfig)
    val streams = requestStreams(request)
    sarLambda.handleRequest(streams.inputStream, streams.outputStream, null)
    val responseString = new String(streams.outputStream.toByteArray)
    println("lambda output was: " + responseString)
  }

  def formstackPerformSarTestRun(request: SarRequest): Unit = {
    val performSarLambdaConfig = FormstackConfig.getPerformHandlerConfig
    val performSarLambda = FormstackPerformSarHandler(Dynamo(), formstackService, S3, performSarLambdaConfig)
    val streams = requestStreams(request)
    performSarLambda.handleRequest(streams.inputStream, streams.outputStream, null)
    val responseString = new String(streams.outputStream.toByteArray)
    println("lambda output was: " + responseString)
  }

  def formstackRerTestRun(request: RerRequest): Unit = {
    val rerLambdaConfig = FormstackConfig.getInitHandlerConfig
    val rerLambda = FormstackSarHandler(S3, StepFunction, rerLambdaConfig)
    val streams = requestStreams(request)
    rerLambda.handleRequest(streams.inputStream, streams.outputStream, null)
    val responseString = new String(streams.outputStream.toByteArray)
    println("lambda output was: " + responseString)
  }

  def formstackPerformRerTestRun(request: RerRequest): Unit = {
    val performRerLambdaConfig = FormstackConfig.getPerformHandlerConfig
    val performRerLambda = FormstackPerformSarHandler(Dynamo(), formstackService, S3, performRerLambdaConfig)
    val streams = requestStreams(request)
    performRerLambda.handleRequest(streams.inputStream, streams.outputStream, null)
    val responseString = new String(streams.outputStream.toByteArray)
    println("lambda output was: " + responseString)
  }

  def updateDynamoTestRun(request: UpdateDynamoRequest): Unit = {
    val updateConfig = FormstackConfig.getPerformHandlerConfig
    val updateLambda = UpdateDynamoHandler(Dynamo(), S3, formstackService, updateConfig)
    val streams = requestStreams(request)
    updateLambda.handleRequest(streams.inputStream, streams.outputStream, null)
    val responseString = new String(streams.outputStream.toByteArray)
    println("lambda output was: " + responseString)
  }

  val sarStatusRequest = SarStatusRequest(initiationReference = "initiationReference")
  val sarPerformRequest = SarPerformRequest(
    initiationReference = "initiationReference",
    subjectEmail = "example@test.com",
    dataProvider = "formstack")

  val rerStatusRequest = RerStatusRequest(initiationReference = "initiationReference")
  val rerPerformRequest = RerPerformRequest(
    initiationReference = "initiationReference",
    subjectEmail = "example@test.com",
    dataProvider = "formstack")

  val updateDynamoRequest =
    UpdateDynamoRequest(
      requestType = SAR,
      initiationReference = "initiationReference",
      subjectEmail = "example@test.com",
      dataProvider = "formstack",
      accountNumber = Some(1),
      formPage = 1,
      count = FormstackService.formResultsPerPage,
      timeOfStart = LocalDateTime.now
    )

  updateDynamoTestRun(updateDynamoRequest)
}