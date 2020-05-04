package com.gu.identity.formstackbatonrequests

import com.amazonaws.services.lambda.model.InvokeResult
import com.gu.identity.formstackbatonrequests.BatonModels.{PerformRequest, SarPerformRequest}
import com.gu.identity.formstackbatonrequests.aws.LambdaClient

class LambdaClientStub(
  invokeLambda: Either[Throwable, InvokeResult]
) extends LambdaClient {
  def invokeLambda(performRequest: PerformRequest, config: InitLambdaConfig): Either[Throwable, InvokeResult] = invokeLambda
}

object LambdaClientStub {
  val mockInvokeResult = new InvokeResult()

  def withSuccessResponse = new LambdaClientStub(Right(mockInvokeResult))
}