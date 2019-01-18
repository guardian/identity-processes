# Payment failure lambda

The lambda is responsible for sending emails (via Braze) related to payment failures of reader revenue products. 
In particular, it includes the user's (encrypted) email in any links in payment failure emails, so that the data can
be used to facilitate login.

The lambda is invoked by the following series of events:

event => email queue => [workflow](https://github.com/guardian/membership-workflow) => sns topic => identity queue => identity lambda

## Resources:

- see [this](https://github.com/guardian/identity-platform/blob/master/docs/adr/0000-include-email-in-payment-failure-links.md)
  document which records the architectural decisions made when setting up this lambda.
- see [`test-payment-failure-email.sh`](https://github.com/guardian/membership-workflow/blob/master/dev/test-payment-failure-email.sh)
  in membership-workflow as a way of sending events which will invoke the lambda
