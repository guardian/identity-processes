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

case class EventbritePagination(has_more_items: Option[Boolean], continuation: Option[String]) {
  val continuationToken: Option[String] = this match {
    case EventbritePagination(Some(true), Some(cont)) => Some(cont)
    case _ => None
  }
}

case class EventbriteResponse(pagination: EventbritePagination, attendees: Option[Vector[EventbriteAttendee]]) {
  val attendeesList: Vector[EventbriteAttendee] = attendees.toVector.flatten
}

