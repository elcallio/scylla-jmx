/*
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
/*
 * Copyright 2015 ScyllaDB
 *
 * Modified by ScyllaDB
 */
package org.apache.cassandra.db.compaction;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import com.google.common.base.Throwables;

public class CompactionHistoryTabularData {
    private static final String[] ITEM_NAMES = new String[] { "id",
            "keyspace_name", "columnfamily_name", "compacted_at", "bytes_in",
            "bytes_out", "rows_merged" };

    private static final String[] ITEM_DESCS = new String[] { "time uuid",
            "keyspace name", "column family name", "compaction finished at",
            "total bytes in", "total bytes out", "total rows merged" };

    private static final String TYPE_NAME = "CompactionHistory";

    private static final String ROW_DESC = "CompactionHistory";

    private static final OpenType<?>[] ITEM_TYPES;

    private static final CompositeType COMPOSITE_TYPE;

    private static final TabularType TABULAR_TYPE;

    static {
        try {
            ITEM_TYPES = new OpenType[] { SimpleType.STRING, SimpleType.STRING,
                    SimpleType.STRING, SimpleType.LONG, SimpleType.LONG,
                    SimpleType.LONG, SimpleType.STRING };

            COMPOSITE_TYPE = new CompositeType(TYPE_NAME, ROW_DESC, ITEM_NAMES,
                    ITEM_DESCS, ITEM_TYPES);

            TABULAR_TYPE = new TabularType(TYPE_NAME, ROW_DESC, COMPOSITE_TYPE,
                    ITEM_NAMES);
        } catch (OpenDataException e) {
            throw Throwables.propagate(e);
        }
    }

    public static TabularData from(JsonArray resultSet)
            throws OpenDataException {
        TabularDataSupport result = new TabularDataSupport(TABULAR_TYPE);
        for (int i = 0; i < resultSet.size(); i++) {
            JsonObject row = resultSet.getJsonObject(i);
            String id = row.getString("id");
            String ksName = row.getString("ks");
            String cfName = row.getString("cf");
            long compactedAt = row.getJsonNumber("compacted_at").longValue();
            long bytesIn = row.getJsonNumber("bytes_in").longValue();
            long bytesOut = row.getJsonNumber("bytes_out").longValue();

            JsonArray merged = row.getJsonArray("rows_merged");
            StringBuilder sb = new StringBuilder();
            if (merged != null) {
                sb.append('{');
                for (int m = 0; m < merged.size(); m++) {
                    JsonObject entry = merged.getJsonObject(m);
                    if (m > 0) {
                        sb.append(',');
                    }
                    sb.append(entry.getString("key")).append(':')
                            .append(entry.getString("value"));

                }
                sb.append('}');
            }
            result.put(new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES,
                    new Object[] { id, ksName, cfName, compactedAt, bytesIn,
                            bytesOut, sb.toString() }));
        }
        return result;
    }
}
