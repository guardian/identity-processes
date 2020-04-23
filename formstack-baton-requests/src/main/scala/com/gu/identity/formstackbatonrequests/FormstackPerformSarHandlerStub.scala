package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, SarPerformRequest, SarPerformResponse, SarRequest, SarResponse}
import com.gu.identity.formstackbatonrequests.aws.{DynamoClient, S3Client}

case class FormstackPerformSarHandlerStub(
  s3Client: S3Client,
  config: PerformSarLambdaConfig) extends FormstackHandler[SarRequest, SarResponse] {
  override def handle(request: SarRequest): Either[Throwable, SarPerformResponse] = {
    request match {
      case r: SarPerformRequest =>
        val successfulResult =
          List(
            FormstackSubmissionQuestionAnswer(
              "submissionId",
              "timestamp",
              List(FormstackLabelValue("Email address", "example@test.com"))))

        s3Client.writeSuccessResult(r.initiationReference, successfulResult, config)
          .map(_ => SarPerformResponse(Completed, r.initiationReference, r.subjectEmail, None))
      case _ =>
        throw new RuntimeException(
          "Unable to retrieve email and initiation reference from request.")
    }
  }
}
