package com.gu.identity.formstackbatonrequests

import java.time.LocalDateTime

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

  sealed trait BatonRequest
  sealed trait BatonResponse

  sealed trait SarRequest extends BatonRequest

  case class SarInitiateRequest(subjectEmail: String, dataProvider: String, requestType: BatonRequestType) extends SarRequest
  case class SarStatusRequest(initiationReference: String) extends SarRequest
  case class SarPerformRequest(initiationReference: String, subjectEmail: String, dataProvider: String) extends SarRequest

  sealed trait SarResponse extends BatonResponse

  case class SarInitiateResponse(initiationReference: String) extends SarResponse
  case class SarStatusResponse(
    status: BatonTaskStatus,
    resultLocations: Option[List[String]] = None,
    message: Option[String] = None
  ) extends SarResponse
  case class SarPerformResponse(
    status: BatonTaskStatus,
    initiationReference: String,
    subjectEmail: String
  ) extends SarResponse

  sealed trait RerRequest extends BatonRequest

  case class RerInitiateRequest(subjectEmail: String, dataProvider: String, requestType: BatonRequestType) extends RerRequest
  case class RerStatusRequest(initiationReference: String) extends RerRequest
  case class RerPerformRequest(initiationReference: String, subjectEmail: String, dataProvider: String) extends RerRequest

  sealed trait RerResponse extends BatonResponse

  case class RerInitiateResponse(initiationReference: String, message: String, status: BatonTaskStatus) extends RerResponse
  case class RerStatusResponse(
    initiationReference: String,
    status: BatonTaskStatus,
    message: String
  ) extends RerResponse
  case class RerPerformResponse(
    initiationReference: String,
    subjectEmail: String,
    status: BatonTaskStatus
  ) extends RerResponse

  case class UpdateDynamoRequest(
    requestType: BatonRequestType,
    initiationReference: String,
    subjectEmail: String,
    dataProvider: String,
    accountNumber: Option[Int],
    formPage: Int,
    count: Int,
    timeOfStart: LocalDateTime
  ) extends BatonRequest

  case class UpdateDynamoResponse(
    status: BatonTaskStatus,
    initiationReference: String,
    subjectEmail: String,
    dataProvider: String,
    accountNumber: Int,
    formPage: Option[Int],
    count: Option[Int],
    requestType: BatonRequestType,
    timeOfStart: LocalDateTime
  ) extends BatonResponse
}
