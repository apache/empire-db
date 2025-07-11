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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBIndex.DBIndexType;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.apache.empire.dbms.DBMSHandlerBase.DBSeqTable;

public class MSSqlDDLGenerator extends DBDDLGenerator<DBMSHandlerMSSQL>
{
    public MSSqlDDLGenerator(DBMSHandlerMSSQL dbms)
    {
        super(dbms);
        // set Oracle specific data types
        initDataTypes(dbms);
        // Alter Column Phrase
        alterColumnPhrase  = " ALTER COLUMN ";
    }

    /**
     * sets Oracle specific data types
     * @param dbms
     */
    protected void initDataTypes(DBMSHandlerMSSQL dbms)
    {   // Override data types
        DATATYPE_CHAR       = "NCHAR";      // Fixed length chars (unicode)
        DATATYPE_VARCHAR    = "NVARCHAR";   // variable length characters (unicode)      
        DATATYPE_DATE       = "DATE";
        DATATYPE_DATETIME   = (dbms.isUseDateTime2() ? "DATETIME2" : "DATETIME");
        DATATYPE_TIMESTAMP  = (dbms.isUseDateTime2() ? "DATETIME2" : "DATETIME");
        DATATYPE_CLOB       = "NTEXT";
        DATATYPE_BLOB       = "IMAGE";
        DATATYPE_UNIQUEID   = "UNIQUEIDENTIFIER";  // Globally Unique Identifier
    }

    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, DBSQLBuilder sql)
    {
        switch (type)
        {
            case AUTOINC:
                super.appendColumnDataType(type, size, c, sql);
                // Check for identity column
                if (dbms.isUseSequenceTable()==false)
                {   // Make this column the identity column
                    int minValue = ObjectUtils.getInteger(c.getAttribute(Column.COLATTR_MINVALUE), 1);
                    sql.append(" IDENTITY(");
                    sql.append(String.valueOf(minValue));
                    sql.append(", 1) NOT NULL");
                    return false;
                }
                break;
            case VARCHAR:
            case CHAR:
            {   // Char or Varchar
                if (type==DataType.CHAR)
                    sql.append((c.isSingleByteChars()) ? DATATYPE_CHAR.substring(1) : DATATYPE_CHAR);
                else
                    sql.append((c.isSingleByteChars()) ? DATATYPE_VARCHAR.substring(1) : DATATYPE_VARCHAR);
                // get length
                int len = Math.abs((int)size);
                if (len == 0)
                    len = (type==DataType.CHAR) ? 1 : 100;
                // Check sign for char (unicode) or bytes (non-unicode) 
                sql.append("(");
                sql.append(String.valueOf(len));
                sql.append(")");
            }
                break;
            case UNIQUEID:
                sql.append(DATATYPE_UNIQUEID);
                if (c.isAutoGenerated())
                    sql.append(" ROWGUIDCOL");
                break;
            case BLOB:
                sql.append(DATATYPE_BLOB);
                break;
                
            default:
                // use default
                return super.appendColumnDataType(type, size, c, sql);
        }
        return true;
    }
 
    @SuppressWarnings("unused")
    @Override
    protected void createDatabase(DBDatabase db, DBSQLScript script)
    {
        // User Master to create Database
        String databaseName = dbms.getDatabaseName();
        if (StringUtils.isNotEmpty(databaseName))
        {   // Create Database
            script.addStmt("USE master");
            script.addStmt("IF NOT EXISTS(SELECT * FROM sysdatabases WHERE name = '" + databaseName + "') CREATE DATABASE " + databaseName);
            script.addStmt("USE " + databaseName);
            script.addStmt("SET DATEFORMAT ymd");
            // Sequence Table
            if (dbms.isUseSequenceTable() && db.getTable(dbms.getSequenceTableName())==null)
                new DBSeqTable(dbms.getSequenceTableName(), db);
        }
        // default processing
        super.createDatabase(db, script);
    }
    
    @Override
    protected void addCreateIndexStmt(DBIndex index, DBSQLBuilder sql, DBSQLScript script)
    {
        // Check type
        if (index.getType()==DBIndexType.UNIQUE_ALLOW_NULL)
        {   // Add WHERE constraint for ALLOW_NULL
            boolean first = true;
            for (DBColumn col : index.getColumns())
            {
                // Check whether columns is nullable
                if (col.isRequired())
                    continue;
                // append
                sql.append((first) ? " WHERE " : " AND ");
                col.addSQL(sql, DBExpr.CTX_NAME);
                sql.append(" IS NOT NULL");
                first = false;
            }
        }
        // done
        super.addCreateIndexStmt(index, sql, script);
    }
    
    @Override
    protected void addCreateViewStmt(DBView v, DBSQLBuilder sql, DBSQLScript script)
    {
        // log.info("Adding create statmement for view {}.", v.getName());
        String stmt = sql.toString();
        stmt = StringUtils.replace(stmt, "CREATE VIEW", "CREATE OR ALTER VIEW");
        script.addStmt(stmt);
    }
    
}
