/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 GatlingCql developers
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
package io.github.gatling.cql.request

import com.datastax.driver.core.exceptions.DriverException
import com.datastax.driver.core.ResultSet
import com.google.common.util.concurrent.{FutureCallback, Futures}
import io.gatling.commons.util.ClockSingleton._
import io.gatling.commons.stats.KO
import io.gatling.commons.stats._
import io.gatling.core.CoreComponents
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.util.NameGen
import io.github.gatling.cql.response.CqlResponse

class CqlRequestAction( val next: Action,
                        val coreComponents: CoreComponents,
                        val protocol: CqlProtocol,
                        val throttled: Boolean,
                        val cqlAttributes: CqlAttributes )
  extends ExitableAction with NameGen{

  val statsEngine = coreComponents.statsEngine
  override val name = genName(cqlAttributes.tag)

  def execute(session: Session): Unit = {

    val parsedStatement = cqlAttributes statement session

    parsedStatement onFailure ( err => {
      val msg = s"Error setting up prepared statement: $err"
      statsEngine logResponse(session, name, ResponseTimings(nowMillis, nowMillis), KO, None, Some(msg), Nil)
      throttling(throttled, session, next, session.markAsFailed)
    } )

    parsedStatement onSuccess { statement =>

      statement.setConsistencyLevel(cqlAttributes.cl)
      statement.setSerialConsistencyLevel(cqlAttributes.serialCl)

      val start = nowMillis

      Futures.addCallback(
        protocol.session.executeAsync(statement),
        new FutureCallback[ResultSet] {

          override def onSuccess(resultSet: ResultSet): Unit = {
            val response = new CqlResponse(resultSet)
            val respTimings = ResponseTimings(start, nowMillis)

            val (checks, errors) = Check.check(response, session, cqlAttributes.checks)

            errors match {
              case None =>
                statsEngine logResponse(session, name, respTimings, OK, None, None, Nil)
                throttling(throttled, session, next, checks(session).markAsSucceeded)
              case _ =>
                val msg = s"Error verifying results: " + errors
                statsEngine logResponse(session, name, respTimings, KO, None, Some(msg), Nil)
                throttling(throttled, session, next, checks(session).markAsFailed)
            }

          }

          override def onFailure(t: Throwable): Unit = {
            val respTimings = ResponseTimings(start, nowMillis)

            t match {
              case _: DriverException =>
                val msg = name + ": c.d.d.c.e." + t.getClass.getSimpleName + ": " + t.getMessage
                statsEngine logResponse(session, name, respTimings, KO, None, Some(msg), Nil)

              case _ =>
                logger.error(s"$name: Error executing statement $statement", t)
                statsEngine logResponse(session, name, respTimings, KO, None, Some(name + ": " + t), Nil)
            }

            throttling(throttled, session, next, session.markAsFailed)

          }
        }
      )

    }


  }


  def throttling(throttled: Boolean, session: Session, next: Action, marked: Session) = {
    if (throttled) {
      coreComponents.throttler.throttle(session.scenario, () => next ! marked)
    } else {
      next ! marked
    }
  }

}
