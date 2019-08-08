# Formstack Consents Lambda

Used to trigger a consent email from IDAPI upon submssion of a [Formstack](https://guardiannewsandmedia.formstack.com) newsletter signup form or marketing consent form via a webhook.

Example Formstack newsletter signup forms used for [Professional Networks]:(https://www.theguardian.com/guardian-professional)
- [Teacher Newsletter](https://www.theguardian.com/teacher-network/2018/mar/21/guardian-teacher-network-newsletter-sign-up)
- [Society Newsletter](https://www.theguardian.com/society/2018/jun/20/society-weekly-email-newsletter-sign-up)


## How the lambda works

Any new newsletter or marketing consent form required must be specified in `com.gu.identity.formstackconsents.Newsletter.scala` wirh corresponding `FormID` from Formstack

1. Upon a submission of one of the Formstack forms, a webhook set up via Formstack's UI sends a POST request to the lambda's `/consent` endpoint triggering this lambda via an API Gateway address.

2. The lambda decodes the email address, form ID, and any additional opt in if required.

3. If successfully decoded, the `IdentityClient` makes a POST request to Identity, triggering a confirmation email to be sent to that user. (Email and newsletter/consent data is carried by the token.)

4. When the user clicks the link in the email, an identity account will be created for them and they will be signed up to the newsletter or have appropriated consents set. 

**Note:** Formstack webhook can be set with coniditional logic in Formstack settings directly, but we perform an addditional check in the lambda as a precaution. 

## Running Serverless Application Model locally

The JSON config for your local development can be downloaded from S3 with the following command:

`sudo aws s3 cp --profile identity s3://identity-private-config/CODE/identity-formstack-consents/formstack-consents.json /etc/gu/`

1. Ensure that is SAM installed. Instructions [here.](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install-mac.html)
2. To run in DEV mode, you need to make two local changes to `cloud-formation.yaml`:
    - Change `CodeUri`  to: `CodeUri: target/scala-2.12/main.jar`
    - Remove both `!Sub` references to `${Stage}` as these will throw an error.
3. Since `sam build` does not support Java 8 using SBT, use `sbt assembly` to run sam locally. Run `sbt assembly && sam local start-api -t cloud-formation.yaml -n /etc/gu/formstack-consents.json`. The -t refers to the template where the cloudformation can be found and the -n refers to a local JSON file that stores the environment variables passed to the lambda. 

## Testing the endpoint
With SAM running locally, make a POST request to http://127.0.0.1:3000/consent with the following example body:

    {
        "FormID": <INSERT-NEWSLETTER-FORM-ID>,
        "UniqueID": "12345678",
        "email_address": <INSERT-YOUR-OWN-EMAIL-ADDRESS>,
        "region": "Other",
        "i_am_": "Other",
        "course": "Other",
        "universitycollegeinstitute": "Other",
        "year_of_graduation": "Sep 2012",
        "HandshakeKey": <INSERT-CODE-FORMSTACK-SHARED-PASSWORD>
    }

*Note: when testing locally, make sure to use your own email address as this will trigger an email to be sent to the specified account*

# Formstack Form Settings
The webhooks set up for each consent form in Formstack require the following options to be set in order for this lambda to work
- "Post with sub-field names" should be ticked
- Post Data Field Key should be set to "Post with API-friendly field keys"
- Content Type should be set to JSON
- Shared secret should be set to the secret defined in config