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

import io.github.gatling.cql.checks.CqlCheck
import io.github.gatling.cql.request.{CqlAttributes, CqlProtocol, CqlRequestAction}
import org.easymock.Capture
import org.easymock.EasyMock.capture
import org.easymock.EasyMock.reset
import org.scalatest.BeforeAndAfter
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mock.EasyMockSugar
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.RegularStatement
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Session
import com.datastax.driver.core.SimpleStatement
import io.gatling.commons.validation.FailureWrapper
import io.gatling.commons.validation.SuccessWrapper
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{Session => GSession}

class CqlRequestActionSpec extends FlatSpec with EasyMockSugar with Matchers with BeforeAndAfter {
  val config = GatlingConfiguration.loadForTest()
  val cassandraSession = mock[Session]
  val statement = mock[CqlStatement]
  val session = GSession("scenario", 1)
  val coreComponents = CoreComponents(
    controller = null,
    throttler = null,
    statsEngine = null,
    exit = null,
    configuration = config)

  val target =
    new CqlRequestAction("some-name", coreComponents.exit,
      coreComponents.statsEngine,
      CqlProtocol(cassandraSession),
      CqlAttributes("test", statement, ConsistencyLevel.ANY, ConsistencyLevel.SERIAL, List.empty[CqlCheck]))

  before {
    reset(statement, cassandraSession)
  }

  "CqlRequestAction" should "fail if expression is invalid and return the error" in {
    expecting {
      statement.apply(session).andReturn("OOPS".failure)
    }
//    whenExecuting(statement) {
//      target.execute(session) should be("OOPS".failure)
//    }
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
