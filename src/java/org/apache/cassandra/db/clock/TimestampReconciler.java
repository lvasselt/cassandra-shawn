/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.db.clock;

import org.apache.cassandra.db.Column;
import org.apache.cassandra.db.IClock.ClockRelationship;
import org.apache.cassandra.utils.FBUtilities;

/**
 * Keeps the column with the highest timestamp. If both are equal
 * return the left column.
 */
public final class TimestampReconciler extends AbstractReconciler
{
    public static final TimestampReconciler instance = new TimestampReconciler();

    private TimestampReconciler()
    {/* singleton */}

    public Column reconcile(Column left, Column right)
    {
        ClockRelationship cr = left.clock().compare(right.clock());
        switch (cr)
        {
        case EQUAL:
            // tombstones take precedence.  (if both are tombstones, then it doesn't matter which one we use.)
            if (left.isMarkedForDelete())
                return left;
            if (right.isMarkedForDelete())
                return right;
            // break ties by comparing values.
            return FBUtilities.compareByteArrays(left.value(), right.value()) < 0 ? right : left;
        case GREATER_THAN:
            return left;
        case LESS_THAN:
            return right;
        default:
            throw new IllegalArgumentException(
                    "Timestamp clocks must either be equal, greater then or less than: " + cr);
        }
    }
}
