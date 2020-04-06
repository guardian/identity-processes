package com.gu.identity.formstackbatonrequests.aws

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.Regions

object AwsCredentials {
  def credentials = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    new ProfileCredentialsProvider("identity"),
    new InstanceProfileCredentialsProvider(false)
  )

  val region: Regions = Regions.EU_WEST_1
}
