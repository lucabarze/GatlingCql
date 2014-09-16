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
package io.github.gatling.cql

import org.easymock.Capture
import org.easymock.EasyMock.capture
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mock.EasyMockSugar
import com.datastax.driver.core.RegularStatement
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Session
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{Session => GSession}
import io.gatling.core.session.el.ELCompiler
import io.gatling.core.validation.FailureWrapper
import com.datastax.driver.core.SimpleStatement

class CqlRequestActionSpec extends FlatSpec with EasyMockSugar with Matchers {
  implicit lazy val system = ActorSystem()
  GatlingConfiguration.setUp()
  val el = ELCompiler.compile[String]("select * from test where  id = ${test}")
  val cassandraSession = mock[Session]

  val target = TestActorRef(
    new CqlRequestAction(ActorRef.noSender,
      CqlProtocol(cassandraSession),
      CqlAttributes("test", SimpleCqlStatement(el)))).underlyingActor

  "CqlRequestAction" should "fail if expression is invalid" in {
    val s = GSession("scenario", "1", Map("bar" -> "BAR"))

    val result = target.executeOrFail(s)
    result should be("No attribute named 'test' is defined".failure)
  }

  it should "execute a valid statement" in {
    val s = GSession("scenario", "1", Map("test" -> "BAR"))

    val stmt = new Capture[RegularStatement]
    expecting {
      cassandraSession.executeAsync(capture(stmt)) andReturn (mock[ResultSetFuture])
    }
    whenExecuting(cassandraSession) {
      target.executeOrFail(s)
      stmt.getValue() shouldBe a [SimpleStatement]
      stmt.getValue().getQueryString() should be ("select * from test where  id = BAR")
    }

  }
}