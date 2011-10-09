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
package org.apache.empire.db.mysql;

import java.util.Iterator;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBDatabaseDriver.DBSeqTable;

public class MySQLDDLGenerator extends DBDDLGenerator<DBDatabaseDriverMySQL>
{
    public MySQLDDLGenerator(DBDatabaseDriverMySQL driver)
    {
        super(driver);
        // Alter Column Phrase
        alterColumnPhrase  = " MODIFY ";
        // set Oracle specific data types
        initDataTypes();
    }

    /**
     * sets Oracle specific data types
     * @param driver
     */
    private void initDataTypes()
    {   // Override data types
        DATATYPE_TIMESTAMP  = "DATETIME";
        DATATYPE_CLOB       = "LONGTEXT";
    }

    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, StringBuilder sql)
    {
        switch (type)
        {
            case AUTOINC:
            { // Auto increment
                super.appendColumnDataType(type, size, c, sql);
                if (driver.isUseSequenceTable()==false)
                    sql.append(" AUTO_INCREMENT");
                break;
            }    
           default:
                // use default
                return super.appendColumnDataType(type, size, c, sql);
        }
        return true;
    }
     
    @Override
    protected void createDatabase(DBDatabase db, DBSQLScript script)
    {
        // create Database if database name is supplied
        String databaseName = driver.getDatabaseName();
        if (StringUtils.isNotEmpty(databaseName))
        { 
            // Create Database
            script.addStmt("CREATE DATABASE IF NOT EXISTS " + databaseName + " CHARACTER SET " + driver.getCharacterSet());
            script.addStmt("USE " + databaseName);
            // appendDDLStmt(db, "SET DATEFORMAT ymd", buf);
            // Sequence Table
            if (driver.isUseSequenceTable() && db.getTable(driver.getSequenceTableName())==null)
                new DBSeqTable(driver.getSequenceTableName(), db);
        }
        // default processing
        super.createDatabase(db, script);
    }

    /**
     * Appends the DDL-Script for creating the given table to an SQL-Script 
     * @param t the table to create
     * @param script the sql script to which to append the dll command(s)
     */
    @Override
    protected void createTable(DBTable t, DBSQLScript script)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating table ");
        sql.append(t.getName());
        sql.append(" --\r\n");
        sql.append("CREATE TABLE ");
        t.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" (");
        boolean addSeparator = false;
        Iterator<DBColumn> columns = t.getColumns().iterator();
        while (columns.hasNext())
        {
            DBTableColumn c = (DBTableColumn) columns.next();
            // Append column
            sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
            appendColumnDesc(c, false, sql);
            addSeparator = true;
        }
        // Primary Key
        DBIndex pk = t.getPrimaryKey();
        if (pk != null)
        { // add the primary key
            sql.append(",\r\n PRIMARY KEY (");
            addSeparator = false;
            // columns
            DBColumn[] keyColumns = pk.getColumns();
            for (int i = 0; i < keyColumns.length; i++)
            {
                sql.append((addSeparator) ? ", " : "");
                keyColumns[i].addSQL(sql, DBExpr.CTX_NAME);
                addSeparator = true;
            }
            sql.append(")");
        }
        sql.append(")");
        // Engine
        addSeparator = false;
        if (StringUtils.isNotEmpty(driver.getEngine())) 
        {   // Set the table engine
            sql.append("\r\n ENGINE = ");
            sql.append(driver.getEngine());
            addSeparator = true;
        }
        // Comment?
        if (StringUtils.isNotEmpty(t.getComment()))
        {   // Add the table comment
            if (addSeparator)
                sql.append(",");
            sql.append("\r\n COMMENT = '");
            sql.append(t.getComment());
            sql.append("'");
        }
        // Create the table
        addCreateTableStmt(t, sql, script);
        // Create all Indexes
        createTableIndexes(t, pk, script);        
    }
    
}
