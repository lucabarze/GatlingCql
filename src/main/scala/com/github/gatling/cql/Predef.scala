package com.github.gatling.cql

import io.gatling.core.action.builder.ActionBuilder

object Predef {
  val cql = CqlProtocolBuilderBase
  
  def cql(tag: String) = CqlRequestBuilderBase(tag)
  
  implicit def cqlProtocolBuilder2cqlProtocol(builder: CqlProtocolBuilder): CqlProtocol = builder.build
  implicit def cqlRequestBuilder2ActionBuilder(builder: CqlRequestBuilder): ActionBuilder = builder.build()
}