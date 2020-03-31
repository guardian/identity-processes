package com.gu.identity.eventbriteconsents

import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.gu.identity.eventbriteconsents.clients.{EventbriteClient, IdentityClient}
import com.gu.identity.eventbriteconsents.config.LambdaConfig
import com.gu.identity.eventbriteconsents.services.ConsentsService
import com.typesafe.scalalogging.LazyLogging


object Lambda extends LazyLogging {
  val config: LambdaConfig = LambdaConfig.loadFromEnvironment()
  val eventbriteClient: EventbriteClient = new EventbriteClient
  val idapiClient: IdentityClient = new IdentityClient(config.idapiHost, config.idapiAccessToken)
  val consentsService: ConsentsService = new ConsentsService(config, eventbriteClient, idapiClient)

  def handler(event: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent = {
    consentsService.syncConsents()
    new APIGatewayProxyResponseEvent().withStatusCode(204)
  }

  def main(args: Array[String]): Unit = {
    consentsService.syncConsents()
  }
}

