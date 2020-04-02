package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels.{BatonTaskStatus, Completed, Failed, Pending, SarInitiateRequest, SarInitiateResponse, SarPerformRequest, SarRequest, SarResponse, SarStatusRequest, SarStatusResponse}
import io.circe.{Decoder, Encoder, Json, JsonObject}
import io.circe.generic.auto._
import io.circe.syntax._

object circeCodecs {

  implicit val sarRequestDecoder: Decoder[SarRequest] =
    Decoder.instance[SarRequest] { cursor =>
      cursor.downField("action").as[String].flatMap {
        case "status" => cursor.as[SarStatusRequest]
        case "initiate" => cursor.as[SarInitiateRequest]
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
  }

  implicit val sarPerformRequestEncoder: Encoder[SarPerformRequest] =
    Encoder.instance { psr =>
      addAdditionalFields(psr.asJsonObject, "SAR", "perform")
    }

  /** encoder used for test run. */
  implicit val sarRequestEncoder: Encoder[SarRequest] = Encoder.instance {
    case ir: SarInitiateRequest =>
      addAdditionalFields(ir.asJsonObject, "SAR", "initiate")
    case sr: SarStatusRequest =>
      addAdditionalFields(sr.asJsonObject, "SAR", "status")
  }
}
