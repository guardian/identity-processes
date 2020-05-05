package com.gu.identity.formstackbatonrequests

import java.io.{InputStream, OutputStream}

import io.circe.{Decoder, DecodingFailure, Encoder, Printer}
import io.circe.parser._
import io.circe.syntax._
import circeCodecs._
import com.gu.identity.formstackbatonrequests.BatonModels.{RER, RerInitiateRequest, RerPerformRequest, RerStatusRequest, SAR, SarInitiateRequest, SarPerformRequest, SarStatusRequest}
import com.gu.identity.formstackbatonrequests.aws.{Dynamo, Lambda, S3}
import com.gu.identity.formstackbatonrequests.rer.{FormstackPerformRerHandler, FormstackRerHandler}
import com.gu.identity.formstackbatonrequests.sar.{FormstackPerformSarHandler, FormstackSarHandler}
import com.gu.identity.formstackbatonrequests.services.FormstackService
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

trait FormstackHandler[Req, Res] extends LazyLogging {
  def handle(request: Req): Either[Throwable, Res]

  val jsonPrinter: Printer = Printer.spaces2.copy(dropNullValues = true)

  private def checkFormstackDataProvider(request: Req): Either[Throwable, Unit] =
    request match {
      case _: SarStatusRequest => Right(())
      case _: RerStatusRequest => Right(())
      case SarInitiateRequest(_, dataProvider, _) =>
        Either.cond(dataProvider == "formstack", (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
      case SarPerformRequest(_, _, dataProvider) =>
        Either.cond(dataProvider == "formstack", (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
      case RerInitiateRequest(_, dataProvider, _) =>
        Either.cond(dataProvider == "formstack", (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
      case RerPerformRequest(_, _, dataProvider) =>
        Either.cond(dataProvider == "formstack", (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
    }


  private def checkRequestTypeMatchesRequest(request: Req): Either[Throwable, Unit] =
    request match {
      case SarInitiateRequest(_, _, requestType) =>
        Either.cond(requestType == SAR, (), DecodingFailure(s"excepted request type not found: $requestType", List.empty))
      case RerInitiateRequest(_, _, requestType) =>
        Either.cond(requestType == RER, (), DecodingFailure(s"excepted request type not found: $requestType", List.empty))
      case _ => Right(())
    }


  def handleRequest(input: InputStream, output: OutputStream)(
      implicit decoder: Decoder[Req],
      encoder: Encoder[Res]): Unit = {
    try {
      val response = for {
        request <- decode[Req](Source.fromInputStream(input).mkString)
        _ <- checkFormstackDataProvider(request)
        _ <- checkRequestTypeMatchesRequest(request)
        response <- handle(request)
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

  val stage = sys.env.getOrElse("STAGE", "CODE") match {
    case "PROD" => "PROD"
    case _      => "CODE"
  }

  def handleSar(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val sarHandlerConfig = FormstackConfig.getInitHandlerConfig
    val sarHandler = FormstackSarHandler(S3, Lambda, sarHandlerConfig)
    sarHandler.handleRequest(inputStream, outputStream)
  }

  def handlePerformSar(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val performSarHandlerConfig = FormstackConfig.getPerformHandlerConfig
    val performSarHandler =
      if (stage == "PROD")
        FormstackPerformSarHandler(Dynamo(), FormstackService, S3, performSarHandlerConfig)
      else PerformHandlerStubs.FormstackPerformSarHandlerStub(S3, performSarHandlerConfig)
    performSarHandler.handleRequest(inputStream, outputStream)
  }


  def handleRer(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val rerHandlerConfig = FormstackConfig.getInitHandlerConfig
    val rerHandler = FormstackRerHandler(S3, Lambda, rerHandlerConfig)
    rerHandler.handleRequest(inputStream, outputStream)
  }

  def handlePerformRer(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val performRerHandlerConfig = FormstackConfig.getPerformHandlerConfig
    val performRerHandler =
      if (stage == "PROD")
        FormstackPerformRerHandler(Dynamo(), FormstackService, S3, performRerHandlerConfig)
      else PerformHandlerStubs.FormstackPerformRerHandlerStub(S3, performRerHandlerConfig)
    performRerHandler.handleRequest(inputStream, outputStream)
  }

}