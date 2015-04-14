
```
shawny@ubuntu:~/software/apache-cassandra-0.6.5$ bin/cassandra -f
 INFO 08:09:40,826 JNA not found. Native methods will be disabled.
 INFO 08:09:42,430 DiskAccessMode 'auto' determined to be standard, indexAccessMode is standard
 INFO 08:09:43,417 Saved Token not found. Using 143395886662297243557596592964572292126
 INFO 08:09:43,417 Saved ClusterName not found. Using Test Cluster
 INFO 08:09:43,433 Creating new commitlog segment /var/lib/cassandra/commitlog/CommitLog-1287068983433.log
 INFO 08:09:43,647 LocationInfo has reached its threshold; switching in a fresh Memtable at CommitLogContext(file='/var/lib/cassandra/commitlog/CommitLog-1287068983433.log', position=419)
 INFO 08:09:43,683 Enqueuing flush of Memtable-LocationInfo@17689439(169 bytes, 4 operations)
 INFO 08:09:43,685 Writing Memtable-LocationInfo@17689439(169 bytes, 4 operations)
 INFO 08:09:50,650 Completed flushing /var/lib/cassandra/data/system/LocationInfo-1-Data.db
 INFO 08:09:50,696 Starting up server gossip
 INFO 08:09:50,885 Binding thrift service to localhost/127.0.0.1:9160
 INFO 08:09:50,919 Cassandra starting up...
```

  * Show the directory structure of "/var/log/cassandra" and "/var/lib/cassandra".
```
shawny@ubuntu:/var/log$ tree cassandra/
cassandra/
`-- system.log

0 directories, 1 file
```
```
shawny@ubuntu:/var/lib$ tree cassandra/
cassandra/
|-- commitlog
|   `-- CommitLog-1287068983433.log
`-- data
    |-- Keyspace1
    `-- system
        |-- LocationInfo-1-Data.db
        |-- LocationInfo-1-Filter.db
        `-- LocationInfo-1-Index.db

4 directories, 4 files
```

  * Read and write some data using the command line.
```
shawny@ubuntu:~/software/apache-cassandra-0.6.5$ bin/cassandra-cli --host localhost --port 9160
Connected to: "Test Cluster" on localhost/9160
Welcome to cassandra CLI.

Type 'help' or '?' for help. Type 'quit' or 'exit' to quit.
cassandra> set Keyspace1.Standard2['shawn']['firstname'] = 'songhe'
Value inserted.
cassandra> set Keyspace1.Standard2['shawn']['lastname'] = 'yang'
Value inserted.
cassandra> set Keyspace1.Standard2['shawn']['age'] = '26'
Value inserted.
cassandra> get Keyspace1.Standard2['shawn']
=> (column=lastname, value=yang, timestamp=1287069766906000)
=> (column=firstname, value=songhe, timestamp=1287069740087000)
=> (column=age, value=26, timestamp=1287069847418000)
Returned 3 results.
```

  * Check the Cassandra data directory again.
```
shawny@ubuntu:/var/lib$ tree cassandra/
cassandra/
|-- commitlog
|   `-- CommitLog-1287070586474.log
`-- data
    |-- Keyspace1
    |   |-- Standard2-1-Data.db
    |   |-- Standard2-1-Filter.db
    |   `-- Standard2-1-Index.db
    `-- system
        |-- LocationInfo-1-Data.db
        |-- LocationInfo-1-Filter.db
        |-- LocationInfo-1-Index.db
        |-- LocationInfo-2-Data.db
        |-- LocationInfo-2-Filter.db
        |-- LocationInfo-2-Index.db
        |-- LocationInfo-3-Data.db
        |-- LocationInfo-3-Filter.db
        `-- LocationInfo-3-Index.db

4 directories, 13 files
```