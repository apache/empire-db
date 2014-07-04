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
package org.apache.empire.db.hsql;

import java.util.Iterator;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;

public class HSqlDDLGenerator extends DBDDLGenerator<DBDatabaseDriverHSql>
{
    public HSqlDDLGenerator(DBDatabaseDriverHSql driver)
    {
        super(driver);
        // Database object name for DROP database
        databaseObjectName = "SCHEMA";
        // set Oracle specific data types
        initDataTypes();
    }

    /**
     * sets HSql specific data types
     */
    private void initDataTypes()
    {   // Override data types
        DATATYPE_CLOB       = "LONGVARCHAR";
        DATATYPE_BLOB       = "LONGVARBINARY";
    }

    /*
    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, StringBuilder sql)
    {
        switch (type)
        {
            default:
                // use default
                return super.appendColumnDataType(type, size, c, sql);
        }
        return true;
    }
    */
 
    @Override
    protected void createDatabase(DBDatabase db, DBSQLScript script)
    {
        // Create all Sequences
        for (DBTable table : db.getTables())
        {
            for (DBColumn dbColumn : table.getColumns())
            {
                DBTableColumn c = (DBTableColumn) dbColumn;
                if (c.getDataType() == DataType.AUTOINC) {
                    createSequence(db, c, script);
                }
            }
        }
        // default processing
        super.createDatabase(db, script);
    }

    /**
     * Appends the DDL-Script for creating a sequence to an SQL-Script<br>
     * @param db the database to create
     * @param c the column for which to create the sequence
     * @param script the sql script to which to append the dll command(s)
     */
    protected void createSequence(DBDatabase db, DBTableColumn c, DBSQLScript script)
    {
        Object defValue = c.getDefaultValue();
        String seqName = (defValue != null) ? defValue.toString() : c.toString();
        // createSQL
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating sequence for column ");
        sql.append(c.toString());
        sql.append(" --\r\n");
        sql.append("CREATE SEQUENCE ");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        sql.append(" START WITH 1");
        // executeDLL
        script.addStmt(sql);
    }

}