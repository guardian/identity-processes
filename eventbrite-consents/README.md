# Eventbrite consents sync Lambda

Scala lambda which hits the Eventbrite API and finds answers where users have ticked the 'hear more about guardian news and media' check box.

For positive answers the lambda hits the consent-emails endpoint idapi to trigger a confirmation email.

## Deployments

There is no CODE environment for Eventbrite. For testing we have a DEV Lambda which is configured with `isDebug=true`. This prevents the lambda
from updating the consents in Identity.
