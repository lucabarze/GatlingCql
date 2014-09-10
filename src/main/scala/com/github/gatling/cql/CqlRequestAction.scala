/*
 * #%L
 * GatlingCql
 * %%
 * Copyright (C) 2014 Mikhail Stepura
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
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