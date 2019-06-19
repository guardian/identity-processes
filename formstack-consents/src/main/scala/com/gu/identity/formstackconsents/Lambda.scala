package com.gu.identity.formstackconsents

import com.gu.identity.globalConfig.DevConfig
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.circe.{Json, ParsingFailure}
import scalaj.http.HttpResponse
import io.circe.parser._

object Lambda extends App {

  val newsletters: List[Newsletter] = List(Holidays, Students, Universities, Teachers, Masterclasses, SocietyWeekly, EdinburghFestivalDataCollection)

  def handler(event: APIGatewayProxyRequestEvent): Either[ParsingFailure, Json] = {
    // TODO: look into integrating parameter store through cloudformation
    //    val config = new DevConfig
    //    val formstackClient = new FormstackClient(config)
    //    val identityClient = new IdentityClient(config)
    //    val lambdaService = new LambdaService(config, formstackClient, identityClient)
    //
    //    newsletters.map(lambdaService.getConsentsAndSendToIdentity)
    println("Hello ")
    println(event.getBody)
    val response = """
      {
        "isBase64Encoded": false,
        "statusCode": 200,
        "headers": { "headerName": "headerValue" },
        "body": "body"
      }
    """
    val jsonRes = parse(response)
    println(jsonRes)
    jsonRes
  }
}

