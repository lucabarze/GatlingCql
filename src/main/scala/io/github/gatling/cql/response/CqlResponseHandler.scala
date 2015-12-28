/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Mikhail Stepura
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.gatling.cql.response

import akka.actor._
import com.datastax.driver.core.{ResultSet, Statement}
import com.google.common.util.concurrent.FutureCallback
import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.check.Check
import io.gatling.core.result.message._
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis

import io.github.gatling.cql.checks.CqlCheck


class CqlResponseHandler(next: ActorRef, session: Session, start: Long, tag: String, stmt: Statement, checks: List[CqlCheck])
  extends FutureCallback[ResultSet]
  with DataWriterClient
  with StrictLogging {

  private def writeData(status: Status, message: Option[String]) = writeRequestData(session, tag, start, nowMillis, session.startDate, nowMillis, status, message, Nil)

  def onSuccess(resultSet: ResultSet) = {
    val response = new CqlResponse(resultSet)

    val checkRes = Check.check(response, session, checks)
    if (checkRes._2.isEmpty) {
      writeData(OK, None)
      next ! checkRes._1(session).markAsSucceeded
    }
    else {
      val errors = checkRes._2.get
      writeData(KO, Some(s"Error verifying results: $errors"))
      next ! checkRes._1(session).markAsFailed
    }
  }

  def onFailure(t: Throwable) = {
    logger.error(s"$stmt", t)
    writeData(KO, Some(s"Error executing statement: $t"))
    next ! session.markAsFailed
  }
}
