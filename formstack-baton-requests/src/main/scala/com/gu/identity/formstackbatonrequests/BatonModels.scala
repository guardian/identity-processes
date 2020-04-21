package com.gu.identity.formstackbatonrequests

object BatonModels {

  sealed trait BatonTaskStatus
  case object Pending extends BatonTaskStatus
  case object Completed extends BatonTaskStatus
  case object Failed extends BatonTaskStatus

  sealed trait SarRequest

  case class SarInitiateRequest(subjectEmail: String, dataProvider: String) extends SarRequest
  case class SarStatusRequest(initiationReference: String) extends SarRequest
  case class SarPerformRequest(initiationReference: String, subjectEmail: String, dataProvider: String) extends SarRequest

  sealed trait SarResponse

  case class SarInitiateResponse(initiationReference: String) extends SarResponse
  case class SarStatusResponse(
    status: BatonTaskStatus,
    resultLocations: Option[List[String]] = None,
    message: Option[String] = None
  ) extends SarResponse
  case class SarPerformResponse(
    status: BatonTaskStatus,
    initiationReference: String,
    subjectEmail: String,
    message: Option[String]
  ) extends SarResponse
}
