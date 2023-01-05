# Formstack Baton Requests

Formstack Baton Requests consists of 2 step functions and a number of lambdas, which work together to make Subject Access and Right to Erasure Requests to Formstack.

## Step functions
As it can take some time to update DynamoDb with Formstack submissions, we use step functions to overcome the 15 minute time limit of lambdas. 

### FormstackSar
The `FormstackSar` step function is triggered by the `FormstackSarHandler` with an `UpdateDynamoRequest` and has the followings stages:
1. Adds an `accountNumber` parameter to each `UpdateDynamoRequest` (This is a leftover from when there used to be 2 formstack accounts)
2. Triggers the `UpdateDynamoHandler` (see more information on this lambda below).
3. Performs a check to see if the status in the `UpdateDynamoResponse` is `Completed`, if it's not, the `UpdateDynamoHandler` is triggered again to continue the update. If a `Completed` status is found, the update branch is completed for that account's token.
4. Once updates are completed for both tokens, a the `FormstackPerformSarHandler` is triggered (see more information on this lambda below). Steps 2, 3 and 4 are then performed for both accounts.

### FormstackRer
The `FormstackRer` step function is triggered by the `FormstackRerHandler` with an `UpdateDynamoRequest` and has the followings stages:
1. Adds an `accountNumber` parameter to each `UpdateDynamoRequest` ( this is a leftover from when there used to be 2 formstack accounts)
2. Triggers the `UpdateDynamoHandler` (see more information on this lambda below).
3. Performs a check to see if the status in the `UpdateDynamoResponse` is `Completed`, if it's not, the `UpdateDynamoHandler` is triggered again to continue the update. If a `Completed` status is found, the update branch is completed for that account's token.
4. Once updates are completed for both tokens, a the `FormstackPerformRerHandler` is triggered (see more information on this lambda below).

## Lambdas

### UpdateDynamoHandler
The UpdateDynamoHandler updates DynamoDb with submissions received by Formstack since the last run of the lambda. This happens in 4 steps:
1. The date of the last time the submissions table was updated is retrieved from a separate table in DynamoDb. Each Formstack account (we have 2) has an entry for when the table was last updated. 
2. A call is made to Formstack to get all forms and, for each form, further calls are made to get submissions since the last run date. 
3. The submissions are filtered down to those containing email addresses. These email addresses, along with the corresponding submission IDs, are written to the submissions table in DynamoDb. 
4. After each page of forms returned by the Formstack API have been processed, the UpdateDynamoHandler checks the remaining time the lambda has to run. If it is less than 5 minutes but there are still more forms to be processed, an UpdateDynamoResponse is returned with a `Pending` status. Otherwise, it has a `Completed` status.
5. Once a `Completed` status is returned, the DynamoDb table storing the last run date is updated for that account.

### FormstackSarHandler
The `FormstackSarHandler` is called from [Baton](https://github.com/guardian/baton) with two types of requests.
1. An **initiate** request generates a UUID, which is then used by Baton to make subsequent status checks, and triggers the `FormstackSar` step function, which updates DynamoDb with recent Formstack submissions and makes a Subject Access Request.
2. A **status** request makes calls to S3 to check if an object exists in the results bucket with a /completed or /failed path. If an object exists in the /failed path, a Failed response is returned to Baton. If an object exists in the /completed path, a Completed response is returned to Baton with the results locations. If no object exists for a given ID in neither the /completed path nor the /failed path, then a Pending response is returned to Baton.

### FormstackPerformSarHandler
The `FormstackPerformSarHandler` works as follows:
1. Makes a query to see if any submissions exist in DynamoDb with the user in question's email address.
2. If submissions are found, a call is made to Formstack to retrieve data for each submission.
3. This data is then written to a /completed path in S3. If there are no results, an empty object is created. The `FormstackSarHandler` status checks look for objects existing in this path to indicate a successful Subject Access Request. If the `FormstackPerformSarHandler` is unsuccessful, it writes to a /failed path in S3.

### FormstackRerHandler
The `FormstackRerHandler` is called from [Baton](https://github.com/guardian/baton) with two types of requests.
1. An **initiate** request generates a UUID, which is then used by Baton to make subsequent status checks, and triggers the `FormstackRer` step function, which updates DynamoDb with recent Formstack submissions and makes a Right to Erasure Request.
2. A **status** request makes calls to S3 to check if an object exists in the results bucket with a /completed or /failed path. If an object exists in the /failed path, a Failed response is returned to Baton. If an object exists in the /completed path, a Completed response is returned to Baton. If no object exists for a given ID in neither the /completed path nor the /failed path, then a Pending response is returned to Baton.

### FormstackPerformRerHandler
The `FormstackPerformRerHandler` works as follows:
1. Makes a query to see if any submissions exist in DynamoDb with the user in question's email address.
2. If submissions are found, a call is made to Formstack to delete the data for each submission.
3. The email address and submission entries are deleted from the DynamoDb submissions table.
4. An empty object is then written to the /completed path in S3. The `FormstackRerHandler` status checks look for objects existing in this path to indicate a successful Right to Erasure Request. If the `FormstackPerformRerHandler` is unsuccessful, it writes to a /failed path in S3.

## How to:
### Test locally
There is a `FormstackBatonLambdaLocalRun` object available for local testing. Simply get Identity credentials and replace the method call at the bottom with the method and request you'd like to test. Because the `UpdateDynamoHandler` requires an AWS context, you will also have to comment out the `context.getRemainingTimeInMillis > 300000` check in the `DynamoUpdateService` if you wish to test this lambda locally.

### Update the DynamoDb submissions table for all entries.
If, for some reason, you want to repopulate the entire submissions DynamoDb table - for example, if we lose track of when it was last updated - there are a couple of steps to take.
1. (Optional) Update the CloudFormation template to remove the submissions tables and then re-add it. If you choose to do this, it should always be done via CloudFormation. This will give you a clean, empty table. The reason this is optional is because Dynamo will overwrite anything with the same primary key, which in this case is the Submission ID and email address, so deleting the table is not obligatory.
2. Change the last updated dates for both accounts to 1970-01-01 00:00:00. This can be done via the AWS console. This date is early enough to retrieve all submissions we have in Formstack.
3. Trigger the `FormstackSar` step function using the `formstack-baton-sar-lambda-CODE` lambda function in the AWS console. This can be done by triggering a test event with a test email address (like the one below). The justification behind triggering the whole step function, rather than just the `UpdateDynamoHandler` itself, is that the step function will deal with the `UpdateDynamoHandler` exceeding Lambda's runtime limits. Once the table has been updated, it will also update the `lastUpdated` field in the last updated DynamoDb table so that the next Formstack SAR or RER will only update since the most recent run.
```
{
  "requestType": "SAR",
  "action": "initiate",
  "dataProvider": "formstack",
  "subjectEmail": "email1@email.com"
}
```