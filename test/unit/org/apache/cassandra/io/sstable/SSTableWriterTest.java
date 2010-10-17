package org.apache.cassandra.io.sstable;
/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.cassandra.CleanupHelper;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.filter.IFilter;
import org.apache.cassandra.db.columniterator.IdentityQueryFilter;
import org.apache.cassandra.db.filter.QueryPath;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.thrift.IndexClause;
import org.apache.cassandra.thrift.IndexExpression;
import org.apache.cassandra.thrift.IndexOperator;
import org.apache.cassandra.utils.FBUtilities;
import org.junit.Test;

public class SSTableWriterTest extends CleanupHelper {

    @Test
    public void testRecoverAndOpen() throws IOException, ExecutionException, InterruptedException
    {
        RowMutation rm;

        rm = new RowMutation("Keyspace1", "k1".getBytes());
        rm.add(new QueryPath("Indexed1", null, "birthdate".getBytes("UTF8")), FBUtilities.toByteArray(1L), new TimestampClock(0));
        rm.apply();
        
        ColumnFamily cf = ColumnFamily.create("Keyspace1", "Indexed1");        
        cf.addColumn(new Column("birthdate".getBytes(), FBUtilities.toByteArray(1L), new TimestampClock(0)));
        cf.addColumn(new Column("anydate".getBytes(), FBUtilities.toByteArray(1L), new TimestampClock(0)));
        
        Map<byte[], byte[]> entries = new HashMap<byte[], byte[]>();
        
        DataOutputBuffer buffer = new DataOutputBuffer();
        ColumnFamily.serializer().serializeWithIndexes(cf, buffer);
        entries.put("k2".getBytes(), Arrays.copyOf(buffer.getData(), buffer.getLength()));        
        cf.clear();
        
        cf.addColumn(new Column("anydate".getBytes(), FBUtilities.toByteArray(1L), new TimestampClock(0)));
        buffer = new DataOutputBuffer();
        ColumnFamily.serializer().serializeWithIndexes(cf, buffer);               
        entries.put("k3".getBytes(), Arrays.copyOf(buffer.getData(), buffer.getLength()));
        
        SSTableReader orig = SSTableUtils.writeRawSSTable("Keyspace1", "Indexed1", entries);        
        // whack the index to trigger the recover
        FileUtils.deleteWithConfirm(orig.desc.filenameFor(Component.PRIMARY_INDEX));
        FileUtils.deleteWithConfirm(orig.desc.filenameFor(Component.FILTER));

        SSTableReader sstr = CompactionManager.instance.submitSSTableBuild(orig.desc).get();
        ColumnFamilyStore cfs = Table.open("Keyspace1").getColumnFamilyStore("Indexed1");
        cfs.addSSTable(sstr);
        cfs.buildSecondaryIndexes(cfs.getSSTables(), cfs.getIndexedColumns());
        
        IndexExpression expr = new IndexExpression("birthdate".getBytes("UTF8"), IndexOperator.EQ, FBUtilities.toByteArray(1L));
        IndexClause clause = new IndexClause(Arrays.asList(expr), "".getBytes(), 100);
        IFilter filter = new IdentityQueryFilter();
        IPartitioner p = StorageService.getPartitioner();
        Range range = new Range(p.getMinimumToken(), p.getMinimumToken());
        List<Row> rows = cfs.scan(clause, range, filter);
        
        assertEquals("IndexExpression should return two rows on recoverAndOpen", 2, rows.size());
        assertTrue("First result should be 'k1'",Arrays.equals("k1".getBytes(), rows.get(0).key.key));
    }
}
