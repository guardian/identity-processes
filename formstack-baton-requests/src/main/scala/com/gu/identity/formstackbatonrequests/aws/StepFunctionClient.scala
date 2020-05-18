package com.gu.identity.formstackbatonrequests.aws

import com.gu.identity.formstackbatonrequests.circeCodecs._
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClient
import com.amazonaws.services.stepfunctions.model.{StartExecutionRequest, StartExecutionResult}
import com.gu.identity.formstackbatonrequests.BatonModels.UpdateDynamoRequest
import com.gu.identity.formstackbatonrequests.InitLambdaConfig
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._

import scala.util.Try

trait StepFunctionClient {
  def startStepFunction(updateDynamoRequest: UpdateDynamoRequest, config: InitLambdaConfig): Either[Throwable, StartExecutionResult]
}

object StepFunction extends StepFunctionClient with LazyLogging {
  private val stepFunctionClient = AWSStepFunctionsClient
    .builder()
    .withRegion(AwsCredentials.region)
    .withCredentials(AwsCredentials.credentials)
    .build()

  override def startStepFunction(updateDynamoRequest: UpdateDynamoRequest, config: InitLambdaConfig): Either[Throwable, StartExecutionResult] = {
    val executionRequest = new StartExecutionRequest()
      .withStateMachineArn(config.stateMachineArn)
      .withInput(updateDynamoRequest.asJson.toString())

    Try(stepFunctionClient.startExecution(executionRequest)).toEither.left.map { err =>
      logger.error(s"unable to start ${updateDynamoRequest.requestType} step function", err)
      err
    }
  }
}