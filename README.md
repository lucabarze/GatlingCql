GatlingCql
==========

Gatling support for Apache Cassandra CQL

Features
--------

Basic Gatling DSL for Apache Cassandra CQL, prepared statement are supported as well

```scala
class CassandraSimulation extends Simulation{
  val keyspace = "test"
  val table_name = "test_table"
  val session = Cluster.builder().addContactPoint("127.0.0.1").build().connect(s"$keyspace")
  val cqlConfig = cql.session(session)
  
  session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor': '1'}")
  session.execute(s"""CREATE TABLE IF NOT EXISTS $table_name (
		  id timeuuid,
		  num int,
		  str text,
		  PRIMARY KEY (id)
		);
      """)
  session.execute(f"CREATE INDEX IF NOT EXISTS $table_name%s_num_idx ON $table_name%s (num)")
  
  val prepared = session.prepare(s"INSERT INTO $table_name (id, num, str) values (now(), ?, ?)")
  
  val random = new util.Random
  val feeder = Iterator.continually(
      Map(
          "randomString" -> random.nextString(20), 
          "randomNum" -> random.nextInt()
          ))

  val scn = scenario("Two statements").repeat(1) {
    feed(feeder)
    .exec(cql("simple SELECT")
        .execute("SELECT * FROM test_table WHERE num = ${randomNum}"))  //Gatling EL for ${randomNum}"
    .exec(cql("prepared INSERT")
        .execute(prepared)
        .withParams(Integer.valueOf(random.nextInt()), "${randomString}")
        .consistencyLevel(ConsistencyLevel.ANY))
  }

  setUp(scn.inject(rampUsersPerSec(10) to 100 during (30 seconds)))
    .protocols(cqlConfig)
```


Installation
------------

* Get a release TGZ
* Unpack into Gatling folder: ``tar -xjf GatlingCql-0.0.2-SNAPSHOT-release.tar.gz -C gatling-charts-highcharts-2.0.0-RC5/``
* Run Gatling and you should see ``cassandra.CassandraSimulation`` in your simulations list
