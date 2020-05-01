package com.gu.identity.formstackbatonrequests

import java.io.{InputStream, OutputStream}

import io.circe.{Decoder, DecodingFailure, Encoder, Printer}
import io.circe.parser._
import io.circe.syntax._
import circeCodecs._
import com.gu.identity.formstackbatonrequests.BatonModels.{SarInitiateRequest, SarPerformRequest, SarStatusRequest}
import com.gu.identity.formstackbatonrequests.aws.{Dynamo, Lambda, S3}
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

trait FormstackHandler[Req, Res] extends LazyLogging {
  def handle(request: Req): Either[Throwable, Res]

  val jsonPrinter: Printer = Printer.spaces2.copy(dropNullValues = true)

  private def checkFormstackDataProvider(request: Req): Either[Throwable, Unit] = {
    request match {
      case _: SarStatusRequest => Right(())
      case SarInitiateRequest(_, dataProvider) =>
        Either.cond(dataProvider == "formstack", (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
      case SarPerformRequest(_, _, dataProvider) =>
        Either.cond(dataProvider == "formstack", (), DecodingFailure(s"invalid dataProvider: $dataProvider", List.empty))
    }
  }

  def handleRequest(input: InputStream, output: OutputStream)(
      implicit decoder: Decoder[Req],
      encoder: Encoder[Res]): Unit = {
    try {
      val response = for {
        request <- decode[Req](Source.fromInputStream(input).mkString)
        _ <- checkFormstackDataProvider(request)
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
    val sarHandlerConfig = FormstackConfig.getSarHandlerConfig
    val sarHandler = FormstackSarHandler(S3, Lambda, sarHandlerConfig)
    sarHandler.handleRequest(inputStream, outputStream)
  }

  def handlePerformSar(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val performSarHandlerConfig = FormstackConfig.getPerformSarHandlerConfig
    val performSarHandler =
      if (stage == "PROD")
        FormstackPerformSarHandler(Dynamo(), FormstackSarService, S3, performSarHandlerConfig)
      else FormstackPerformSarHandlerStub(S3, performSarHandlerConfig)
    performSarHandler.handleRequest(inputStream, outputStream)
  }

}