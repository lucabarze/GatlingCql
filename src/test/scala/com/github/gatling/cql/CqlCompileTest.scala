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

import scala.concurrent.duration.DurationInt

import com.datastax.driver.core.Cluster
import com.github.gatling.cql.Predef._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation

class CqlCompileTest extends Simulation {
  val session = Cluster.builder().addContactPoint("127.0.0.1").build().connect("system")
  val cqlConfig = cql.session(session)
  
  val prepared = session.prepare("select * from schema_columnfamilies where keyspace_name = ?")
  
  val random = new util.Random
  val feeder = Iterator.continually(Map("keyspace" -> (random.nextString(20))))

  val scn = scenario("Two selects").repeat(1) {
    feed(feeder).
    exec(cql("simple statement").execute("SELECT * FROM schema_columnfamilies where keyspace_name = '${keyspace}'"))
    .exec(cql("prepared statement").execute(prepared).params("${keyspace2}"))
  }

  setUp(scn.inject(rampUsersPerSec(10) to 100 during (30 seconds)))
    .protocols(cqlConfig)

}