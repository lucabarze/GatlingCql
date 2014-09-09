package com.github.gatling.cql

import io.gatling.core.config.Credentials
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session

//just a wrapper around CqlProtocol

case object CqlProtocolBuilderBase {
  def session(session: Session) = CqlProtocolBuilder(session)
}

case class CqlProtocolBuilder(session: Session) {
  def build = new CqlProtocol(session)
}

