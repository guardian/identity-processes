package com.gu.identity.formstackbatonrequests.aws

import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient
import com.typesafe.scalalogging.LazyLogging

object ParameterStoreClient extends LazyLogging {
  val parameterStoreClient = AWSSimpleSystemsManagementClient
    .builder()
    .withRegion(AwsCredentials.region)
    .withCredentials(AwsCredentials.credentials)
    .build()

  def readSecureString(name: String): Option[String] = {
    try {
      val paramReq = new GetParameterRequest()
        .withName(name)
        .withWithDecryption(true)
      val paramValue =
        parameterStoreClient.getParameter(paramReq).getParameter.getValue
      Some(paramValue)
    } catch {
      case e: Exception =>
        logger.warn("Unable to retrieve parameter", e)
        None
    }
  }
}