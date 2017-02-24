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

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.SimpleStatement
import com.datastax.driver.core.Statement

import io.gatling.core.session._
import io.gatling.commons.validation._

trait CqlStatement {
  def apply(session:Session): Validation[Statement]
}


case class SimpleCqlStatement(statement: Expression[String]) extends CqlStatement {
  def apply(session: Session): Validation[Statement] = statement(session).flatMap(stmt => new SimpleStatement(stmt).success)
}


case class BoundCqlStatement(statement: PreparedStatement, params: Expression[AnyRef]*) extends CqlStatement {

  def apply(session: Session): Validation[Statement] = {

    val parsedParams = params map (_ (session))
    val firstFailure = parsedParams collectFirst { case Failure(f) => f }

    if (firstFailure isEmpty) {
      try {
        statement bind (parsedParams map (_.get): _*) success
      } catch {
        case e: Exception => e.getMessage.failure
      }
    } else {
      firstFailure.get.failure
    }

  }
}
