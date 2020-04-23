package com.gu.identity.formstackbatonrequests

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import BatonModels.{SarPerformRequest, SarRequest, SarStatusRequest}
import io.circe.syntax._
import circeCodecs._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.gu.identity.formstackbatonrequests.aws.{AwsCredentials, Dynamo, Lambda, S3}

/* This object can be used for local runs of the lambda, for end-to-end testing. */

object FormstackBatonLambdaLocalRun extends App {


  case class InputOutputStreams(inputStream: ByteArrayInputStream, outputStream: ByteArrayOutputStream)

  private def requestStreams(request: SarRequest): InputOutputStreams = {
    val jsonRequest = request.asJson.noSpaces
    val testInputStream = new ByteArrayInputStream(jsonRequest.getBytes)
    val testOutputStream = new ByteArrayOutputStream()
    InputOutputStreams(testInputStream, testOutputStream)
  }

  def formstackSarTestRun(request: SarRequest): Unit = {
    val sarLambdaConfig = FormstackConfig.getSarHandlerConfig
    val sarLambda = FormstackSarHandler(S3, Lambda, sarLambdaConfig)
    val streams = requestStreams(request)
    sarLambda.handleRequest(streams.inputStream, streams.outputStream)
    val responseString = new String(streams.outputStream.toByteArray)
    println("lambda output was: " + responseString)
  }

  def formstackPerformSarTestRun(request: SarRequest): Unit = {
    val performSarLambdaConfig = FormstackConfig.getPerformSarHandlerConfig
    val performSarLambda = FormstackPerformSarHandler(Dynamo(), FormstackSarService, S3, performSarLambdaConfig)
    val streams = requestStreams(request)
    performSarLambda.handleRequest(streams.inputStream, streams.outputStream)
    val responseString = new String(streams.outputStream.toByteArray)
    println("lambda output was: " + responseString)
  }

  val sarStatusRequest = SarStatusRequest(initiationReference = "initiationReference")
  val sarPerformRequest = SarPerformRequest(
    initiationReference = "initiationReference",
    subjectEmail = "alastairsharpe89@gmail.com",
    dataProvider = "formstack")

  formstackPerformSarTestRun(sarPerformRequest)
}
