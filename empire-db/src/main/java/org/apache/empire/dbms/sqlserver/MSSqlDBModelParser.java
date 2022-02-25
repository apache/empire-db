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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.validation.DBModelParser;

/**
 * MSSqlDBModelChecker
 * DataModel checker implementation for Microsoft SQLServer
 * @author doebele
 */
public class MSSqlDBModelParser extends DBModelParser
{
    /**
     * create a MSSqlDBModelChecker
     * @param db the database 
     * @param catalog the catalog
     */
    public MSSqlDBModelParser(String catalog, String schema)
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
    
    @Override
    protected boolean isIdentityColumn(ResultSet rs)
    {   try {
            int i = rs.findColumn("TYPE_NAME");
            return rs.getString(i).matches(".*(?i:identity).*");
        } catch(SQLException e) {
            log.warn("Missing column TYPE_NAME. Unable to detect Identity column");
            return false;
        }
    }
    
}
