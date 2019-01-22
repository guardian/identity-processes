# Payment failure lambda

The lambda is responsible for sending emails (via Braze) related to payment failures of reader revenue products. 
In particular, it includes the user's (encrypted) email in any links in payment failure emails, so that the data can
be used to facilitate login.

The following diagram illustrates how a payment failure event flows through our AWS infrastructure, resulting in the 
lambda being invoked.

event => email queue => [workflow](https://github.com/guardian/membership-workflow) => sns topic => identity queue => identity lambda

## Resources:

- see [this](https://github.com/guardian/identity-platform/blob/master/docs/adr/0000-include-email-in-payment-failure-links.md)
  document which records the architectural decisions made when setting up this lambda.
- see [`test-payment-failure-email.sh`](https://github.com/guardian/membership-workflow/blob/master/dev/test-payment-failure-email.sh)
  in membership-workflow as a way of sending events which will invoke the lambda


## FAQ:

- _How do I send messages on the dead letter queue to the queue that the lambda reads from (so that message processing)
  can be retried?_ 
  
  [This](https://stackoverflow.com/questions/25408158/best-way-to-move-messages-off-dlq-in-amazon-sqs)
  Stackoverflow answer by Rajkumar provides a convenient way. In summary:
  1. set the main SQS queue as the DLQ for the actual DLQ with Maximum Receives as 1
  2. view the content in DLQ (this will move the messages to the main queue as this is the DLQ for the actual DLQ)
  3. remove the setting so that the main queue is no more the DLQ of the actual DLQ