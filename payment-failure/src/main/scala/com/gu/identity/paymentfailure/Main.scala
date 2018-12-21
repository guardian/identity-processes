package com.gu.identity.paymentfailure

object Main extends App {

  val testIdentityClient = new IdentityClient
  val testBrazeClient = new BrazeClient
  val testSendEmailService = new SendEmailService(testIdentityClient, testBrazeClient)
  val testEmailData = IdentityBrazeEmailData("123","test@test.com", "testTemplate", Map.empty)
  val testConfig = Config("https://idapi.thegulocal.com", "https://testEndpoint", "dev-login-token","testUrl")
  val t = testSendEmailService.sendEmail(testEmailData, testConfig)
  println(t)
}
