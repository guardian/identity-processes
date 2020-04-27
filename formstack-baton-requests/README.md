# Formstack Baton Requests

Formstack Baton Requests consists of 2 lambdas, which work together to make Subject Access Requests to Formstack.

## FormstackSarHandler
The Formstack SAR Lambda is called from [Baton](https://github.com/guardian/baton) with two types of requests.
1. An **initiate** request generates a UUID, which is then used by Baton to make subsequent status checks, and triggers the FormstackPerformSarLambda, which performs the requests to Formstack.
2. A **status** request makes calls to S3 to check if an object exists in the results bucket with a completed or failed path. Otherwise, if an object exists in the failed path, a Failed response is returned to Baton. If an object exists in the completed path, a Completed response is returned to Baton with the results location. If no object exists for a given ID in neither the completed path nor the failed path, then a Pending response is returned to Baton.

## FormstackPerformSarLambda 
The FormstackPerformSarLambda works as follows:

1. Updates Dynamo with submissions received by Formstack since the last run of the lambda. This happens in 4 steps:
    - The date of the last time the submissions table was updated is retrieved from a separate table in Dynamo. 
    - A call is made to Formstack to get all forms and, for each form, further calls are made to get submissions since the last run date. 
    - The submissions are filtered down to those containing email addresses. These email addresses, along with the corresponding submission IDs, are written to the submissions table in Dynamo. 
    - The Dynamo table storing the last run date is updated. 
2. Once the submissions table is updated, a query is made to see if any submissions exist with the user in question's email address.
3. If submissions are found, a call is made to Formstack to retrieve data for each submission.
4. This data is then written to a /completed path in S3. If there are no results, an empty object is created. The FormstackSarLambda status checks look for objects existing in this path to indicate a successful Subject Access Request.