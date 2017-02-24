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
package io.github.gatling.cql

import akka.actor.ActorRef
import com.datastax.driver.core._
import io.gatling.commons.stats.KO
import io.gatling.commons.validation.{FailureWrapper, SuccessWrapper}
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.session.{Session => GSession}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.github.gatling.cql.checks.CqlCheck
import io.github.gatling.cql.request.{CqlAttributes, CqlProtocol, CqlRequestAction}
import org.easymock.Capture
import org.easymock.EasyMock.{anyObject, anyString, capture, reset, eq => eqAs}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.scalatest.easymock.EasyMockSugar

class CqlRequestActionSpec extends FlatSpec with EasyMockSugar with Matchers with BeforeAndAfter {
  val config = GatlingConfiguration.loadForTest()
  val cassandraSession = mock[Session]
  val statement = mock[CqlStatement]
  val coreComponents = CoreComponents(mock[ActorRef], mock[Throttler], mock[StatsEngine], mock[Action], mock[GatlingConfiguration])
  val nextAction = mock[Action]
  val throttled = false
  val session = GSession("scenario", 1)
  val protocol = CqlProtocol(cassandraSession)

  val target =
    new CqlRequestAction(
      nextAction,
      coreComponents,
      CqlProtocol(cassandraSession),
      throttled,
      CqlAttributes("test", statement, ConsistencyLevel.ANY, ConsistencyLevel.SERIAL, List.empty[CqlCheck])
    )

  before {
    reset(statement, cassandraSession)
  }


  it should "fail if expression is invalid and return the error" in {
    val errorMessageCapture = new Capture[Some[String]]
    expecting {
      statement.apply(session).andReturn("OOPS".failure)
      coreComponents.statsEngine.logResponse(eqAs(session), anyString, anyObject[ResponseTimings], eqAs(KO), eqAs(None), capture(errorMessageCapture), eqAs(Nil))
    }

    whenExecuting(statement, coreComponents.statsEngine) {
      target.execute(session)
    }
    val captureErrorMessage = errorMessageCapture.getValue
    captureErrorMessage.get should be("Error setting up prepared statement: OOPS")
  }

  it should "execute a valid statement" in {
    val statementCapture = new Capture[RegularStatement]
    expecting {
      statement.apply(session).andReturn(new SimpleStatement("select * from test").success)
      cassandraSession.executeAsync(capture(statementCapture)) andReturn mock[ResultSetFuture]
    }
    whenExecuting(statement, cassandraSession) {
      target.execute(session)
    }
    val capturedStatement = statementCapture.getValue
    capturedStatement shouldBe a[SimpleStatement]
    capturedStatement.getConsistencyLevel shouldBe ConsistencyLevel.ANY
    capturedStatement.getQueryString should be("select * from test")
  }
}
