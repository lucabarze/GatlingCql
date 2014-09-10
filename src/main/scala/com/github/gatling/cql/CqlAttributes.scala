package com.github.gatling.cql

import com.datastax.driver.core.Statement
import io.gatling.core.session.Expression
import io.gatling.core.session.Session
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.SimpleStatement
import io.gatling.core.validation._

case class CqlAttributes(tag: String, statement: CqlStatement)

trait CqlStatement {
  def apply(session:Session):Statement
}
case class SimpleCqlStatement(statement: Expression[String]) extends CqlStatement {
  def apply(session: Session) = statement(session) match {
    case Success(stmt) => new SimpleStatement(stmt)
    case Failure(error) => throw new IllegalArgumentException(error)
  }
}
case class BoundCqlStatement(statement: BoundStatement) extends CqlStatement {
  def apply(session:Session) = statement
}
