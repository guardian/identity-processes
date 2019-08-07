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

// Marketing Forms requiring opt in check for consent collection.
// Formstack form can be setup with conditional logic to trigger the lambda webhook,
// but we checked again when the submission is decoded as a precaution.
// key-value pair must match Formstack submission data.
abstract class MarketingConsent extends Newsletter {
  val optInKey: String
  val optInValue: String
}

case object EventMarketingConsentCollection extends MarketingConsent {
  val formId = "3534972"
  val listType = "set-consents"
  val consent = "supporter"
  override val optInKey: String = "supporter_consent_opt_in"
  override val optInValue: String = "Opt in"
}