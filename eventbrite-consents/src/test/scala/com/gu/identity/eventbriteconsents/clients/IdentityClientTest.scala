package com.gu.identity.eventbriteconsents.clients

import org.scalatest.FlatSpec
import org.slf4j.{Logger => Underlying}
import com.typesafe.scalalogging.Logger
import org.mockito.Mockito._

class IdentityClientTest extends FlatSpec {

  def identityClient(mocked: Underlying): IdentityClient = {
    new IdentityClient("idapiUrl", "idapiToken") {
      override lazy val logger = Logger(mocked)
    }
  }

  val mockedLogger = mock(classOf[Underlying])
  val emailAddress = "someEmailAddress"
  val invalidEmailResponse = "{\"status\":\"error\",\"errors\":[{\"message\":\"Bad Request\",\"description\":\"Bad email format, Error(Registration Error,There was an error with your registration.,None)\"}]}"
  val someOtherErrorResponse = "{\"status\":\"error\",\"errors\":[{\"message\":\"Bad Request\",\"description\":\"Something else bad happened\"}]}"
  val nonJsonErrorResponse = "some other error message"

  "handleErrorResponse" should "log invalid email errors" in {
    when(mockedLogger.isErrorEnabled()).thenReturn(true)

    identityClient(mockedLogger).handleErrorResponse(400, invalidEmailResponse, emailAddress)
    verify(mockedLogger).error("Invalid email address, could not process consents: " + emailAddress)
  }

  "handleErrorResponse" should "throw runtime exception for other errors" in {
    when(mockedLogger.isErrorEnabled()).thenReturn(true)

    assertThrows[RuntimeException] {
      identityClient(mockedLogger).handleErrorResponse(400, someOtherErrorResponse, emailAddress)
    }
  }

  "handleErrorResponse" should "throw runtime exception for non-json error responses" in {
    when(mockedLogger.isErrorEnabled()).thenReturn(true)

    assertThrows[RuntimeException] {
      identityClient(mockedLogger).handleErrorResponse(400, nonJsonErrorResponse, emailAddress)
    }
  }

}
