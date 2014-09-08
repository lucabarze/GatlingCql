package com.github.gatling.cql

import akka.actor.ActorRef
import io.gatling.core.action.Chainable
import io.gatling.core.action.Failable
import io.gatling.core.session.Session
import io.gatling.core.validation.Failure
import io.gatling.core.validation.Validation
import io.gatling.core.action.Interruptable

class CqlRequestAction(val next: ActorRef) extends Interruptable with Failable {
  def executeOrFail(session: Session):Validation[Unit] = {
   Failure("test") 
  }
}