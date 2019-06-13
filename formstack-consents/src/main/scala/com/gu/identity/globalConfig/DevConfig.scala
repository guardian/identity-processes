package com.gu.identity.globalConfig

import com.typesafe.config.ConfigFactory

class DevConfig {

  // this config will go in parameter store

  private val conf = ConfigFactory.load()

  object Formstack {
    val token: String = conf.getString("formstack-token")
    val host: String = conf.getString("formstack-host")
    val password: String = conf.getString("form-password")
  }

  object Identity {
    val accessToken: String = conf.getString("identity-access-token-dev")
    val prodAccessToken: String = conf.getString("identity-access-token-prod")
    val host: String = conf.getString("idapi-host-dev")
    val prodHost: String = conf.getString("idapi-host-prod")
  }
}


