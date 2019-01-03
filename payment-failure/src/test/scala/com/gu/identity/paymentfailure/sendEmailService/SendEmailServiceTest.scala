package com.gu.identity.paymentfailure.sendEmailService

import com.gu.identity.paymentfailure._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

class SendEmailServiceTest extends WordSpec with Matchers with MockitoSugar {

  trait TestFixture {
    val config = mock[Config]
    val identityClient = mock[IdentityClient]
    val brazeClient = mock[BrazeClient]
    val sendEmailService = new SendEmailService(identityClient, brazeClient)
  }

  "The sendEmail method" should {
    "encrypt an email then trigger an email in Braze" in new TestFixture {

      val emailData = IdentityBrazeEmailData("1111", "test@test.com", "templateIdMock", Map("first name" -> "test name"))

      when(identityClient.encryptEmail("test@test.com", config)).thenReturn(Right(IdentityEmailTokenResponse("OK", "encryptedEmailString")))
      when(brazeClient.sendEmail(emailData, "encryptedEmailString", config)).thenReturn(Right(BrazeUnitResponse("success")))
      sendEmailService.sendEmail(emailData, config)

      verify(identityClient, times(1)).encryptEmail("test@test.com", config)
      verify(brazeClient, times(1)).sendEmail(emailData, "encryptedEmailString", config)
    }

    "not trigger a braze email if email encryption fails" in new TestFixture {
      val emailData = IdentityBrazeEmailData("1111", "test@test.com", "templateIdMock", Map("first name" -> "test name"))

      val mockException = mock[Exception]
      when(identityClient.encryptEmail("test@test.com", config)).thenReturn(Left(mockException))

      sendEmailService.sendEmail(emailData, config)

      verify(identityClient, times(1)).encryptEmail("test@test.com", config)
      verify(brazeClient, never()).sendEmail(emailData, "encryptedEmailString", config)

    }
  }
}