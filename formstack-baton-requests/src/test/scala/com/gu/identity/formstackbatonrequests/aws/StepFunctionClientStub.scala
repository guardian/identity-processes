package com.gu.identity.formstackbatonrequests.aws

import com.amazonaws.services.stepfunctions.model.StartExecutionResult
import com.gu.identity.formstackbatonrequests.BatonModels.UpdateDynamoRequest
import com.gu.identity.formstackbatonrequests.InitLambdaConfig

class StepFunctionClientStub(
  startStepFunctionResult: Either[Throwable, StartExecutionResult]
) extends StepFunctionClient {
  def startStepFunction(updateDynamoRequest: UpdateDynamoRequest, config: InitLambdaConfig): Either[Throwable, StartExecutionResult] = startStepFunctionResult
}

object StepFunctionClientStub {
  val mockStartStepFunctionResult = new StartExecutionResult()

  def withSuccessResponse = new StepFunctionClientStub(Right(mockStartStepFunctionResult))
}