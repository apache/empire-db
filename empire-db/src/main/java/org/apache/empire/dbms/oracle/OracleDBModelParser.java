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
package org.apache.empire.dbms.oracle;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.empire.db.validation.DBModelParser;

public class OracleDBModelParser extends DBModelParser
{
    public OracleDBModelParser(String schemaName)    
    {
        super(null, schemaName);
    }
    
    /**
     * @return the database schema name
     */
    public String getSchemaName()
    {
        return schema;
    }

    /**
     * collects all column information at once
     */
    @Override
    protected int collectColumns(DatabaseMetaData dbMeta)
            throws SQLException
    {
        return super.collectColumns(dbMeta, null);
    }

    /**
     * collects all foreign keys at once
     */
    @Override
    protected int collectForeignKeys(DatabaseMetaData dbMeta)
            throws SQLException
    {
        return super.collectForeignKeys(dbMeta, null);
    }

}
