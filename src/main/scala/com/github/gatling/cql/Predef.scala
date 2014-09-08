package com.github.gatling.cql

object Predef {
  val cql = CqlProtocolBuilderBase
  
  def cql(tag: String) = CqlRequestBuilder(tag)
  
  implicit def cqlProtocolBuilder2cqlProtocol(builder: CqlProtocolBuilder): CqlProtocol = builder.build
}