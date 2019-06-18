package com.gu.identity.formstackconsents

import com.gu.identity.globalConfig.DevConfig

object Lambda extends App {

  val newsletters: List[Newsletter] = List(Holidays, Students, Universities, Teachers, Masterclasses, SocietyWeekly, EdinburghFestivalDataCollection)

  def handler(): Unit = {
    // TODO: look into integrating parameter store through cloudformation
//    val config = new DevConfig
//    val formstackClient = new FormstackClient(config)
//    val identityClient = new IdentityClient(config)
//    val lambdaService = new LambdaService(config, formstackClient, identityClient)
//
//    newsletters.map(lambdaService.getConsentsAndSendToIdentity)
    println("Hello")
  }
}

