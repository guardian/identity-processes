package com.gu.identity.formstackconsents

import io.circe.syntax._
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.gu.identity.formstackconsents.Lambda.Config
import com.typesafe.scalalogging.StrictLogging
import io.circe.Encoder
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto.deriveEncoder
import scalaj.http.{Http, HttpResponse}

import scala.util.Try

class IdentityClient(config: Config) extends StrictLogging {

  val newsletters: List[Newsletter] = List(Holidays, Students, Universities, Teachers, Masterclasses, SocietyWeekly, EdinburghFestivalDataCollection, EventMarketingConsentCollection)

  val optInForms: List[MarketingConsent] = List(EventMarketingConsentCollection)

  def updateConsent(formstackSubmission: FormstackSubmission, newsletter: Newsletter): Option[HttpResponse[String]] = {

    Try {
      Http(s"${config.idapiHost}/consent-email")
        .headers(("Authorization", config.idapiAccessToken), ("Content-type", "application/json"), ("Accept", "text/plain"))
        .postData(IdentityClient.createRequestBody(formstackSubmission.emailAddress, newsletter))
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

  def checkHasOptedIn(formstackSubmission: FormstackSubmission): Boolean = {
    val formId = formstackSubmission.formId

    optInForms.find(f => f.formId == formstackSubmission.formId) match {
          // Extra opt in is required
          case Some(_) =>
            formstackSubmission.opt_in match {
              case Some(optInValue) => optInValue // opt in required. Field present as expected.
              case None =>
                logger.error(s"Opt in error: Missing required opt in data for FormID: $formId")
                false // opt in required BUT missing expected opt in field.
            }
         // Extra opt in NOT required
          case None =>
            formstackSubmission.opt_in match {
              case None => true // opt in not required. No unexpected opt_in field present
              case Some(_) =>
                logger.error(s"Opt in error: Unexpected data received for FormID: $formId")
                false // opt in not required BUT unexpected opt_in field present
            }
        }
    }

  def sendConsentToIdentity(formstackSubmission: FormstackSubmission): Option[APIGatewayProxyResponseEvent] = {
    val newsletterOpt = newsletters.find(n => n.formId == formstackSubmission.formId)

    if (checkHasOptedIn(formstackSubmission)) {
      newsletterOpt.flatMap { newsletter => {
        val response = updateConsent(formstackSubmission, newsletter)
        handleResponseFromIdentity(response, formstackSubmission, newsletter)
      }}
    } else {
      None // form is not submitted due to missing consent opt in requirements
    }
  }
}

object IdentityClient {

  case class IdentityRequest(email: String, listType: List[String])

  private def identityRequestEncoder(newsletter: Newsletter): Encoder[IdentityRequest] = {

    implicit val circeConfig: Configuration = Configuration.default.copy(
      transformMemberNames = {
        case "listType" => newsletter.listType
        case other => other
      }
    )

    deriveEncoder[IdentityRequest]
  }

  def createRequestBody(email: String, newsletter: Newsletter): String = {
    // The key in the JSON sent to Identity depends on the listType. Sometimes the listType is 'set-lists' with a value of the consent name,
    // and sometimes the listType is 'set-consents'. See example body of POST request to IDAPI below.
    //  {
    //    "email" : "example.test@exampledomain.co.uk",
    //    "set-consents" : "holidays"
    //  }

    implicit val identityRequestEncoder = IdentityClient.identityRequestEncoder(newsletter)
    IdentityRequest(email, List(newsletter.consent)).asJson.noSpaces
  }

}

