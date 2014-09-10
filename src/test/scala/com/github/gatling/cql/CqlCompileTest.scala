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

  val scn = scenario("CQLS DSL test").repeat(1) {
    feed(feeder).
    exec(cql("simple statement").execute("SELECT * FROM schema_columnfamilies where keyspace_name ='${keyspace}'"))
    .exec(cql("prepared statement").execute(prepared).params("system"))
  }

  setUp(scn.inject(rampUsersPerSec(10) to 100 during (30 seconds)))
    .protocols(cqlConfig)

}