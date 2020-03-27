package com.gu.identity.eventbriteconsents.config

case class LambdaConfig(
  idapiHost: String,
  idapiAccessToken: String,
  masterclassesToken: String,
  eventsToken: String,
  syncFrequencyHours: Int
)

object LambdaConfig {
  private def loadConfigVar(name: String) = Option(System.getenv(name)).getOrElse(throw new RuntimeException(s"missing config $name"))

  def loadFromEnvironment(): LambdaConfig = LambdaConfig(
    idapiHost = loadConfigVar("idapiHost"),
    idapiAccessToken = loadConfigVar("idapiAccessToken"),
    masterclassesToken = loadConfigVar("masterclassesToken"),
    eventsToken = loadConfigVar("eventsToken"),
    syncFrequencyHours = loadConfigVar("syncFrequencyHours").toInt,
  )
}