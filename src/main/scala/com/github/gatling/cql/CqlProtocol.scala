package com.github.gatling.cql

import io.gatling.core.config.{ Credentials, Protocol }
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session

//holds reference to a cluster, just settings
case class CqlProtocol(session: Session) extends Protocol