package com.gu.identity.formstackbatonrequests

import com.amazonaws.services.lambda.runtime.Context
import com.gu.identity.formstackbatonrequests.BatonModels.{Completed, RER, RerPerformRequest, RerPerformResponse, RerRequest, RerResponse, SAR, SarPerformRequest, SarPerformResponse, SarRequest, SarResponse}
import com.gu.identity.formstackbatonrequests.aws.S3Client
import com.gu.identity.formstackbatonrequests.sar.{FormstackLabelValue, FormstackSubmissionQuestionAnswer}

object PerformHandlerStubs {
  case class FormstackPerformSarHandlerStub(
    s3Client: S3Client,
    config: PerformLambdaConfig) extends FormstackHandler[SarRequest, SarResponse] {
    override def handle(request: SarRequest, context: Context): Either[Throwable, SarPerformResponse] = {
      request match {
        case r: SarPerformRequest =>
          val successfulResult =
            List(
              FormstackSubmissionQuestionAnswer(
                "submissionId",
                "timestamp",
                List(FormstackLabelValue("Email address", "example@test.com"))))

          s3Client.writeSuccessResult(r.initiationReference, successfulResult, SAR, config)
            .map(_ => SarPerformResponse(Completed, r.initiationReference, r.subjectEmail))
        case _ =>
          throw new RuntimeException(
            "Unable to retrieve email and initiation reference from request.")
      }
    }
  }

  case class FormstackPerformRerHandlerStub(
    s3Client: S3Client,
    config: PerformLambdaConfig) extends FormstackHandler[RerRequest, RerResponse] {
    override def handle(request: RerRequest, context: Context): Either[Throwable, RerPerformResponse] = {
      request match {
        case r: RerPerformRequest =>
          s3Client.writeSuccessResult(r.initiationReference, List.empty, RER, config)
            .map(_ => RerPerformResponse(r.initiationReference, r.subjectEmail, Completed))
        case _ =>
          throw new RuntimeException(
            "Unable to retrieve email and initiation reference from request.")
      }
    }
  }
}


