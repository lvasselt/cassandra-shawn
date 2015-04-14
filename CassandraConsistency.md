http://cassandra-shawn.googlecode.com/files/CassandraConsistency.JPG

Cassandra使用的几个保证一致性的feature:

  * Hinted Handoff
  * Read Repair (vector clock, version control)
http://cassandra-shawn.googlecode.com/files/Read_Repair.JPG
  * Anti Entropy (DHT, Merkle Tree)