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
import io.gatling.core.result.message.KO

class CqlRequestAction(val next: ActorRef, protocol: CqlProtocol, attr: CqlAttributes) extends Interruptable with Failable with DataWriterClient {

  def executeOrFail(session: Session): Validation[Unit] = {
    val start = nowMillis
    val stmt = attr.statement(session)
    try {
      protocol.session.execute(stmt)
      writeRequestData(session, attr.tag, start, nowMillis, start, nowMillis, OK, None, Nil)
      logger.trace("{}", stmt)
      next ! session
      Success()
    } catch {
      case e:Exception => {
        logger.error("Error in: {}", stmt)
        writeRequestData(session, attr.tag, start, nowMillis, start, nowMillis, KO, Some(e.getMessage()), List(stmt.toString()))
        next ! session.markAsFailed
        Failure(e.getMessage())
      }
    }
  }

}