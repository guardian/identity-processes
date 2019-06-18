package com.gu.identity.formstackconsents

import com.gu.identity.globalConfig.DevConfig
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent

object Lambda extends App {

  val newsletters: List[Newsletter] = List(Holidays, Students, Universities, Teachers, Masterclasses, SocietyWeekly, EdinburghFestivalDataCollection)

  def handler(event: APIGatewayProxyRequestEvent): Unit = {
    // TODO: look into integrating parameter store through cloudformation
//    val config = new DevConfig
//    val formstackClient = new FormstackClient(config)
//    val identityClient = new IdentityClient(config)
//    val lambdaService = new LambdaService(config, formstackClient, identityClient)
//
//    newsletters.map(lambdaService.getConsentsAndSendToIdentity)
    println("Hello ")
    println(event.getBody)
  }
}

