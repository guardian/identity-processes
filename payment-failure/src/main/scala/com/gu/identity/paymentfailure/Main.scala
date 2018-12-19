package com.gu.identity.paymentfailure

import com.gu.identity.paymentfailure.Model.IdentityBrazeEmailData

object Main extends App {

  val testIdentityClient = new IdentityClient
  val testBrazeClient = new BrazeClient
  val testSendEmailService = new SendEmailService(testIdentityClient, testBrazeClient)
  val testEmailData = IdentityBrazeEmailData("123","test@test.com", "testTemplate", Map.empty)
  val t = testSendEmailService.sendEmail(testEmailData)
  println(t)
}
