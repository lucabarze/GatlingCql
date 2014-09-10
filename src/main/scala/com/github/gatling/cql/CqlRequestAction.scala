package com.github.gatling.cql

import com.datastax.driver.core.ResultSet
import com.typesafe.scalalogging.slf4j.StrictLogging

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import io.gatling.core.action.Failable
import io.gatling.core.action.Interruptable
import io.gatling.core.result.message.KO
import io.gatling.core.result.message.OK
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.FailureWrapper
import io.gatling.core.validation.SuccessWrapper
import io.gatling.core.validation.Validation

class CqlRequestAction(val next: ActorRef, protocol: CqlProtocol, attr: CqlAttributes) 
  extends Interruptable 
  with Failable 
  with DataWriterClient 
  with StrictLogging {

  def executeOrFail(session: Session): Validation[ResultSet] = {
    val start = nowMillis
    val stmt = attr.statement(session)
    try {
      val result = protocol.session.execute(stmt)
      writeRequestData(session, attr.tag, start, nowMillis, session.startDate, nowMillis, OK, None, Nil)
      logger.trace(s"$stmt")
      next ! session.markAsSucceeded
      result.success
    } catch {
      case e:Exception => {
        logger.error(s"Error in: $stmt")
        writeRequestData(session, attr.tag, start, nowMillis, session.startDate, nowMillis, KO, Some(e.getMessage()), List(stmt.toString()))
        next ! session.markAsFailed
        s"Error in: $stmt".failure
      }
    }
  }

}