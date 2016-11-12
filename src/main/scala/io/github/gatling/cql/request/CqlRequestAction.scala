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

import akka.actor.ActorSystem
import com.datastax.driver.core.Statement
import com.google.common.util.concurrent.{Futures, MoreExecutors}
import io.gatling.commons.stats.KO
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.validation.Validation
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.github.gatling.cql.response.CqlResponseHandler

class CqlRequestAction(val name: String, val next: Action, system: ActorSystem, val statsEngine: StatsEngine, protocol: CqlProtocol, attr: CqlAttributes)
  extends ExitableAction {

  def execute(session: Session): Unit = {
    val stmt: Validation[Statement] = attr.statement(session)

    stmt.onFailure(err => {
      statsEngine.logResponse(session, name, ResponseTimings(nowMillis, nowMillis), KO, None, Some("Error setting up prepared statement: " + err), Nil)
      next ! session.markAsFailed
    })

    stmt.onSuccess({ stmt =>
      stmt.setConsistencyLevel(attr.cl)
      stmt.setSerialConsistencyLevel(attr.serialCl)

      val start = nowMillis
      val result = protocol.session.executeAsync(stmt)
      Futures.addCallback(result, new CqlResponseHandler(next, session, system, statsEngine, start, attr.tag, stmt, attr.checks), MoreExecutors.sameThreadExecutor)
    })
  }
}
