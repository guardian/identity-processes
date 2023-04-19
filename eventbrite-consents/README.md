# Eventbrite consents sync Lambda

The service is a Scala lambda which sends emails to users who tick the 'hear more about guardian news and media' check box in Eventbrite.

## Lambda Details

Eventbrite is a third party service where users can sign up for events. As part of the Eventbrite flow users can click a checkbox labelled

```
Please tick if you are interested in hearing about these products and services from Guardian News & Media 
```

The Lambda retrieves a list of emails for users who responded positively to the question from the Eventbrite API. The lambda passes the emails to 
the `/consent-emails` endpoint on Identity API which triggers confirmation emails for the events consent.

## Deployments

There is no CODE environment for Eventbrite. For testing we have a DEV Lambda which is configured with `isDebug=true`. This prevents the lambda
from updating the consents in Identity. 

## Running Locally

To run the app locally set the environment variables for the app (including API tokens). The parameters used
in DEV and PROD can be found in the Lambda environment configuration on the AWS console. Be careful, if `isDebug=false` then
emails could potentially be sent to users if idapiHost and idapiAccessToken are set.

Run with SBT
```
export idapiHost=not_used_in_debug
export idapiAccessToken=not_used_in_debug
export masterclassesOrganisation=set_this
export masterclassesToken=set_this
export eventsOrganisation=set_this
export eventsToken=set_this
export syncFrequencyHours=4
export isDebug=true
sbt run
```

or run the com.gu.identity.eventbriteconsents.Lambda.main function with environment variables set in Intellij.

## Unit tests

```
sbt test
```
