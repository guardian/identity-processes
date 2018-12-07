package com.gu.identity.paymentfailure

import com.typesafe.scalalogging.StrictLogging

object Lambda extends StrictLogging {

  def handler(): Unit = {
    logger.info("executing lambda")
  }
}
