# Formstack Consents Lambda

Upon a submission to one of the Formstack forms specified in com.gu.identity.formstackconsents.Newsletter, webhooks set up on Formstack forms send a POST request to /consent, triggering this via an API Gateway. A once the email address and form ID has been retrieved from the Formstack submission, a POST request is then made to Identity, triggering a confirmation email to be sent to that user.

When the user clicks the link in the email, an identity account will be created for them and they will be signed up to the newsletter. 

# Running Serless Application Model locally

1. Ensure that is SAM installed. Instructions [here](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install-mac.html)
2. Since `sam build` does not support Java 8 using SBT, use `sbt assembly` to run sam locally. Run `sbt assembly && sam local start-api -t cloud-formation.yaml -n /etc/gu/formstack-consents.json`. The -t refers to the template where the cloudformation can be found and the -n refers to a local JSON file that stores the environment variables passed to the lambda.
