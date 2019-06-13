package com.gu.identity.formstackconsents

sealed trait Newsletter {
  val formId: String
  val listType: String
  val consent: String
}