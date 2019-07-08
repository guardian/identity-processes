import com.gu.identity.formstackconsents.{FormstackSubmission, Holidays, IdentityClient, Students}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar

class IdentityClientTest extends WordSpec with Matchers with MockitoSugar {

  val email = "test@exampledomain.com"

  val formstackSubmission = FormstackSubmission("3082194", "test@exampledomain.com", "secretkey")

  val holidayRequestBody = "{\"email\":\"test@exampledomain.com\",\"set-consents\":[\"holidays\"]}"

  val studentsRequestBody = "{\"email\":\"test@exampledomain.com\",\"set-lists\":[\"guardian-students\"]}"

  "The IdentityClient" should {
    "Correctly encode an Identity Request when the newsletter list-type is set-consents" in {
      IdentityClient.createRequestBody(email, Holidays).shouldEqual(holidayRequestBody)
    }

    "Correctly encode an Identity Request when the newsletter list-type is set-lists" in {
      IdentityClient.createRequestBody(email, Students).shouldEqual(studentsRequestBody)
    }
  }
}
