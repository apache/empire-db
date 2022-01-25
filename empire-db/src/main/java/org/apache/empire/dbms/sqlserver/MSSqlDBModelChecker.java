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
package org.apache.empire.dbms.sqlserver;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.validation.DBModelChecker;

/**
 * MSSqlDBModelChecker
 * DataModel checker implementation for Microsoft SQLServer
 * @author doebele
 */
public class MSSqlDBModelChecker extends DBModelChecker
{
    /**
     * create a MSSqlDBModelChecker
     * @param db the database 
     * @param catalog the catalog
     */
    public MSSqlDBModelChecker(DBDatabase db, String catalog, String schema)
    {
        super(catalog, StringUtils.coalesce(schema, "DBO"));
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
    
}
