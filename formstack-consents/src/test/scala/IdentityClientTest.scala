import com.gu.identity.formstackconsents._
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

class IdentityClientTest extends WordSpec with Matchers with MockitoSugar {

  val config = Lambda.Config("mock", "mock", "mock")
  val idClient = new IdentityClient(config)

  val email = "test@exampledomain.com"

  val formstackSubmission = FormstackSubmission("3082194", "test@exampledomain.com", "secretkey", None)

  val optedInFormstackSubmission = FormstackSubmission("3534972", "test@exampledomain.com", "secretkey", Some(true))

  val notOptedInFormstackSubmission = FormstackSubmission("3534972", "test@exampledomain.com", "secretkey", Some(false))

  val faultyFormstackSubmission1 = FormstackSubmission("3082194", "test@exampledomain.com", "secretkey", Some(true)) // no opt in required
  val faultyFormstackSubmission2 = FormstackSubmission("3534972", "test@exampledomain.com", "secretkey", None) // missing required opt in

  val supporterRequestBody = "{\"email\":\"test@exampledomain.com\",\"set-consents\":[\"supporter\"]}"

  val studentsRequestBody = "{\"email\":\"test@exampledomain.com\",\"set-lists\":[\"guardian-students\"]}"

  "The IdentityClient" should {
    "Correctly encode an Identity Request when the newsletter list-type is set-consents" in {
      IdentityClient.createRequestBody(email, EventMarketingConsentCollection).shouldEqual(supporterRequestBody)
    }

    "Correctly encode an Identity Request when the newsletter list-type is set-lists" in {
      IdentityClient.createRequestBody(email, Students).shouldEqual(studentsRequestBody)
    }
  }

  "IdentityClient.checkHasOptedIn" should {
    "Return true if an opt in is not required for the form" in {
      idClient.checkHasOptedIn(formstackSubmission).shouldEqual(true)
    }
    "Return true if the formstack submission has correctly opted in" in {
      idClient.checkHasOptedIn(optedInFormstackSubmission).shouldEqual(true)
    }
    "Return false if the formstack submission has not opted in" in {
      idClient.checkHasOptedIn(notOptedInFormstackSubmission).shouldEqual(false)
    }
    "Return false if the newsletter has unexpected opt_in field" in {
      idClient.checkHasOptedIn(faultyFormstackSubmission1).shouldEqual(false)
    }
    "Return false if the formstack submission is missing the opt in field requirement" in {
      idClient.checkHasOptedIn(faultyFormstackSubmission2).shouldEqual(false)
    }
  }
}
