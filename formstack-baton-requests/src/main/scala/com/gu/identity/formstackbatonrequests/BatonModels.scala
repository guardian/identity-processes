package com.gu.identity.formstackbatonrequests

object BatonModels {

  sealed trait BatonTaskStatus
  case object Pending extends BatonTaskStatus
  case object Completed extends BatonTaskStatus
  case object Failed extends BatonTaskStatus

  sealed trait BatonRequestType {
    def id: String
  }

  object BatonRequestType {
    val requests = List(SAR, RER)
  }

  case object SAR extends BatonRequestType {
    override def id: String = "SAR"
  }
  case object RER extends BatonRequestType {
    override def id: String = "RER"
  }

  sealed trait PerformRequest
  sealed trait PerformResponse

  sealed trait SarRequest

  case class SarInitiateRequest(subjectEmail: String, dataProvider: String, requestType: BatonRequestType) extends SarRequest
  case class SarStatusRequest(initiationReference: String) extends SarRequest
  case class SarPerformRequest(initiationReference: String, subjectEmail: String, dataProvider: String) extends SarRequest with PerformRequest

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
  ) extends SarResponse with PerformResponse

  sealed trait RerRequest

  case class RerInitiateRequest(subjectEmail: String, dataProvider: String, requestType: BatonRequestType) extends RerRequest
  case class RerStatusRequest(initiationReference: String) extends RerRequest
  case class RerPerformRequest(initiationReference: String, subjectEmail: String, dataProvider: String) extends RerRequest with PerformRequest

  sealed trait RerResponse

  case class RerInitiateResponse(initiationReference: String) extends RerResponse
  case class RerStatusResponse(
    initiationReference: String,
    status: BatonTaskStatus,
    message: Option[String]
  ) extends RerResponse
  case class RerPerformResponse(
    initiationReference: String,
    subjectEmail: String,
    status: BatonTaskStatus,
    message: Option[String]
  ) extends RerResponse with PerformResponse
}
