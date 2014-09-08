package com.github.gatling.cql

import io.gatling.core.config.Credentials
import com.datastax.driver.core.Cluster

//just a wrapper around CqlProtocol

case object CqlProtocolBuilderBase {
  def cluster(cluster: Cluster) = CqlProtocolBuilder(cluster)
}

case class CqlProtocolBuilder(cluster: Cluster) {
  def build = new CqlProtocol(cluster)
}

