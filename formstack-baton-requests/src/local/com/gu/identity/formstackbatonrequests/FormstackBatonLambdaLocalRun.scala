package com.gu.identity.formstackbatonrequests

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import BatonModels.{SarRequest, SarStatusRequest}
import io.circe.syntax._
import circeCodecs._
import com.gu.identity.formstackbatonrequests.aws.{Lambda, S3}

/** This object can be used for local runs of the lambda, for end-to-end testing. */

object FormstackBatonLambdaLocalRun extends App {
  def runWith(request: SarRequest): Unit = {
    val sarLambdaConfig = FormstackConfig.getSarHandlerConfig
    val sarLambda = FormstackSarHandler(S3, Lambda, sarLambdaConfig)
    val jsonRequest = request.asJson.noSpaces
    val testInputStream = new ByteArrayInputStream(jsonRequest.getBytes)
    val testOutputStream = new ByteArrayOutputStream()
    sarLambda.handleRequest(testInputStream, testOutputStream)
    val responseString = new String(testOutputStream.toByteArray)
    println("lambda output was:" + responseString)
  }

  val sarStatusRequest = SarStatusRequest(initiationReference = "testSubjectId")

  runWith(sarStatusRequest)
}
