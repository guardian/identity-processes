package com.gu.identity.formstackconsents

import com.gu.identity.formstackconsents.FormstackClient.FormstackConsent
import com.gu.identity.globalConfig.DevConfig
import com.typesafe.scalalogging.StrictLogging

class LambdaService(config: DevConfig, formstackClient: FormstackClient, identityClient: IdentityClient) extends StrictLogging {

  def getEmailConsentFromSubmission(submission: FormstackClient.FormstackSubmission): Either[Throwable, FormstackConsent] = {
    val emailField = submission.data.find(field => field._2.label.toLowerCase().contains("email address") && field._2.consentType == "email")
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
      case Right(multiplePageResponses) => Right(multiplePageResponses
        .map(pageResponse => pageResponse.submissions
          .map(submission => getEmailConsentFromSubmission(submission)
            .map(email => sendConsentToIdentity(email, newsletter)))))
    }
  }
}
