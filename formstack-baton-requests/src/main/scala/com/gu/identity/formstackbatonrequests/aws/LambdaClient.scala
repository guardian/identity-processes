package com.gu.identity.formstackbatonrequests.aws

import com.gu.identity.formstackbatonrequests.circeCodecs._
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.{InvocationType, InvokeRequest, InvokeResult}
import com.gu.identity.formstackbatonrequests.BatonModels.SarPerformRequest
import com.gu.identity.formstackbatonrequests.SarLambdaConfig
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._

import scala.util.Try

trait LambdaClient {
  def invokeLambda(sarPerformRequest: SarPerformRequest, config: SarLambdaConfig): Either[Throwable, InvokeResult]
}

object Lambda extends LambdaClient with LazyLogging {
  private val lambdaClient = AWSLambdaClient
    .builder()
    .withRegion(AwsCredentials.region)
    .withCredentials(AwsCredentials.credentials)
    .build()

  override def invokeLambda(sarPerformRequest: SarPerformRequest, config: SarLambdaConfig): Either[Throwable, InvokeResult] = {
    val invokeRequest = new InvokeRequest()
      .withFunctionName(config.performSarFunctionName)
      .withPayload(sarPerformRequest.asJson.toString)
      .withInvocationType(InvocationType.Event)

    Try(lambdaClient.invoke(invokeRequest)).toEither.left.map { err =>
      logger.error("unable to invoke FormstackPerformSarLambda", err)
      err
    }
  }
}