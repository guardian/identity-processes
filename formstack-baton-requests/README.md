# Formstack Baton Requests

Formstack Baton Requests is concerned with making Subject Access Requests to Formstack.

## FormstackSarHandler
The Formstack SAR Lambda is called from [Baton](https://github.com/guardian/baton) with two types of requests.
1. An **initiate** request generates a UUID, which is then used by Baton to make subsequent status checks, and triggers the FormstackPerformSarLambda, which performs the requests to Formstack.
2. A **status** request makes calls to S3 to check if an object exists in the results bucket with a completed or failed path. Otherwise, if an object exists in the failed path, a Failed response is returned to Baton. If an object exists in the completed path, a Completed response is returned to Baton with the results location. If no object exists for a given ID in neither the completed path nor the failed path, then a Pending response is returned to Baton.

## FormstackPerformSarLambda 
**COMING SOON**