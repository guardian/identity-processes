package com.gu.identity.formstackbatonrequests.aws

import org.scalatest.{FreeSpec, Matchers}

import java.time.LocalDateTime

class DateTests extends FreeSpec with Matchers {
    "SubmissionTableUpdateDate" - {
    "should convert to eastern by applying an offset of 5 hours for winter dates" in {
      val utcDate = LocalDateTime.of(2023,1,13,12,45,31)
      DateHelpers.toEasternTimeString(utcDate) shouldBe "2023-01-13 07:45:31"
    }
    "should convert to eastern by applying an offset of 4 hours for summer dates" in {
      val utcDate = LocalDateTime.of(2023,8,9,16,54,31)
      DateHelpers.toEasternTimeString(utcDate) shouldBe "2023-08-09 12:54:31"
    }
  }
  "SubmissionTableUpdateDate" - {
    "should convert to localDateTime" in {
      val utcDate = SubmissionTableUpdateDate("unused_metadata", "2023-01-13 12:45:31")
      utcDate.asLocalTimestamp shouldBe LocalDateTime.of(2023,1,13,12,45,31)
    }
  }
}
