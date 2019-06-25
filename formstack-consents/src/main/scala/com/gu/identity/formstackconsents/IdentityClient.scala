package com.gu.identity.formstackconsents

import io.circe.syntax._
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.gu.identity.formstackconsents.Lambda.{Config, FormstackSubmission}
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.extras._
import scalaj.http.{Http, HttpResponse}

import scala.util.Try

class IdentityClient(config: Config) extends StrictLogging {

  val newsletters: List[Newsletter] = List(Holidays, Students, Universities, Teachers, Masterclasses, SocietyWeekly, EdinburghFestivalDataCollection)

  def updateConsent(formstackSubmission: FormstackSubmission, newsletter: Newsletter): Option[HttpResponse[String]] = {
    // The key in the JSON sent to Identity depends on the listType. Sometimes the listType is 'set-lists' with a value of the consent name,
    // and sometimes the listType is 'set-consents'. See example below.
    //  {
    //    "email" : "lauren.emms@guardian.co.uk",
    //    "set-consents" : "holidays"
    //  }
    val requestBody = IdentityClient.createIdentityRequest(formstackSubmission.emailAddress, newsletter)

    Try {
      Http(s"${config.idapiHost}/consent-email")
        .headers(("Authorization", config.idapiAccessToken), ("Content-type", "application/json"), ("Accept", "text/plain"))
        .postData(requestBody.asJson.noSpaces)
        .asString
    }.toOption
  }

  def handleResponseFromIdentity(response: Option[HttpResponse[String]], formstackSubmission: FormstackSubmission, newsletter: Newsletter): Option[APIGatewayProxyResponseEvent] = {
    val successfulResponse = new APIGatewayProxyResponseEvent
    val unsuccessfulResponse = new APIGatewayProxyResponseEvent
    response match {
      case Some(r) if r.is2xx =>
        logger.info(s"successfully posted newsletter consent to identity: email: ${formstackSubmission.emailAddress}, newsletter: ${newsletter.consent}")
        Some(successfulResponse.withStatusCode(200))
      case Some(r) =>
        logger.error(s"unable to post newsletter consent to identity for email: ${formstackSubmission.emailAddress}, newsletter: ${newsletter.consent}, $r")
        Some(unsuccessfulResponse.withStatusCode(r.code))
      case _ =>
        logger.error(s"unable to connect to identity")
        None
    }
  }

  def sendConsentToIdentity(formstackSubmission: FormstackSubmission): Option[APIGatewayProxyResponseEvent] = {
    val newsletterOpt = newsletters.find(n => n.formId == formstackSubmission.formId)

    newsletterOpt.flatMap { newsletter => {
      val response = updateConsent(formstackSubmission, newsletter)
      handleResponseFromIdentity(response, formstackSubmission, newsletter)
    }}
  }
}

object IdentityClient {
  implicit val circeConfig: Configuration = Configuration.default
  @ConfiguredJsonCodec case class IdentityRequest(email: String, listType: List[String])

  def createIdentityRequest(email: String, newsletter: Newsletter): IdentityRequest = {

    implicit val circeConfig: Configuration = Configuration.default.copy(
      transformMemberNames = {
        case "listType" => newsletter.listType
        case other => other
      }
    )

    IdentityRequest(email, List(newsletter.consent))
  }
}

