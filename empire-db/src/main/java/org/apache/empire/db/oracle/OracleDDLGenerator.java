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
package org.apache.empire.db.oracle;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle.BooleanType;

public class OracleDDLGenerator extends DBDDLGenerator<DBDatabaseDriverOracle>
{
    public OracleDDLGenerator(DBDatabaseDriverOracle driver)
    {
        super(driver);
        // Database object name for DROP database
        databaseObjectName = "USER";
        // Alter Column Phrase
        alterColumnPhrase  = " MODIFY ";
        // Name Primary Keys
        namePrimaryKeyConstraint = true;
        // set Oracle specific data types
        initDataTypes(driver);
    }

    /**
     * sets Oracle specific data types
     * @param driver the oracle driver in use
     */
    private void initDataTypes(DBDatabaseDriverOracle driver)
    {   // Override data types
        DATATYPE_INT_SMALL  = "NUMBER(5)";
        DATATYPE_INT_BIG    = "NUMBER(38)";
        DATATYPE_VARCHAR    = "VARCHAR2";
        DATATYPE_DECIMAL    = "NUMBER";
        if ( driver.getBooleanType() == BooleanType.CHAR )
             DATATYPE_BOOLEAN = "CHAR(1)";
        else DATATYPE_BOOLEAN = "NUMBER(1,0)";
    }

    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, StringBuilder sql)
    {
        switch (type)
        {
            case TEXT:
            case VARCHAR:
            case CHAR:
            {   // Char or Varchar
                sql.append((type==DataType.CHAR) ? DATATYPE_CHAR : DATATYPE_VARCHAR);
                // get length
                int len = Math.abs((int)size);
                if (len == 0)
                    len = (type==DataType.CHAR) ? 1 : 100;
                sql.append("(");
                sql.append(String.valueOf(len));
                // Check sign for char (unicode) or bytes (non-unicode) 
                sql.append((c.isSingleByteChars()) ? " BYTE)" : " CHAR)");
            }
                break;
            case BOOL:
                if ( driver.getBooleanType() == BooleanType.CHAR )
                     sql.append("CHAR(1)");
                else sql.append("NUMBER(1,0)");
                break;
                
            default:
                // use default
                return super.appendColumnDataType(type, size, c, sql);
        }
        return true;
    }
 
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
        // Tables and the rest
        super.createDatabase(db, script);
    }

    @Override
    protected void dropDatabase(DBDatabase db, DBSQLScript script)
    {
        dropObject(null, db.getSchema(), "USER", script);
    }
    
    @Override
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        super.getDDLScript(type, dbo, script);
        // Additional tasks
        if ((type==DBCmdType.DROP) && (dbo instanceof DBTable))
        {   // Drop Sequences
            for (DBColumn c : ((DBTable) dbo).getColumns())
            {
                if (c.getDataType() == DataType.AUTOINC && (c instanceof DBTableColumn))
                {   // SEQUENCE column
                    DBTableColumn column = (DBTableColumn) c;
                    script.addStmt("DROP SEQUENCE " + column.getSequenceName());
                }
            }
        }
    }
    
    /**
     * Returns true if the sequence has been created successfully.
     */
    protected void createSequence(DBDatabase db, DBTableColumn c, DBSQLScript script)
    {
        String seqName = c.getSequenceName();
        // createSQL
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating sequence for column ");
        sql.append(c.getFullName());
        sql.append(" --\r\n");
        sql.append("CREATE SEQUENCE ");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        sql.append(" INCREMENT BY 1 START WITH 1 MINVALUE 0 NOCYCLE NOCACHE NOORDER");
        // executeDLL
        script.addStmt(sql);
    }

    @Override
    protected void createTable(DBTable t, DBSQLScript script)
    {
        super.createTable(t, script);
        // Add Column comments (if any)
        DBDatabase db = t.getDatabase();
        createComment(db, "TABLE", t, t.getComment(), script);
        for (DBColumn c : t.getColumns())
        {
            String com = c.getComment();
            if (com != null)
                createComment(db, "COLUMN", c, com, script);
        }
    }

    protected void createComment(DBDatabase db, String type, DBExpr expr, String comment, DBSQLScript script)
    {
        if (comment==null || comment.length()==0)
            return; // Nothing to do
        StringBuilder sql = new StringBuilder();
        sql.append("COMMENT ON ");
        sql.append(type);
        sql.append(" ");
        if (expr instanceof DBColumn)
        {
            DBColumn c = (DBColumn)expr;
            c.getRowSet().addSQL(sql, DBExpr.CTX_NAME);
            sql.append(".");
        }
        expr.addSQL(sql, DBExpr.CTX_NAME);
        sql.append(" IS '");
        sql.append(comment);
        sql.append("'");
        // Create Index
        script.addStmt(sql);
    }
    
}
