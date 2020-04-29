package com.gu.identity.formstackbatonrequests.aws

import com.gu.identity.formstackbatonrequests.circeCodecs._
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.{InvocationType, InvokeRequest, InvokeResult}
import com.gu.identity.formstackbatonrequests.BatonModels.{PerformRequest, SarPerformRequest}
import com.gu.identity.formstackbatonrequests.InitLambdaConfig
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._

import scala.util.Try

trait LambdaClient {
  def invokeLambda(performRequest: PerformRequest, config: InitLambdaConfig): Either[Throwable, InvokeResult]
}

object Lambda extends LambdaClient with LazyLogging {
  private val lambdaClient = AWSLambdaClient
    .builder()
    .withRegion(AwsCredentials.region)
    .withCredentials(AwsCredentials.credentials)
    .build()

  override def invokeLambda(performRequest: PerformRequest, config: InitLambdaConfig): Either[Throwable, InvokeResult] = {
    val invokeRequest = new InvokeRequest()
      .withFunctionName(config.performFunctionName)
      .withPayload(performRequest.asJson.toString)
      .withInvocationType(InvocationType.Event)

    Try(lambdaClient.invoke(invokeRequest)).toEither.left.map { err =>
      logger.error("unable to invoke perform request", err)
      err
    }
  }
}