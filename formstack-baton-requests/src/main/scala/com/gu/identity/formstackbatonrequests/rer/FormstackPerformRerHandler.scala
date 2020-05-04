package com.gu.identity.formstackbatonrequests.rer

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

  val dynamoUpdateService = DynamoUpdateService(formstackClient, dynamoClient, config)

  def initiateRer(request: RerPerformRequest): Either[Throwable, S3WriteSuccess] =
    for {
      submissionTableUpdateDate <- dynamoClient.mostRecentTimestamp(config.lastUpdatedTableName)
      _ <- dynamoUpdateService.updateDynamo(submissionTableUpdateDate)
      email = request.subjectEmail.toLowerCase
      submissionIds <- dynamoClient.userSubmissions(email, config.bcryptSalt, config.submissionTableName)
      _ <- formstackClient.deleteUserData(submissionIds, config)
      _ <- dynamoClient.deleteUserSubmissions(submissionIds, config.bcryptSalt, config.submissionTableName)
      writeToS3Response <- s3Client.writeSuccessResult(request.initiationReference, List.empty, RER, config)
    } yield writeToS3Response

  override def handle(request: RerRequest): Either[Throwable, RerResponse] =
    request match {
      case r: RerPerformRequest =>
        initiateRer(r) match {
          case Right(_) => Right(RerPerformResponse(r.initiationReference, r.subjectEmail, Completed, None))
          case Left(err) =>
            s3Client.writeFailedResults(r.initiationReference, err.getMessage, RER, config)
            Right(RerPerformResponse(r.initiationReference, r.subjectEmail, Failed, Some(err.getMessage)))
        }
      case _ => Left(new Exception("Unable to retrieve email and initiation reference from request"))
    }
}
