package com.github.gatling.cql

import io.gatling.core.config.{ Credentials, Protocol }
import com.datastax.driver.core.Cluster

//holds reference to a cluster, just settings
case class CqlProtocol(cluster: Cluster) extends Protocol