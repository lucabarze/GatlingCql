package com.github.gatling.cql

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import io.gatling.core.action.Failable
import io.gatling.core.action.Interruptable
import io.gatling.core.result.message.OK
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation._

class CqlRequestAction(val next: ActorRef, protocol: CqlProtocol, attr: CqlAttributes) extends Interruptable with Failable with DataWriterClient {

  def executeOrFail(session: Session): Validation[Unit] = {
    val start = nowMillis
    val stmt = attr.statement(session)
    protocol.session.execute(stmt)
    writeRequestData(session, attr.tag, start, nowMillis, start, nowMillis, OK, None, Nil)
    logger.trace("{}", stmt)
    next ! session
    Success()
  }

}