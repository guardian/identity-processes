# Formstack Consents Lambda

This lambda is responsible for making calls to Formstack, getting the email address of each user who has signed up to newsletters via Formstack and making a subsequent POST request to Identity API /consent-email with the user's email address and the newsletter they signed up to. This will then trigger a confirmation email for that user. 

When the user clicks the link in the email, an identity account will be created for them and they will be signed up to the newsletter. 