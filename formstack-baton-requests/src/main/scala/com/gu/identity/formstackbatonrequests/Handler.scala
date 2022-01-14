package com.gu.identity.formstackbatonrequests

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.time.LocalDateTime

import io.circe.{Decoder, DecodingFailure, Encoder, Printer}
import io.circe.parser._
import io.circe.syntax._
import circeCodecs._
import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels.{BatonRequest, RER, RerInitiateRequest, RerPerformRequest, RerRequest, RerStatusRequest, SAR, SarInitiateRequest, SarPerformRequest, SarRequest, SarStatusRequest, UpdateDynamoRequest}
import com.gu.identity.formstackbatonrequests.aws.{Dynamo, S3, StepFunction}
import com.gu.identity.formstackbatonrequests.rer.{FormstackPerformRerHandler, FormstackRerHandler}
import com.gu.identity.formstackbatonrequests.sar.{FormstackPerformSarHandler, FormstackSarHandler}
import com.gu.identity.formstackbatonrequests.services.FormstackService
import com.gu.identity.formstackbatonrequests.updatedynamo.UpdateDynamoHandler
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

trait FormstackHandler[Req, Res] extends LazyLogging {
  def handle(request: Req, context: Context): Either[Throwable, Res]

  val jsonPrinter: Printer = Printer.spaces2.copy(dropNullValues = true)

  private def checkFormstackDataProvider(request: Req): Either[Throwable, Unit] = {
    val supportedProviders = List("formstack", "formstackrer")
    request match {
      case _: SarStatusRequest => Right(())
      case _: RerStatusRequest => Right(())
      case SarInitiateRequest(_, dataProvider, _) =>
        Either.cond(supportedProviders.contains(dataProvider), (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
      case SarPerformRequest(_, _, dataProvider) =>
        Either.cond(supportedProviders.contains(dataProvider), (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
      case RerInitiateRequest(_, dataProvider, _) =>
        Either.cond(supportedProviders.contains(dataProvider), (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
      case RerPerformRequest(_, _, dataProvider) =>
        Either.cond(supportedProviders.contains(dataProvider), (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
      case UpdateDynamoRequest(_, _, _, dataProvider, _, _, _, _) =>
        Either.cond(supportedProviders.contains(dataProvider), (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
    }
  }


  private def checkRequestTypeMatchesRequest(request: Req): Either[Throwable, Unit] =
    request match {
      case SarInitiateRequest(_, _, requestType) =>
        Either.cond(requestType == SAR, (), DecodingFailure(s"excepted request type not found: $requestType", List.empty))
      case RerInitiateRequest(_, _, requestType) =>
        Either.cond(requestType == RER, (), DecodingFailure(s"excepted request type not found: $requestType", List.empty))
      case _ => Right(())
    }


  def handleRequest(input: InputStream, output: OutputStream, context: Context)(
      implicit decoder: Decoder[Req],
      encoder: Encoder[Res]): Unit = {
    try {
      val response = for {
        request <- decode[Req](Source.fromInputStream(input).mkString)
        _ <- checkFormstackDataProvider(request)
        _ <- checkRequestTypeMatchesRequest(request)
        response <- handle(request, context)
      } yield response

      response match {
        case Left(err) => output.write(err.toString.asJson.printWith(jsonPrinter).getBytes)
        case Right(res) => output.write(res.asJson.printWith(jsonPrinter).getBytes)
      }

    } finally {
      output.close()
    }
  }
}

object Handler {

  val stage: String = "PROD"

  def handleUpdateDynamo(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {
    val performUpdateConfig = FormstackConfig.getPerformHandlerConfig
    val updateHandler = UpdateDynamoHandler(Dynamo(), S3, FormstackService, performUpdateConfig)
    updateHandler.handleRequest(inputStream, outputStream, context)
  }

  def handleSar(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val sarHandlerConfig = FormstackConfig.getInitHandlerConfig
    val sarHandler = FormstackSarHandler(S3, StepFunction, sarHandlerConfig)
    sarHandler.handleRequest(inputStream, outputStream, null)
  }

  def handlePerformSar(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val performSarHandlerConfig = FormstackConfig.getPerformHandlerConfig
    val performSarHandler =
      if (stage == "PROD")
        FormstackPerformSarHandler(Dynamo(), FormstackService, S3, performSarHandlerConfig)
      else PerformHandlerStubs.FormstackPerformSarHandlerStub(S3, performSarHandlerConfig)
    performSarHandler.handleRequest(inputStream, outputStream, null)
  }


  def handleRer(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val rerHandlerConfig = FormstackConfig.getInitHandlerConfig
    val rerHandler = FormstackRerHandler(S3, StepFunction, rerHandlerConfig)
    rerHandler.handleRequest(inputStream, outputStream, null)
  }

  def handlePerformRer(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val performRerHandlerConfig = FormstackConfig.getPerformHandlerConfig
    val performRerHandler =
      if (stage == "PROD")
        FormstackPerformRerHandler(Dynamo(), FormstackService, S3, performRerHandlerConfig)
      else PerformHandlerStubs.FormstackPerformRerHandlerStub(S3, performRerHandlerConfig)
    performRerHandler.handleRequest(inputStream, outputStream, null)
  }

}

object LocalRun extends App {

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

  val updateDynamoRequest =
    UpdateDynamoRequest(
      requestType = SAR,
      initiationReference = "initiationReference",
      subjectEmail = "example@test.com",
      dataProvider = "formstack",
      accountNumber = Some(1),
      formPage = 1,
      count = FormstackService.resultsPerPage,
      timeOfStart = LocalDateTime.now,
    )

  val updateConfig = FormstackConfig.getPerformHandlerConfig
  val updateLambda = UpdateDynamoHandler(Dynamo(), S3, FormstackService, updateConfig)
  val streams = requestStreams(updateDynamoRequest)
  updateLambda.handleRequest(streams.inputStream, streams.outputStream, null)
  val responseString = new String(streams.outputStream.toByteArray)
  println("lambda output was: " + responseString)
}
