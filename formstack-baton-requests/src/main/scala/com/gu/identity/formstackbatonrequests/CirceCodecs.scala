package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.BatonModels._
import io.circe.generic.JsonCodec
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject}
import io.circe.generic.semiauto._
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

  implicit val updateDynamoRequestDecoder: Decoder[UpdateDynamoRequest] = deriveDecoder[UpdateDynamoRequest]

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

  implicit val updateDynamoResponseEncoder: Encoder[UpdateDynamoResponse] =
    Encoder.encodeJson.contramap[UpdateDynamoResponse] { res =>
      res.asJsonObject.add("action", "perform".asJson).asJson
    }

  implicit val requestTypeEncoder: Encoder[BatonRequestType] = Encoder.instance {
    case SAR => "SAR".asJson
    case RER => "RER".asJson
  }

  implicit val updateDynamoRequestEncoder: Encoder[UpdateDynamoRequest] = deriveEncoder

  /* encoders used for test runs. */

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

  /* Codecs for decoding accountFormsForGivenPage response */
  @JsonCodec case class Form(id: String)
  @JsonCodec case class FormsResponse(forms: List[Form], total: Int)

  /* Codecs for decoding formSubmissionsForGivenPage response */
  @JsonCodec case class ResponseValue(value: Json)
  case class FormSubmission(id: String, data: Map[String, ResponseValue])
  @JsonCodec case class FormSubmissions(submissions: List[FormSubmission], pages: Int)

  val decodePopulatedSubmission: Decoder[FormSubmission] = (c: HCursor) => for {
    id <- c.downField("id").as[String]
    data <- c.downField("data").as[Map[String, ResponseValue]]
  } yield {
    FormSubmission(id, data)
  }

  val decodeEmptySubmission: Decoder[FormSubmission] = (c: HCursor) => {
    for {
      data <- c.downField("data").as[List[String]]
      id <-
        if (data.isEmpty) c.downField("id").as[String]
        else Left(DecodingFailure("Unable to decode submission data format", List.empty))
    } yield FormSubmission(id, Map.empty)
  }

  implicit val decodeSubmission: Decoder[FormSubmission] = decodePopulatedSubmission or decodeEmptySubmission

  /* Codecs for decoding submissionsById response */
  @JsonCodec case class SubmissionData(field: String, value: String)
  @JsonCodec case class Submission(id: String, timestamp: String, data: List[SubmissionData])

  /* Codecs for decoding retrieveSubmissionLabels*/
  @JsonCodec case class SubmissionLabelField(label: String)

  /* Codecs for submission deletion */
  @JsonCodec case class SubmissionDeletionReponse(success: Int)
}
