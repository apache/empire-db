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

import java.util.Iterator;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle.BooleanType;

public class OracleDDLGenerator extends DBDDLGenerator<DBDatabaseDriverOracle>
{
    public OracleDDLGenerator(DBDatabaseDriverOracle driver)
    {
        super(driver);
        // Alter Column Phrase
        alterColumnPhrase  = " MODIFY ";
        // set Oracle specific data types
        initDataTypes(driver);
    }

    /**
     * sets Oracle specific data types
     * @param driver
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
        Iterator<DBTable> seqtabs = db.getTables().iterator();
        while (seqtabs.hasNext())
        {
            DBTable table = seqtabs.next();
            Iterator<DBColumn> cols = table.getColumns().iterator();
            while (cols.hasNext())
            {
                DBTableColumn c = (DBTableColumn) cols.next();
                if (c.getDataType() == DataType.AUTOINC)
                {
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
        dropObject(db.getSchema(), "USER", script);
    }
    
    /**
     * Returns true if the sequence has been created successfully.
     * 
     * @return true if the sequence has been created successfully
     */
    protected void createSequence(DBDatabase db, DBTableColumn c, DBSQLScript script)
    {
        Object defValue = c.getDefaultValue();
        String seqName = (defValue != null) ? defValue.toString() : c.toString();
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
        Iterator<DBColumn> columns = t.getColumns().iterator();
        while (columns.hasNext())
        {
            DBColumn c = columns.next();
            String com = c.getComment();
            if (com != null)
                createComment(db, "COLUMN", c, com, script);
        }
    }
    
    /**
     * Returns true if the comment has been created successfully.
     * 
     * @return true if the comment has been created successfully
     */
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
