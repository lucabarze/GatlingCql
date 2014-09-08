package com.github.gatling.cql

import io.gatling.core.action.builder.ActionBuilder
import com.datastax.driver.core.SimpleStatement
import com.datastax.driver.core.Statement

case class CqlRequestBuilderBase(tag: String) {
  def execute(statement: String) = new CqlRequestBuilder(CqlAttributes(tag, new SimpleStatement(statement)))
  def execute(statement: Statement) = new CqlRequestBuilder(CqlAttributes(tag, statement))
}  

case class CqlRequestBuilder(attr: CqlAttributes) {
    def build(): ActionBuilder = new CqlRequestActionBuilder(attr)
}