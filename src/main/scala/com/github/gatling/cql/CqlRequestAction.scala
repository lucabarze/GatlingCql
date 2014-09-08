package com.github.gatling.cql

import akka.actor.ActorRef
import io.gatling.core.action.Chainable
import io.gatling.core.action.Failable
import io.gatling.core.session.Session
import io.gatling.core.validation.Failure
import io.gatling.core.validation.Validation
import io.gatling.core.action.Interruptable
import com.datastax.driver.core.Cluster

class CqlRequestAction(val next: ActorRef, protocol: CqlProtocol, attr: CqlAttributes) extends Interruptable with Failable {
  
  def executeOrFail(session: Session):Validation[Unit] = {
   //create a session here
   // execute a statement
   Failure("test") 
  }
}