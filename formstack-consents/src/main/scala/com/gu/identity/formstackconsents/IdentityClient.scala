package com.gu.identity.formstackconsents

import com.gu.identity.globalConfig.DevConfig
import io.circe.syntax._
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.gu.identity.formstackconsents.Lambda.FormstackSubmission
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.extras._
import scalaj.http.{Http, HttpResponse}
import scala.util.Try

class IdentityClient(config: DevConfig) extends StrictLogging {

  val newsletters: List[Newsletter] = List(Holidays, Students, Universities, Teachers, Masterclasses, SocietyWeekly, EdinburghFestivalDataCollection)

  def getResponseFromIdentity(formstackSubmission: FormstackSubmission, newsletter: Newsletter): Option[HttpResponse[String]] = {
    // The key in the JSON sent to Identity depends on the listType. Sometimes the listType is 'set-lists' with a value of the consent name,
    // and sometimes the listType is 'set-consents'. See example below.
    //  {
    //    "email" : "lauren.emms@guardian.co.uk",
    //    "set-consents" : "holidays"
    //  }

    implicit val circeConfig: Configuration = Configuration.default.copy(
      transformMemberNames = {
        case "listType" => newsletter.listType
        case other => other
      }
    )

    @ConfiguredJsonCodec case class IdentityRequest(email: String, listType: List[String])

    val requestBody = IdentityRequest(formstackSubmission.emailAddress, List(newsletter.consent))

    Try {
      Http(s"${config.Identity.host}/consent-email")
        .headers(("Authorization", config.Identity.accessToken), ("Content-type", "application/json"), ("Accept", "text/plain"))
        .postData(requestBody.asJson.noSpaces)
        .asString
    }.toOption
  }

  def handleResponseFromIdentity(response: Option[HttpResponse[String]], formstackSubmission: FormstackSubmission, newsletter: Newsletter): Option[APIGatewayProxyResponseEvent] = {
    response match {
      case Some(r) if r.is2xx =>
        logger.info(s"successfully posted newsletter consent to identity: email: ${formstackSubmission.emailAddress}, newsletter: ${newsletter.consent}")
        val successfulResponse = new APIGatewayProxyResponseEvent
        Some(successfulResponse.withStatusCode(200))
      case Some(r) =>
        logger.error(s"unable to post newsletter consent to identity for email: ${formstackSubmission.emailAddress}, newsletter: ${newsletter.consent}, $r")
        None
      case _ =>
        logger.error(s"unable to connect to identity")
        None
    }
  }

  def sendConsentToIdentity(formstackSubmission: FormstackSubmission): Option[APIGatewayProxyResponseEvent] = {
    val newsletterOpt = newsletters.find(n => n.formId == formstackSubmission.formId)

    newsletterOpt.flatMap { newsletter => {
      val response = getResponseFromIdentity(formstackSubmission, newsletter)
      handleResponseFromIdentity(response, formstackSubmission, newsletter)
    }}
  }
}

