package com.gu.identity.formstackconsents

sealed trait Newsletter {
  val formId: String
  val listType: String
  val consent: String
}

case object Holidays extends Newsletter {
  val formId = "1945214"
  val listType = "set-consents"
  val consent = "holidays"
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

case object Masterclasses extends Newsletter {
  val formId = "1898609"
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