package com.gu.identity.formstackconsents

import com.gu.identity.formstackconsents.FormstackClient.FormstackConsent
import com.gu.identity.globalConfig.DevConfig
import com.typesafe.scalalogging.StrictLogging
import scalaj.http.HttpResponse
import cats.implicits._

class LambdaService(config: DevConfig, formstackClient: FormstackClient, identityClient: IdentityClient) extends StrictLogging {

  def getEmailConsentFromSubmission(submission: FormstackClient.FormstackSubmission): Either[Throwable, FormstackConsent] = {
    val emailField = submission.data.find(field => field._2.label.toLowerCase().contains("email address") && field._2.`type` == "email")
    emailField match {
      case Some(consent) => Right(consent._2)
      case None =>
        val errorMessage = "Unable to find email address field in Formstack submission"
        logger.error(errorMessage)
        Left(new Throwable(errorMessage))
    }
  }

  def sendConsentToIdentity(consent: FormstackConsent, newsletter: Newsletter): Either[Throwable, Unit] = {
    identityClient.sendConsentToIdentity(consent, newsletter)
  }

  def getConsentsAndSendToIdentity(newsletter: Newsletter): Either[Throwable, Unit] = {
    formstackClient.getConsentsForNewsletter(newsletter) match {
      case Left(err) => Left(err)
      case Right(multiplePageResponses) => processPageResponses(multiplePageResponses, newsletter)
    }
  }

  def processPageResponses(pageResponses: List[FormstackClient.FormstackResponse], newsletter: Newsletter): Either[Throwable, List[HttpResponse[String]]] = {
    val submissions: List[FormstackClient.FormstackSubmission] = pageResponses.flatMap(_.submissions)
    val emails: List[Either[Throwable, FormstackConsent]] = submissions.map(getEmailConsentFromSubmission)
    emails.map{ consentsOrErrors =>
      consentsOrErrors.flatMap(consent => identityClient.sendConsentToIdentity(consent, newsletter))
    }.sequence
  }




}
