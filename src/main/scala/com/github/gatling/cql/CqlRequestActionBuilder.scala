package com.github.gatling.cql

import akka.actor.ActorRef
import akka.actor.Props
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.Protocols

class CqlRequestActionBuilder(attr: CqlAttributes) extends ActionBuilder {
  def build(next: ActorRef, registry: Protocols) = {
    val cqlProtocol = registry.getProtocol[CqlProtocol].getOrElse(throw new UnsupportedOperationException("CQL protocol wasn't registered"))
    system.actorOf(Props(new CqlRequestAction(next, cqlProtocol, attr)))
  }
}