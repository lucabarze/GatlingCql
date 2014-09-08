package com.github.gatling.cql

import com.datastax.driver.core.Statement

case class CqlAttributes(tag: String, statement: Statement)