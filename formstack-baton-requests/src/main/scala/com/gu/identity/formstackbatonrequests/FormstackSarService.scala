package com.gu.identity.formstackbatonrequests

import com.typesafe.scalalogging.LazyLogging
import scalaj.http.Http
import io.circe.parser.decode
import com.gu.identity.formstackbatonrequests.aws.SubmissionTableUpdateDate
import io.circe.Json
import io.circe.generic.JsonCodec

trait FormstackSar {
  def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormstackFormsResponse]
  def formSubmissionsForGivenPage(page: Int, formId: String, minTime: SubmissionTableUpdateDate, encryptionPassword: String, accountToken: FormstackAccountToken): Either[Throwable, FormstackFormSubmissionsResponse]
}
/* Codecs for decoding accountFormsForGivenPage response */
@JsonCodec case class FormstackForm(id: String)
@JsonCodec case class FormstackFormsResponse(forms: List[FormstackForm], total: Int)

/* Codecs for decoding formSubmissionsForGivenPage response */
@JsonCodec case class FormstackResponseValue(value: Json)
@JsonCodec case class FormstackSubmission(id: String, data: Map[String, FormstackResponseValue])
@JsonCodec case class FormstackFormSubmissionsResponse(submissions: List[FormstackSubmission], pages: Int)

case class FormstackDecryptionError(message: String) extends Throwable

object FormstackSarService extends FormstackSar with LazyLogging {

  val resultsPerPage = 25

  override def accountFormsForGivenPage(page: Int, accountToken: FormstackAccountToken): Either[Throwable, FormstackFormsResponse] = {
    val response = Http(s"https://www.formstack.com/api/v2/form.json")
      .header("Authorization", accountToken.secret)
      .params(
        Seq(
          ("page", page.toString),
          ("per_page", resultsPerPage.toString)
        )
      ).asString

    if(!response.is2xx) {
      logger.error(response.body)
    }
    decode[FormstackFormsResponse](response.body)
  }

  override def formSubmissionsForGivenPage(
    page: Int,
    formId: String,
    minTime: SubmissionTableUpdateDate,
    encryptionPassword: String,
    accountToken: FormstackAccountToken): Either[Throwable, FormstackFormSubmissionsResponse] = {
    println("getting submissions since" + minTime.date)
    val response = Http(s"https://www.formstack.com/api/v2/form/$formId/submission.json")
      .headers(
        Seq(
          ("Authorization", accountToken.secret),
          ("X-FS-ENCRYPTION-PASSWORD", encryptionPassword)
        )
      )
      .params(
        Seq(
          ("page", page.toString),
          ("per_page", resultsPerPage.toString),
          ("data", "true"),
          ("expand_data", "true"),
          ("sort", "DESC"),
          ("min_time", minTime.date)
        )
      ).asString

    if(!response.is2xx) {
      logger.error(response.body)
    }

    /* There are a couple of forms that the Formstack API can't seem to decrypt. There seems to be no way around this
     *  so we capture this specific error and skip these forms. */
    if(response.body.contains("An error occurred while decrypting the submissions"))
      Left(FormstackDecryptionError(response.body))
    else
      logger.info(response.body)
      decode[FormstackFormSubmissionsResponse](response.body)
  }
}
