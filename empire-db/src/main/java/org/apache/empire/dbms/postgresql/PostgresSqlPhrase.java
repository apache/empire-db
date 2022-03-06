/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.dbms.postgresql;

/**
 * Enum for all SQL phrases that may be supplied by the dbms
 * @author rainer
 */
public enum PostgresSqlPhrase
{
    // functions
    AGE             ("AGE(?)"),
    AGE_BETWEEN     ("AGE(?, {0})"),
    EXTRACT         ("EXTRACT({0:*} FROM ?)"),  // :* is important!
    TO_TSQUERY      ("to_tsquery(?)"),
    TO_TSVECTOR     ("to_tsvector(?)"),
    PLAINTO_TSQUERY ("plainto_tsquery(?)");

    private final String functionSQL;
    private final boolean aggregate;
    
    private PostgresSqlPhrase(String phrase, boolean aggregate)
    {
        this.functionSQL = phrase;
        this.aggregate = aggregate;
    }
    
    private PostgresSqlPhrase(String sqlDefault)
    {
        this(sqlDefault, false);
    }

    public String getSQL()
    {
        return functionSQL;
    }

    public boolean isAggregate()
    {
        return aggregate;
    }
}
