package com.github.gatling.cql

import io.gatling.core.action.builder.ActionBuilder

case class CqlRequestBuilder(tag: String) {
  def execute(statement: String) = None
  
  def build(): ActionBuilder = new CqlRequestActionBuilder()
}