package com.gu.identity.formstackbatonrequests.sar

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

  val dynamoUpdateService = DynamoUpdateService(formstackClient, dynamoClient, config)

  def initiateSar(request: SarPerformRequest): Either[Throwable, S3WriteSuccess] =
    for {
      submissionTableUpdateDate <- dynamoClient.mostRecentTimestamp(config.lastUpdatedTableName)
      _ <- dynamoUpdateService.updateDynamo(submissionTableUpdateDate)
      submissionIds <- dynamoClient.userSubmissions(request.subjectEmail.toLowerCase, config.bcryptSalt, config.submissionTableName)
      submissionData <- formstackClient.submissionData(submissionIds, config)
      writeToS3Response <- s3Client.writeSuccessResult(request.initiationReference, submissionData, SAR, config)
    } yield writeToS3Response

  override def handle(request: SarRequest): Either[Throwable, SarResponse] =
    request match {
      case r: SarPerformRequest =>
        initiateSar(r) match {
          case Right(_) => Right(SarPerformResponse(Completed, r.initiationReference, r.subjectEmail, None))
          case Left(err) =>
            s3Client.writeFailedResults(r.initiationReference, err.getMessage, SAR, config)
            Right(SarPerformResponse(Failed, r.initiationReference, r.subjectEmail, Some(err.getMessage)))
        }
      case _ => Left(new Exception("Unable to retrieve email and initiation reference from request"))
    }
}
