package com.github.gatling.cql

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import com.github.gatling.cql.Predef._
import com.datastax.driver.core.Cluster
import scala.Option.option2Iterable
import scala.concurrent.duration.DurationInt

class CqlCompileTest extends Simulation {
  val cluster = Cluster.builder().addContactPoint("127.0.0.1").build().connect("system")
  val cqlConfig = cql.cluster(cluster)

  val scn = scenario("CQLS DSL test").repeat(1) {
    exec(cql("test select").execute("SELECT * FROM schema_columnfamilies"))
  }

  setUp(scn.inject(rampUsersPerSec(10) to 100 during (30 seconds)))
    .protocols(cqlConfig)

}