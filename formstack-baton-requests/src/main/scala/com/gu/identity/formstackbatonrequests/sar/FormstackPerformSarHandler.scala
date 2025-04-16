package com.gu.identity.formstackbatonrequests.sar

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, S3Client, S3WriteSuccess}
import com.gu.identity.formstackbatonrequests.services.{DynamoUpdateService, FormstackRequestService}
import com.gu.identity.formstackbatonrequests.{FormstackHandler, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

case class SubmissionIdEmail(email: String, submissionId: String, receivedByLambdaTimestamp: Long, accountNumber: Int)
case class FormstackLabelValue(label: String, value: String)
case class FormstackSubmissionQuestionAnswer(id: String, timestamp: String, fields: List[FormstackLabelValue])

case class FormstackPerformSarHandler(
  dynamoClient: DynamoClient,
  formstackClient: FormstackRequestService,
  s3Client: S3Client,
  config: PerformLambdaConfig)
  extends LazyLogging with FormstackHandler[SarRequest, SarResponse] {

  val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(formstackClient, dynamoClient, config)

  def initiateSar(request: SarPerformRequest): Either[Throwable, S3WriteSuccess] = ???

  override def handle(request: SarRequest, context: Context): Either[Throwable, SarResponse] = ???
}
