package com.gu.identity.eventbriteconsents.config

import com.gu.identity.eventbriteconsents.clients.EventbriteCredentials

case class LambdaConfig(
  idapiHost: String,
  idapiAccessToken: String,
  masterclassesCredentials: EventbriteCredentials,
  eventsCredentials: EventbriteCredentials,
  syncFrequencyHours: Int,
  isDebug: Boolean,
)

object LambdaConfig {
  private def loadConfigVar(name: String) = Option(System.getenv(name)).filter(_.nonEmpty).getOrElse(throw new RuntimeException(s"missing config $name"))

  def loadFromEnvironment(): LambdaConfig = LambdaConfig(
    idapiHost = loadConfigVar("idapiHost"),
    idapiAccessToken = loadConfigVar("idapiAccessToken"),
    masterclassesCredentials = EventbriteCredentials(loadConfigVar("masterclassesOrganisation"), loadConfigVar("masterclassesToken")),
    eventsCredentials = EventbriteCredentials(loadConfigVar("eventsOrganisation"), loadConfigVar("eventsToken")),
    syncFrequencyHours = loadConfigVar("syncFrequencyHours").toInt,
    isDebug = loadConfigVar("isDebug").toBoolean,
  )
}