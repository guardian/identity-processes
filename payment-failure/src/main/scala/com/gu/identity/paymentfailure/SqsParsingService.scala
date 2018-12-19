package com.gu.identity.paymentfailure

import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.generic.semiauto.deriveDecoder
import com.gu.identity.paymentfailure.Model.IdentityBrazeEmailData


class SqsParsingService extends StrictLogging {

  def parseSingleMessage(sqsMessage: SQSMessage): Either[Throwable, IdentityBrazeEmailData] = {
    implicit val identityBrazeEmailDataDecoder: Decoder[IdentityBrazeEmailData] = deriveDecoder[IdentityBrazeEmailData]
    decode[IdentityBrazeEmailData](sqsMessage.getBody)
  }
}
