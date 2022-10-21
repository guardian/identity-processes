package com.gu.identity.formstackbatonrequests.services

object Util {
  def extractEmails(txt: String): List[String] = {
    val emailReg = """(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}\b""".r
    emailReg.findAllIn(txt).toList
  }
}

