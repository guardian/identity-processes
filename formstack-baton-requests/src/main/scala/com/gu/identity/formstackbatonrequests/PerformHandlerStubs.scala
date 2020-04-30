package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, RerPerformRequest, RerPerformResponse, RerRequest, RerResponse, SAR, SarPerformRequest, SarPerformResponse, SarRequest, SarResponse}
import com.gu.identity.formstackbatonrequests.aws.S3Client

object PerformHandlerStubs {
  case class FormstackPerformSarHandlerStub(
    s3Client: S3Client,
    config: PerformLambdaConfig) extends FormstackHandler[SarRequest, SarResponse] {
    override def handle(request: SarRequest): Either[Throwable, SarPerformResponse] = {
      request match {
        case r: SarPerformRequest =>
          val successfulResult =
            List(
              FormstackSubmissionQuestionAnswer(
                "submissionId",
                "timestamp",
                List(FormstackLabelValue("Email address", "example@test.com"))))

          s3Client.writeSuccessResult(r.initiationReference, successfulResult, SAR, config)
            .map(_ => SarPerformResponse(Completed, r.initiationReference, r.subjectEmail, None))
        case _ =>
          throw new RuntimeException(
            "Unable to retrieve email and initiation reference from request.")
      }
    }
  }

  case class FormstackPerformRerHandlerStub(
    s3Client: S3Client,
    config: PerformLambdaConfig) extends FormstackHandler[RerRequest, RerResponse] {
    override def handle(request: RerRequest): Either[Throwable, RerPerformResponse] = {
      request match {
        case r: RerPerformRequest =>
          s3Client.writeSuccessResult(r.initiationReference, List.empty, SAR, config)
            .map(_ => RerPerformResponse(r.initiationReference, r.subjectEmail, Completed, None))
        case _ =>
          throw new RuntimeException(
            "Unable to retrieve email and initiation reference from request.")
      }
    }
  }
}


