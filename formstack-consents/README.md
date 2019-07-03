# Formstack Consents Lambda

Upon a submission to one of the Formstack forms specified in com.gu.identity.formstackconsents.Newsletter, webhooks set up on Formstack forms send a POST request to /consent, triggering this via an API Gateway. A once the email address and form ID has been retrieved from the Formstack submission, a POST request is then made to Identity, triggering a confirmation email to be sent to that user.

When the user clicks the link in the email, an identity account will be created for them and they will be signed up to the newsletter. 

# Running Serverless Application Model locally

1. Ensure that is SAM installed. Instructions [here](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install-mac.html)
2. Change the CodeUri in cloud-formation.yaml to target/scala-2.12/main.jar
2. Since `sam build` does not support Java 8 using SBT, use `sbt assembly` to run sam locally. Run `sbt assembly && sam local start-api -t cloud-formation.yaml -n /etc/gu/formstack-consents.json`. The -t refers to the template where the cloudformation can be found and the -n refers to a local JSON file that stores the environment variables passed to the lambda.

# Testing the endpoint
With SAM running locally, make a POST request to http://127.0.0.1:3000/consent with the following example body:

`{
    "FormID": <insert-newsletter-form-id>,
    "UniqueID": "12345678",
    "email_address": <insert-your-guardian-email>,
    "region": "Other",
    "i_am_": "Other",
    "course": "Other",
    "universitycollegeinstitute": "Other",
    "year_of_graduation": "Sep 2012",
    "HandshakeKey": <insert-local-dev-password>
}`

The JSON config for your local development can be downloaded with the following command and will be saved in /etc/gu/formstack-consents.json:

# Formstack Form Settings
The webhooks set up for each consent form in Formstack require the following options to be set in order for this lambda to work
- Post with sub-field names should be ticked
- Post Data Field Key should be set to "Post with API-friendly field keys"
- Content Type should be set to JSON
- Shared secret should be set to the secret defined in config