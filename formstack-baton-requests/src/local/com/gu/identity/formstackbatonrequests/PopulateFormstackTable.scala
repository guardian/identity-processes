package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.aws.{Dynamo, S3, SubmissionTableUpdateDate}
import com.gu.identity.formstackbatonrequests.services.{DynamoUpdateService, FormstackService}
import com.typesafe.scalalogging.LazyLogging

/* Script used to populate Formstack table from empty. When .updateDynamo is not provided a date, Dynamo will be
 * updated with ALL submissions from Formstack */
object PopulateFormstackTable extends App with LazyLogging {

  val defaultConfig = FormstackConfig.getPerformHandlerConfig
  val dynamoUpdateService = DynamoUpdateService(FormstackService, Dynamo(), defaultConfig)

  val updateDynamoResult = dynamoUpdateService.updateDynamo(SubmissionTableUpdateDate("lastUpdated", "1970-01-01 00:00:00"))
  updateDynamoResult match {
    case Left(err) =>
      logger.error("Unable to complete update on formstack-submissions-ids table.", err)
    case Right(_) =>
      logger.info("Successfully completed updates on formstack-submissions-ids and formstack-submissions-last-updated tables.")
  }
}