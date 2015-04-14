## Overview ##

Cassandra writes are first written to the CommitLog, and then to a per-ColumnFamily structure called a Memtable. A Memtable is basically a write-back cache of data rows that can be looked up by key -- that is, unlike a write-through cache, writes are batched up in the Memtable until it is full, before being written to disk as an SSTable.

## Flushing ##

The process of turning a Memtable into a SSTable is called flushing. You can manually trigger flush via jmx (e.g. with bin/nodetool), which you may want to do before restarting nodes since it will reduce CommitLog replay time. Memtables are sorted by key and then written out sequentially. Thus, writes are extremely fast, costing only a commitlog append and an amortized sequential write for the flush!

Once flushed, SSTable files are immutable; no further writes may be done. So, on the read path, the server must (potentially, although it uses tricks like bloom filters to avoid doing so unnecessarily) combine row fragments from all the SSTables on disk, as well as any unflushed Memtables, to produce the requested data.

## Compaction ##

To bound the number of SSTable files that must be consulted on reads, and to reclaim space taken by unused data, Cassandra performs compactions: merging multiple old SSTable files into a single new one. Compactions are triggered when at least N SStables have been flushed to disk, where N is tunable and defaults to 4. Four similar-sized SSTables are merged into a single one. They start out being the same size as your memtable flush size, and then form a hierarchy with each one doubling in size. So you'll have up to N of the same size as your memtable, then up to N double that size, then up to N double that size, etc.

"Minor" only compactions merge sstables of similar size; "major" compactions merge all sstables in a given ColumnFamily. Prior to Cassandra 0.6.6/0.7.0, only major compactions can clean out obsolete tombstones.

Since the input SSTables are all sorted by key, merging can be done efficiently, still requiring no random i/o. Once compaction is finished, the old SSTable files may be deleted: note that in the worst case (a workload consisting of no overwrites or deletes) this will temporarily require 2x your existing on-disk space used. In today's world of multi-TB disks this is usually not a problem but it is good to keep in mind when you are setting alert thresholds.

SSTables that are obsoleted by a compaction are deleted asynchronously when the JVM performs a GC. You can force a GC from jconsole if necessary, but Cassandra will force one itself if it detects that it is low on space. A compaction marker is also added to obsolete sstables so they can be deleted on startup if the server does not perform a GC before being restarted.

CFStoreMBean exposes sstable space used as getLiveDiskSpaceUsed (only includes size of non-obsolete files) and getTotalDiskSpaceUsed (includes everything).

## Further reading ##

(The high-level memtable/sstable design as well as the "Memtable" and "SSTable" names come from Cassandra's sections 5.3 and 5.4 of Google's Bigtable paper, although some of the terminology around compaction differs.)