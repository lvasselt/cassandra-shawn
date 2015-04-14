# Introduction to NOSQL and cassandra, part 1 #


I recently gave a talk at outbrain, where I work, about an introduction to no-sql and Cassandra as we’re looking for alternatives of scaling out our database solution to match our incredible growth rate.

NOSQL is a general name for many non relational databases and Cassandra is one of them.

This was the first session of two in which I introduced the theoretical background and explained few of the important concepts of nosql. In the second session, due next week, I’ll talk more specifically about Cassandra.

The talk is on youtube, video below, but it’s in Hebrew so I’ll share it’s outline in English here. Slides are enclosed as well.

http://prettyprint.me/2010/01/09/introduction-to-nosql-and-cassandra-part-1/

  * SQL and relational DBs in general offer is a very good general purpose solution for many  applications such as blogs, banking, my cat’s site etc.
  * RDBMS provide ACID: Atomicity, Consistency, Isolation and Durability
  * RDBMS + SQL (the query language) + ACID provide a very nice and clean programming interface, suitable for banks, online merchants and many other applications, but not all applications really actually do require full ACID and one has to realize that ACID and SQL features are not without costs when systems need to scale out. Cost is not only in $$, it’s also in application performance and features.
  * The new generation of internet scale applications put very high demands on DB systems when it comes to scale and speed of operation but they don’t necessariry require all the good that’s in RDBMS, such as Full Consistency or Atomicity.
  * So, a new brand of DB systems has grown over the past 5 or so years – nosql, which either stands for No-SQL or Not-Only-SQL.
  * Leading actors in the nosql arena are Google with its BigTable, Amazon with Dynamo, Facebook with Cassandra and there’s more.
  * I presented intermediate solutions before going no-sql, namely RDBMS sharding which is very common and FriendFeed’s particularly interesting solution of application level indexing for using mysql with a schema-less data model.
  * CAP Theorem: At large scale systems you may only choose 2 out of the 3 desired attributes: Consistency, Availability and Partition-Tolerance. All three may not go hand in hand and application designers need to realize that.
  * A Consistent and Available system with no Partition-tolerance is a RDBMS system that comes to a halt if one of it’s hosts is down. That’s a very commonly used solution and perfect for small systems. This blog, for example, which uses WordPress, also uses a single mysql server which, if happens to be down, will also take the blog down. However, for internet scale systems where at almost any point in time there’s a good chance that one of the nodes is either down, or there are network disruptions, the No-Partition-Tolerance approach just isn’t going to cut it and they will have to choose a different approach for providing their SLAs.
  * Systems that are Available at all times and are capable of handling Partitions must sacrifice their consistency. As it turns out, though, this isn’t bad as it seems, as there are pretty good alternatives for lower levels of consistently, one such solution is Eventual Consistency, which actually works pretty nicely for “social applications” such as Google’s Facebook’s and Outbrain’s
  * I introduced the concept of NRW – N is the number of database replicas data is copied to one must replicate data in order to withstand partitions. W is the number of replicas a write operation would block on until it returns to it’s caller and is “successful” and R is the number of replicas a read operation would block on before returning to its caller.
  * N, R and W are crucial when dealing with Eventual Consistency as their values usually determine the level of consistency you’re going to have. For example, when N=R=W you have a full consistency (which isn’t tolerant to partitions or course). When W=0 you have async writes, which is the lowest level of consistency (you never know when the write operation actually finishes)
  * I introduced the concept of Quorum, which means R=W=ceil((N+1)/2)
  * Introduced a (very partial) list of currently available nosql solutions, such as Cassandra, BigTable, HBase, Dynamo, Voldemort, Riak, CouchDB, MongoDB and more.

Overall this was a very interesting talk, a lot of (fun and interesting) theory. The next part is going to be specific about Cassandra – how all this theory fits into Cassandra and how does one use Cassandra’s API, so stay tuned.

# Introduction to NOSQL and cassandra, part 2 #
In part 1 of this talk I presented few of the theoretical concepts behind nosql and cassandra.

In this talk we deep dive into the Cassandra API and implementation. The video is again in Hebrew, but the slides are multilingual ;-)
  * Started with a short recap of some of RDBMS and SQL properties, such as ADIC, why SQL if very programmer friendly, but is also limited in its support for large scale systems.
  * Short recap of the CAP theorem
  * Short recap of what N/R/W are
  * Cassandra Data Model: Cassandra is a column oriented DB which follows a similar data model to Google’s BigTable
  * Do you know SQL? So you better start forgetting it, Cassandra is a different game.
  * Vocabulary:
> > o Keyspace – a logical buffer for application data. For example – Billing keyspace, or statistics keyspace, appX keyspace etc
> > o ColumnFamily – similar to SQL tables. Aggregates columns and rows
> > o Keys (or Rows). Each set of columns is identified by a key. A key is unique per Column Family
> > o Columns – the actual values. Columns are represented by triplets – (name, value, timestamp)
> > o Super-Columns – Facebook’s addition to the BigTable model SuperColumns are columns who’s values is a list of Columns. (but this is not recursive, you can only have one level of super-columns)
  * One way to think of cassandra is as a key-value store, but with extra functionality:
> > o Each key has multiple values. In Cassandra jargon those are Columns
> > o When reading or writing data it’s possible to read/write a set of columns for one specific key (row) atomically. This set of columns may either be a specified by the list column names, or by a slice predicate, assuming the columns are sorted in some way (that’s a configuration parameter)
> > o In a addition, a multi-get operation is supported and a row-range-read operation is supported as well.
> > o Row-range-read operations are supported only of a partitioner is defined which supports that (configuration parameter)
  * Key concept: In SQL you add your data first and then retrieve it in ad-hoc manner using select queries and where clauses; In Cassandra you can’t do that. Data can only be retrieved by it’s row key, so you have to think about how you’re going to be reading your data before you insert it. This is a conceptual diff b/w SQL and Cassandra.
  * I covered the Cassandra API methods:
> > o get
> > o get\_slice
> > o multiget
> > o multiget\_slice
> > o get\_count
> > o get\_range\_slice
> > o insert
> > o batch\_insert
> > o delete
> > o (these are the 0.4 api method. In 0.5 it’s a little different)
  * Between N/R/W, N is set per keyspace; R is defined per each read operation (get/multiget/etc) and W is defined per write operation (insert/batch\_insert/delete)
  * Applications play with their R/W values to get different effects, for example they use QUORUM to get high consistency levels, or DC\_QUORUM for a balance of high consistency and performance, W=0 to have async writes with reduced consistency.
  * Cassandra defines different sorting orders on it’s columns. Sort order may be defined at the ColumnFamily level and is used to get a slice of columns, for example, read all columns that start with a… and end with z…
  * There are several out of the box sort types, such as ascii, utf, numeric and date; Applications may also add their own sorters; This is as far as I recall the only place where Cassandra allows external code to be hooked in.
  * Thrift is a protocol and a library for cross-process communication and is used by Cassandra. You define a thrift interface and then compile it to the language of your choosing – C++, Java, Python, PHP etc. This makes it very easy for cross-language processes to talk to each other.
  * Thrift is also very efficient serializing and  deserializing objects and is also space-efficient (much more than Java serialization is).
  * I did not have enough time to cover the Gossip protocolhttp://en.wikipedia.org/wiki/Gossip_protocol used by Cassandra internally to learn about the health of its hosts.
  * I also did not have enough time to cover the Repair-on-reads algorithm used by Cassandra to repair data inconsistencies lazily.
  * I did not have time to talk about consistent hashinghttp://en.wikipedia.org/wiki/Consistent_hashing, which is what cassandra implements internally to reduce overhead of joined or dropped hosts occurrences.

So, as you can see, this was an overloaded, 1h+ talk with a lot to grasp. Wish me luck implementing Cassandra into outbrain!



