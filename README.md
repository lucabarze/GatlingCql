GatlingCql
==========

Gatling DSL support for Apache Cassandra CQL

Features
--------

Basic Gatling DSL for Apache Cassandra CQL, prepared statements are supported as well

```scala
class CassandraSimulation extends Simulation{
  val keyspace = "test"
  val table_name = "test_table"
  val session = Cluster.builder().addContactPoint("127.0.0.1").build().connect(s"$keyspace") //Your C* session
  val cqlConfig = cql.session(session) //Initialize Gatling DSL with your session
  
  //Setup
  session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor': '1'}")
  session.execute(s"""CREATE TABLE IF NOT EXISTS $table_name (
		  id timeuuid,
		  num int,
		  str text,
		  PRIMARY KEY (id)
		);
      """)
  session.execute(f"CREATE INDEX IF NOT EXISTS $table_name%s_num_idx ON $table_name%s (num)")
  
  
  //Prepare your statement, we want to be effective, right?
  val prepared = session.prepare(s"INSERT INTO $table_name (id, num, str) values (now(), ?, ?)")
  
  val random = new util.Random
  val feeder = Iterator.continually( // this feader will "feed" random data into our Sessions
      Map(
          "randomString" -> random.nextString(20), 
          "randomNum" -> random.nextInt()
          ))

  val scn = scenario("Two statements").repeat(1) { //Name your scenario
    feed(feeder)
    .exec(cql("simple SELECT") // 'execute' can accept a string and understand Gatling expression language
        .execute("SELECT * FROM test_table WHERE num = ${randomNum}"))  //Gatling EL for ${randomNum}"
    .exec(cql("prepared INSERT")
        .execute(prepared) // alternatively 'execute' accepts a prepared statement
        .withParams(Integer.valueOf(random.nextInt()), "${randomString}") // you need to provide parameters for that (gatling EL is supported as well)
        .consistencyLevel(ConsistencyLevel.ANY)) // and set a ConsistencyLevel optionally
  }

  setUp(scn.inject(rampUsersPerSec(10) to 100 during (30 seconds)))
    .protocols(cqlConfig)
```


Installation
------------

* Get a release TGZ
* Unpack into Gatling folder: ``tar -xjf GatlingCql-0.0.2-SNAPSHOT-release.tar.gz -C gatling-charts-highcharts-2.0.0-RC5/``
* Run Gatling and you should see ``cassandra.CassandraSimulation`` in your simulations list
