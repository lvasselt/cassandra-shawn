## Architecture Overview ##
http://wiki.apache.org/cassandra/ArchitectureOverview

## Architecture Internals ##
### General ###

  * Configuration file is parsed by DatabaseDescriptor (which also has all the default values, if any)
  * Thrift generates an API interface in Cassandra.java; the implementation is CassandraServer, and CassandraDaemon ties it together.
  * CassandraServer turns thrift requests into the internal equivalents, then StorageProxy does the actual work, then CassandraServer turns it back into thrift again
  * StorageService is kind of the internal counterpart to CassandraDaemon. It handles turning raw gossip into the right internal state.
  * AbstractReplicationStrategy controls what nodes get secondary, tertiary, etc. replicas of each key range. Primary replica is always determined by the token ring (in TokenMetadata) but you can do a lot of variation with the others. RackUnaware just puts replicas on the next N-1 nodes in the ring. RackAware puts the first non-primary replica in the next node in the ring in ANOTHER data center than the primary; then the remaining replicas in the same as the primary.
  * MessagingService handles connection pooling and running internal commands on the appropriate stage (basically, a threaded executorservice). Stages are set up in StageManager; currently there are read, write, and stream stages. (Streaming is for when one node copies large sections of its SSTables to another, for bootstrap or relocation on the ring.) The internal commands are defined in StorageService; look for registerVerbHandlers.

### Write path ###

  * StorageProxy gets the nodes responsible for replicas of the keys from the ReplicationStrategy, then sends RowMutation messages to them.
> > o If nodes are changing position on the ring, "pending ranges" are associated with their destinations in TokenMetadata and these are also written to.
> > o If nodes that should accept the write are down, but the remaining nodes can fulfill the requested ConsistencyLevel, the writes for the down nodes will be sent to another node instead, with a header (a "hint") saying that data associated with that key should be sent to the replica node when it comes back up. This is called HintedHandoff and reduces the "eventual" in "eventual consistency." Note that HintedHandoff is only an optimization; ArchitectureAntiEntropy is responsible for restoring consistency more completely.
  * on the destination node, RowMutationVerbHandler uses Table.Apply to hand the write first to CommitLog.java, then to the Memtable for the appropriate ColumnFamily.
  * When a Memtable is full, it gets sorted and written out as an SSTable asynchronously by ColumnFamilyStore.switchMemtable
> > o When enough SSTables exist, they are merged by ColumnFamilyStore.doFileCompaction + Making this concurrency-safe without blocking writes or reads while we remove the old SSTables from the list and add the new one is tricky, because naive approaches require waiting for all readers of the old sstables to finish before deleting them (since we can't know if they have actually started opening the file yet; if they have not and we delete the file first, they will error out). The approach we have settled on is to not actually delete old SSTables synchronously; instead we register a phantom reference with the garbage collector, so when no references to the SSTable exist it will be deleted. (We also write a compaction marker to the file system so if the server is restarted before that happens, we clean out the old SSTables at startup time.)
  * See ArchitectureSSTable and ArchitectureCommitLog for more details

### Read path ###

  * StorageProxy gets the nodes responsible for replicas of the keys from the ReplicationStrategy, then sends read messages to them
> > o This may be a SliceFromReadCommand, a SliceByNamesReadCommand, or a RangeSliceReadCommand, depending
  * On the data node, ReadVerbHandler gets the data from CFS.getColumnFamily or CFS.getRangeSlice and sends it back as a ReadResponse
> > o The row is located by doing a binary search on the index in SSTableReader.getPosition
> > o For single-row requests, we use a QueryFilter subclass to pick the data from the Memtable and SSTables that we are looking for. The Memtable read is straightforward. The SSTable read is a little different depending on which kind of request it is: + If we are reading a slice of columns, we use the row-level column index to find where to start reading, and deserialize block-at-a-time (where "block" is the group of columns covered by a single index entry) so we can handle the "reversed" case without reading vast amounts into memory + If we are reading a group of columns by name, we still use the column index to locate each column, but first we check the row-level bloom filter to see if we need to do anything at all
> > o The column readers provide an Iterator interface, so the filter can easily stop when it's done, without reading more columns than necessary + Since we need to potentially merge columns from multiple SSTable versions, the reader iterators are combined through a ReducingIterator, which takes an iterator of uncombined columns as input, and yields combined versions as output
  * If a quorum read was requested, StorageProxy waits for a majority of nodes to reply and makes sure the answers match before returning. Otherwise, it returns the data reply as soon as it gets it, and checks the other replies for discrepancies in the background in StorageService.doConsistencyCheck. This is called "read repair," and also helps achieve consistency sooner.
> > o As an optimization, StorageProxy only asks the closest replica for the actual data; the other replicas are asked only to compute a hash of the data.

Deletes

  * 


> See DistributedDeletes

Gossip

  * 

> based on "Efficient reconciliation and flow control for anti-entropy protocols:" http://www.cs.cornell.edu/home/rvr/papers/flowgossip.pdf
  * 

> See ArchitectureGossip for more details

Failure detection

  * 

> based on "The Phi accrual failure detector:" http://vsedach.googlepages.com/HDY04.pdf

Further reading

  * 

> The idea of dividing work into "stages" with separate thread pools comes from the famous SEDA paper: http://www.eecs.harvard.edu/~mdw/papers/seda-sosp01.pdf
  * 

> Crash-only design is another broadly applied principle. Valerie Henson's LWN article is a good introduction
  * 

> Cassandra's distribution is closely related to the one presented in Amazon's Dynamo paper. Read repair, adjustable consistency levels, hinted handoff, and other concepts are discussed there. This is required background material: http://www.allthingsdistributed.com/2007/10/amazons_dynamo.html. The related article on article on eventual consistency is also relevant. Jeff Darcy's article on Availability and Partition Tolerance explains the underlying principle of CAP better than most.
  * 

> Cassandra's on-disk storage model is loosely based on sections 5.3 and 5.4 of the Bigtable paper.
  * 

> Facebook's Cassandra team authored a paper on Cassandra for LADIS 09: http://www.cs.cornell.edu/projects/ladis2009/papers/lakshman-ladis2009.pdf. Most of the information there is applicable to Apache Cassandra (the main exception is the integration of ZooKeeper).

http://wiki.apache.org/cassandra/ArchitectureInternals