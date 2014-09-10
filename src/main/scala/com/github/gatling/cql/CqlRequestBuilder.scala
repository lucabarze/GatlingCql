package com.github.gatling.cql

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.SimpleStatement

import io.gatling.core.action.builder.ActionBuilder

case class CqlRequestBuilderBase(tag: String) {
  def execute(statement: String) = new CqlRequestBuilder(CqlAttributes(tag, new SimpleStatement(statement)))
  def execute(prepared: PreparedStatement) = new CqlRequestParamsBuilder(tag, prepared)
}  

case class CqlRequestParamsBuilder(tag: String, prepared: PreparedStatement) {
  def params(params: AnyRef*) = new CqlRequestBuilder(CqlAttributes(tag, prepared.bind(params:_*)))
}

case class CqlRequestBuilder(attr: CqlAttributes) {
    def build(): ActionBuilder = new CqlRequestActionBuilder(attr)
}