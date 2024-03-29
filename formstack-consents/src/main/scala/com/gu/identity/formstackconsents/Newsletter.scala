package com.gu.identity.formstackconsents

sealed trait Newsletter {
  val formId: String
  val listType: String
  val consent: String
}

// used on subsite managed by a third party https://holidays.theguardian.com/newsletter/
// form url https://guardiannewsandmedia.formstack.com/forms/holidays_newsletter
case object Traveller extends Newsletter {
  val formId = "5136217"
  val listType = "set-lists"
  val consent = "guardian-traveller"
}

// Being used on a promotional competetion to win tickets to Glastonbury 2024. Includes a name
// field as well as email. All entrants will be signed up for The Guide newsletter.
// The for will be embeded on an article page behind this custom URL: 
// https://www.theguardian.com/glastonbury-prize-draw-2024
// (as of 19/05/2023, the custom URL has been set up, but the the article is not built yet.)
// form url https://guardiannewsandmedia.formstack.com/forms/the_guide_glastonbury_competition
case object TheGuideGlastonbury2024 extends Newsletter {
  val formId = "5296655"
  val listType = "set-lists"
  val consent = "the-guide-staying-in"
}

case object Students extends Newsletter {
  val formId = "2946711"
  val listType = "set-lists"
  val consent = "guardian-students"
}

case object Universities extends Newsletter {
  val formId = "2946489"
  val listType = "set-lists"
  val consent = "higher-education-network"
}

case object Teachers extends Newsletter {
  val formId = "2799698"
  val listType = "set-lists"
  val consent = "teacher-network"
}

// form url https://guardiannewsandmedia.formstack.com/forms/masterclasses_newsletter
case object Masterclasses extends Newsletter {
  val formId = "5136221"
  val listType = "set-consents"
  val consent = "events"
}

case object SocietyWeekly extends Newsletter {
  val formId = "3082194"
  val listType = "set-lists"
  val consent = "society-weekly"
}

// TODO is this form obsolete?
case object EdinburghFestivalDataCollection extends Newsletter {
  val formId = "3163410"
  val listType = "set-consents"
  val consent = "supporter"
}

case object FeastAppBetaSignup extends Newsletter {
  val formId = "5581302"
  val listType = "set-consents"
  val consent = "feast_app_beta"
}

// Some marketing Forms have an opt in checkbox for consent collection.
// Formstack form can be setup with conditional logic to trigger the lambda webhook,
// but we want tp check again when the submission is decoded as a precaution.
// Add new MarketingConsent to newsletters and optInForms values in IdentityClient
abstract class MarketingConsent extends Newsletter {
  val optInKey: String = "opt_in" //  Request the formstack checkbox hidden label be "opt_in" if possible
}

case object EventMarketingConsentCollection extends MarketingConsent {
  val formId = "3534972"
  val listType = "set-consents"
  val consent = "supporter"
  override val optInKey: String = "supporter_consent_opt_in"
}