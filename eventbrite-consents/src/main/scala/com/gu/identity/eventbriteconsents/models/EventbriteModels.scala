package com.gu.identity.eventbriteconsents.models

case class EventbriteAnswer(question: Option[String], answer: Option[String]) {
  val isEventsAndMasterClassesAnswer: Boolean = {
    question.exists(_.toLowerCase.matches(EventbriteAnswer.questionRegex)) &&
      answer.exists(_.toLowerCase.matches(EventbriteAnswer.answerRegex))
  }
}

object EventbriteAnswer {
  val questionRegex = "^please tick if you are interested in hearing about these products and services from guardian news [^ ]+ media:.*"
  val answerRegex = "^events [^ ]+ masterclasses.*"
}

case class EventbriteProfile(email: Option[String])

case class EventbriteAttendee(answers: Option[Vector[EventbriteAnswer]], profile: Option[EventbriteProfile]) {
  val answersList: Vector[EventbriteAnswer] = answers.toVector.flatten
}

case class EventbritePagination(
  has_more_items: Boolean = false,
  continuation: Option[String] = None,
  page_count: Int,
  page_number: Int
) {
  def continuationToken: Option[String] = {
    if (this.page_count > this.page_number && this.continuation.isEmpty) {
      throw new RuntimeException("Eventbrite pagination shows there are more pages to process but continuation token is absent")
    } else {
      this match {
        case EventbritePagination(true, Some(cont), _, _) => Some(cont)
        case _ => None
      }
    }
  }
}

case class EventbriteResponse(pagination: EventbritePagination, attendees: Option[Vector[EventbriteAttendee]]) {
  val attendeesList: Vector[EventbriteAttendee] = attendees.toVector.flatten
}

