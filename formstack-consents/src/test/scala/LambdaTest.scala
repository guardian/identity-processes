import com.gu.identity.formstackconsents.{FormstackSubmission, Lambda}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar

class LambdaTest extends WordSpec with Matchers with MockitoSugar {

  val validEventBody1 =
    """
      |{
      |    "FormID": "3082194",
      |    "UniqueID": "1234567",
      |    "HandshakeKey": "secretkey",
      |    "email_address": "test@exampledomain.com",
      |    "region": "Please select from list",
      |    "employment_sector": "Please select from list",
      |    "role": null,
      |    "position": null,
      |    "interests": null
      |}
    """.stripMargin

  val validEventBody2 =
    """
      |{
      |    "FormID": "3082194",
      |    "UniqueID": "1234567",
      |    "HandshakeKey": "secretkey",
      |    "your_email_address": "test@exampledomain.com",
      |    "region": "Please select from list",
      |    "employment_sector": "Please select from list",
      |    "role": null,
      |    "position": null,
      |    "interests": null
      |}
    """.stripMargin

  val invalidEventBody =
    """
      |{
      |    "FormID": "3082194",
      |    "UniqueID": "1234567",
      |    "HandshakeKey": "secretkey",
      |    "region": "Please select from list",
      |    "employment_sector": "Please select from list",
      |    "role": null,
      |    "position": null,
      |    "interests": null
      |}
    """.stripMargin

  val optedInEventBody =
    """
      |{
      |    "FormID": "3534972",
      |    "UniqueID": "1234567",
      |    "HandshakeKey": "secretkey",
      |    "your_email_address": "test@exampledomain.com",
      |    "supporter_consent_opt_in": "Opt in"
      |}
    """.stripMargin

  val notOptedInEventBody =
    """
      |{
      |    "FormID": "3534972",
      |    "UniqueID": "1234567",
      |    "HandshakeKey": "secretkey",
      |    "your_email_address": "test@exampledomain.com",
      |    "supporter_consent_opt_in": null
      |}
    """.stripMargin


  val formstackSubmission = FormstackSubmission("3082194", "test@exampledomain.com", "secretkey", None)
  val optedInFormstackSubmission = FormstackSubmission("3534972", "test@exampledomain.com", "secretkey", Some(true))
  val notOptedInFormstackSubmission = FormstackSubmission("3534972", "test@exampledomain.com", "secretkey", Some(false))

  "The Lambda" should {
    "Successfully decode a valid event body with email field key as 'email_address'" in {
      Lambda.decodeFormstackSubmission(validEventBody1).shouldEqual(Some(formstackSubmission))
    }

    "Successfully decode a valid event body with email field key as 'your_email_address'" in {
      Lambda.decodeFormstackSubmission(validEventBody2).shouldEqual(Some(formstackSubmission))
    }

    "Unsuccessfully decode an invalid event body without an email field" in {
      Lambda.decodeFormstackSubmission(invalidEventBody).shouldEqual(None)
    }
    "Successfully decode event body if it has additional opt in field" in {
       Lambda.decodeFormstackSubmission(optedInEventBody).shouldEqual(Some(optedInFormstackSubmission))
    }
    "Successfully decode an event body if additional opt in field is null" in {
       Lambda.decodeFormstackSubmission(notOptedInEventBody).shouldEqual(Some(notOptedInFormstackSubmission))
    }
  }
}