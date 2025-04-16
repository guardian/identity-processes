package com.gu.identity.formstackbatonrequests.rer

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels._
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, S3Client, S3WriteSuccess}
import com.gu.identity.formstackbatonrequests.services.{DynamoUpdateService, FormstackRequestService}
import com.gu.identity.formstackbatonrequests.{FormstackHandler, PerformLambdaConfig}
import com.typesafe.scalalogging.LazyLogging

case class FormstackPerformRerHandler(
  dynamoClient: DynamoClient,
  formstackClient: FormstackRequestService,
  s3Client: S3Client,
  config: PerformLambdaConfig)
  extends LazyLogging with FormstackHandler[RerRequest, RerResponse] {

  val dynamoUpdateService: DynamoUpdateService = DynamoUpdateService(formstackClient, dynamoClient, config)

  def initiateRer(request: RerPerformRequest): Either[Throwable, S3WriteSuccess] = ???
//    for {
//      submissionIds <- dynamoClient.userSubmissions(request.subjectEmail.toLowerCase, config.bcryptSalt, config.submissionTableName)
//      _ <- formstackClient.deleteUserData(request.subjectEmail.toLowerCase, submissionIds, config)
//      _ <- dynamoClient.deleteUserSubmissions(submissionIds, config.bcryptSalt, config.submissionTableName)
//      writeToS3Response <- s3Client.writeSuccessResult(request.initiationReference, List.empty, RER, config)
//    } yield writeToS3Response

  override def handle(request: RerRequest, context: Context): Either[Throwable, RerResponse] = ???
}
