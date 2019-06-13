package com.gu.identity.formstackconsents

import com.gu.identity.globalConfig.DevConfig
import cats.syntax.either._
import com.gu.identity.formstackconsents.FormstackClient.FormstackResponse
import com.typesafe.scalalogging.StrictLogging
import io.circe.parser.decode
import scalaj.http.Http
import io.circe.generic.JsonCodec
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec, JsonKey}
import org.joda.time.DateTime

class FormstackClient(config: DevConfig) extends StrictLogging {

  def requestForMultiplePages(res: FormstackResponse, newsletter: Newsletter): List[FormstackResponse] = {
    val pagesList = (2 to res.pages).toList
    pagesList.foldLeft(List(res)){ (acc, page) => {
      getSubmissionsForGivenPage(newsletter.formId, page) match {
        case Left(_) => List(res)
        case Right(response) => response :: acc
      }
    }}
  }

  def getSubmissionsForGivenPage(newsletterId: String, page: Int): Either[Throwable, FormstackResponse] = {
    val request = Http(s"${config.Formstack.host}/api/v2/form/$newsletterId/submission.json")
      .header("Authorization", config.Formstack.token)
      .params(Seq(
        ("status", "attending"),
        ("page", page.toString),
        // TODO: how to manage this so that it is from the last time it ran?
        ("min_time", DateTime.now.minusDays(1).toString()),
        // Formstack api only allows 25 at a time
        ("per_page", "25"),
        ("encryption_password", config.Formstack.password),
        ("data", "1")
      ))

    Either.catchNonFatal(request.asString)
      .flatMap {
        case res if res.is2xx => decode[FormstackResponse](res.body)
        case res =>
          val errorMessage = s"unable to get submissions for newsletter $newsletterId page ${page.toString}"
          logger.error(errorMessage)
          Left(new Throwable(s"$errorMessage: ${res.body}"))
      }
  }

  def getConsentsForNewsletter(newsletter: Newsletter): Either[Throwable, List[FormstackResponse]] = {
    val request = getSubmissionsForGivenPage(newsletter.formId, page = 1)
    request.flatMap(res => res.pages match {
      case 1 => Right(List(res))
      case _ => Right(requestForMultiplePages(res, newsletter))
    })
  }
}

object FormstackClient {
  implicit val config: Configuration = Configuration.default
  // Scala won't allow 'type' as an argument so specifying it used @JsonKey
  @ConfiguredJsonCodec case class FormstackConsent(field: String, label: String, @JsonKey("type") consentType: String, value: String)
  @JsonCodec case class FormstackSubmission(data: Map[String, FormstackConsent])
  @JsonCodec case class FormstackResponse(submissions: List[FormstackSubmission], pages: Int)
}

