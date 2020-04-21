package com.gu.identity.formstackbatonrequests

import com.gu.identity.formstackbatonrequests.aws.{Dynamo, SubmissionTableUpdateDate}
import com.typesafe.scalalogging.LazyLogging

/* Script used to populate Formstack table from empty. When .updateDynamo is not provided a date, Dynamo will be
 * updated with ALL submissions from Formstack */
object PopulateFormstackTable extends App with LazyLogging {

  val defaultConfig = FormstackConfig.getPerformSarHandlerConfig
  val formstackPerformSarHandler = FormstackPerformSarHandler(Dynamo, FormstackSarService, defaultConfig)

  val updateDynamoResult = formstackPerformSarHandler.updateDynamo(SubmissionTableUpdateDate("", ""))
  updateDynamoResult match {
    case Left(err) =>
      logger.error("Unable to complete update on formstack-submissions-ids table.", err)
    case Right(_) =>
      logger.info("Successfully completed updates on formstack-submissions-ids and formstack-submissions-last-updated tables.")
  }
}