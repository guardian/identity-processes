package com.gu.identity.formstackbatonrequests

import com.amazonaws.services.lambda.model.InvokeResult
import com.gu.identity.formstackbatonrequests.BatonModels.SarPerformRequest
import com.gu.identity.formstackbatonrequests.aws.LambdaClient

class LambdaClientStub(
  invokeLambda: Either[Throwable, InvokeResult]
) extends LambdaClient {
  override def invokeLambda(sarPerformRequest: SarPerformRequest, config: SarLambdaConfig): Either[Throwable, InvokeResult] = invokeLambda
}

object LambdaClientStub {
  val mockInvokeResult = new InvokeResult()

  def withSuccessResponse = new LambdaClientStub(Right(mockInvokeResult))
}