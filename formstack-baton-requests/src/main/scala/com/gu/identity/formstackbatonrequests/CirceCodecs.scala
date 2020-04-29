package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels._
import io.circe.{Decoder, Encoder, Json, JsonObject}
import io.circe.generic.auto._
import io.circe.syntax._

object circeCodecs {

  implicit val requestTypeDecoder: Decoder[BatonRequestType] = Decoder.decodeString.map {
    case "SAR" => SAR
    case "RER" => RER
  }

  implicit val sarRequestDecoder: Decoder[SarRequest] =
    Decoder.instance[SarRequest] { cursor =>
      cursor.downField("action").as[String].flatMap {
        case "status" => cursor.as[SarStatusRequest]
        case "initiate" => cursor.as[SarInitiateRequest]
        case "perform" => cursor.as[SarPerformRequest]
      }
    }

  implicit val rerRequestDecoder: Decoder[RerRequest] =
    Decoder.instance[RerRequest] { cursor =>
      cursor.downField("action").as[String].flatMap {
        case "status" => cursor.as[RerStatusRequest]
        case "initiate" => cursor.as[RerInitiateRequest]
        case "perform" => cursor.as[RerPerformRequest]
      }
    }

  implicit val batonTaskStatusEncoder: Encoder[BatonTaskStatus] =
    Encoder.encodeString.contramap {
      case Pending => "pending"
      case Completed => "completed"
      case Failed => "failed"
    }

  private def addAdditionalFields(
    response: JsonObject,
    requestType: String,
    action: String
  ): Json =
    response
      .add("action", action.asJson)
      .add("requestType", requestType.asJson)
      .add("dataProvider", "formstack".asJson)
      .asJson

  implicit val sarResponseEncoder: Encoder[SarResponse] = Encoder.instance {
    case ir: SarInitiateResponse =>
      addAdditionalFields(ir.asJsonObject, "SAR", "initiate")
    case sr: SarStatusResponse =>
      addAdditionalFields(sr.asJsonObject, "SAR", "status")
    case pr: SarPerformResponse =>
      addAdditionalFields(pr.asJsonObject, "SAR", "perform")
  }

  implicit val rerResponseEncoder: Encoder[RerResponse] = Encoder.instance {
    case ir: RerInitiateResponse =>
      addAdditionalFields(ir.asJsonObject, "RER", "initiate")
    case sr: RerStatusResponse =>
      addAdditionalFields(sr.asJsonObject, "RER", "status")
    case pr: RerPerformResponse =>
      addAdditionalFields(pr.asJsonObject, "RER", "perform")
  }

  implicit val performRequestEncoder: Encoder[PerformRequest] = Encoder.instance {
    case spr: SarPerformRequest => addAdditionalFields(spr.asJsonObject, "SAR", "perform")
    case rpr: RerPerformRequest => addAdditionalFields(rpr.asJsonObject, "SAR", "perform")
  }

  /* encoder used for test run. */
  implicit val sarRequestEncoder: Encoder[SarRequest] = Encoder.instance {
    case ir: SarInitiateRequest =>
      addAdditionalFields(ir.asJsonObject, "SAR", "initiate")
    case sr: SarStatusRequest =>
      addAdditionalFields(sr.asJsonObject, "SAR", "status")
    case pr: SarPerformRequest =>
      addAdditionalFields(pr.asJsonObject, "SAR", "perform")
  }

  implicit val rerRequestEncoder: Encoder[RerRequest] = Encoder.instance {
    case ir: RerInitiateRequest =>
      addAdditionalFields(ir.asJsonObject, "RER", "initiate")
    case sr: RerStatusRequest =>
      addAdditionalFields(sr.asJsonObject, "RER", "status")
    case pr: RerPerformRequest =>
      addAdditionalFields(pr.asJsonObject, "RER", "perform")
  }
}
