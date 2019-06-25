# Formstack Consents Lambda

Upon a submission to one of the Formstack forms specified in com.gu.identity.formstackconsents.Newsletter, webhooks set up on Formstack forms send a POST request to /consent, triggering this via an API Gateway. A once the email address and form ID has been retrieved from the Formstack submission, a POST request is then made to Identity, triggering a confirmation email to be sent to that user.

When the user clicks the link in the email, an identity account will be created for them and they will be signed up to the newsletter. 