package com.gu.identity.formstackconsents

import com.gu.identity.formstackconsents.FormstackClient.FormstackConsent
import com.gu.identity.globalConfig.DevConfig
import io.circe.syntax._
import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.extras._
import scalaj.http.Http

class IdentityClient(config: DevConfig) extends StrictLogging {
  def sendConsentToIdentity(formstackConsent: FormstackConsent, newsletter: Newsletter): Either[Throwable, Unit] = {

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

    val requestBody = IdentityRequest(formstackConsent.value, List(newsletter.consent))

    val response = Http(s"${config.Identity.host}/consent-email")
      .headers(("Authorization", config.Identity.accessToken), ("Content-type", "application/json"), ("Accept", "text/plain"))
      .postData(requestBody.asJson.noSpaces)

    Either.catchNonFatal(response.asString)
      .leftMap(err => new Throwable(s"Connection to identity failed: $err"))
      .flatMap {
        case res if res.is2xx =>
          Right(logger.info(s"successfully posted newsletter consent to identity: email: ${formstackConsent.value}, newsletter: ${newsletter.consent}"))
        case res =>
          val errorMessage = s"unable to post newsletter consent to identity: email: ${formstackConsent.value}, newsletter: ${newsletter.consent}"
          logger.error(errorMessage)
          Left(new Throwable(s"$errorMessage: ${res.body}"))

      }
  }
}

