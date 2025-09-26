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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBMaterializedView;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.apache.empire.db.DBIndex.DBIndexType;
import org.apache.empire.dbms.oracle.DBMSHandlerOracle.BooleanType;

public class OracleDDLGenerator extends DBDDLGenerator<DBMSHandlerOracle>
{
    public static final String COLATTR_SQLEXPRESSION = "sqlExpression";
    
    public OracleDDLGenerator(DBMSHandlerOracle dmbsHandler)
    {
        super(dmbsHandler);
        // Database object name for DROP database
        databaseObjectName = "USER";
        // Alter Column Phrase
        alterColumnPhrase  = " MODIFY ";
        // set Oracle specific data types
        initDataTypes();
    }

    /**
     * sets Oracle specific data types
     */
    private void initDataTypes()
    {   // Override data types
        DATATYPE_INT_SMALL  = "NUMBER(5)";
        DATATYPE_INT_BIG    = "NUMBER(38)";
        DATATYPE_VARCHAR    = "VARCHAR2";
        DATATYPE_DECIMAL    = "NUMBER";
        DATATYPE_TIME       = "DATE";   // There is not Datatype for TIME
        DATATYPE_DATETIME   = "DATE";   // There is not Datatype for DATETIME
        if ( dbms.getBooleanType() == BooleanType.CHAR )
             DATATYPE_BOOLEAN = "CHAR(1)";
        else DATATYPE_BOOLEAN = "NUMBER(1,0)";
    }

    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, DBSQLBuilder sql)
    {
        switch (type)
        {
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
                if ( dbms.getBooleanType() == BooleanType.CHAR )
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
    protected void appendColumnDesc(DBTableColumn c, boolean alter, DBSQLBuilder sql)
    {
        // virtual column
        Object sqlExpr = c.getAttribute(COLATTR_SQLEXPRESSION);
        if (sqlExpr!=null)
        {   // Append virtual column
            c.addSQL(sql, DBExpr.CTX_NAME);
            sql.append(" AS (");
            sql.append(sqlExpr.toString());
            sql.append(")");
        }
        else
            super.appendColumnDesc(c, alter, sql);
    }

    @Override
    protected void addCreateIndexStmt(DBIndex index, DBSQLBuilder sql, DBSQLScript script)
    {
        if (index.getType()==DBIndexType.FULLTEXT)
        {   // add indextype
            sql.append(" INDEXTYPE IS CTXSYS.CONTEXT");
        }
        super.addCreateIndexStmt(index, sql, script);
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
    public void getDDLScript(DDLActionType type, DBObject dbo, DBSQLScript script)
    {
        super.getDDLScript(type, dbo, script);
        // Additional tasks
        if ((type==DDLActionType.DROP) && (dbo instanceof DBTable))
        {   // Drop Sequences
            for (DBColumn c : ((DBTable) dbo).getColumns())
            {
                if (c.getDataType() == DataType.AUTOINC && (c instanceof DBTableColumn))
                {   // SEQUENCE column
                    DBTableColumn column = (DBTableColumn) c;
                    String seqName = dbms.getColumnSequenceName(column);
                    script.addStmt("DROP SEQUENCE " + seqName);
                }
            }
        }
    }
    
    /**
     * Returns true if the sequence has been created successfully.
     */
    protected void createSequence(DBDatabase db, DBTableColumn column, DBSQLScript script)
    {
        String seqName = dbms.getColumnSequenceName(column);
        // createSQL
        DBSQLBuilder sql = dbms.createSQLBuilder();
        sql.append("-- creating sequence for column ");
        sql.append(column.getFullName());
        sql.append(" --\r\n");
        sql.append("CREATE SEQUENCE ");
        db.appendQualifiedName(sql, seqName, null);
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
    
    @Override
    protected void addCreateViewStmt(DBView v, DBSQLBuilder sql, DBSQLScript script)
    {
        // log.info("Adding create statmement for view {}.", v.getName());
        String stmt = sql.toString();
        if (v instanceof DBMaterializedView) {
            script.addStmt("-- DROP MATERIALIZED VIEW " + v.getFullName());
            stmt = StringUtils.replace(stmt, "CREATE VIEW", "CREATE MATERIALIZED VIEW");
        }
        else
            stmt = StringUtils.replace(stmt, "CREATE VIEW", "CREATE OR REPLACE VIEW");
        script.addStmt(stmt);
    }

    protected void createComment(DBDatabase db, String type, DBExpr expr, String comment, DBSQLScript script)
    {
        if (comment==null || comment.length()==0)
            return; // Nothing to do
        DBSQLBuilder sql = dbms.createSQLBuilder();
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
