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

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;

public class PostgresDDLGenerator extends DBDDLGenerator<DBMSHandlerPostgreSQL>
{
    /*
     * Script for creating the Reverse-Function
     */
    private static final String CREATE_REVERSE_FUNCTION =
        "CREATE OR REPLACE FUNCTION reverse(TEXT) RETURNS TEXT AS $$\n" +
        "DECLARE\n" +
        "   original ALIAS FOR $1;\n" +
        "   reversed TEXT := '';\n" +
        "   onechar  VARCHAR;\n" +
        "   mypos    INTEGER;\n" +
        "BEGIN\n" +
        "   SELECT LENGTH(original) INTO mypos;\n" + 
        "   LOOP\n" +
        "      EXIT WHEN mypos < 1;\n" +
        "      SELECT substring(original FROM mypos FOR 1) INTO onechar;\n" +
        "      reversed := reversed || onechar;\n" +
        "      mypos := mypos -1;\n" +
        "   END LOOP;\n" +
        "   RETURN reversed;\n" +
        "END\n" +
        "$$ LANGUAGE plpgsql IMMUTABLE RETURNS NULL ON NULL INPUT";    
    
    private boolean createReverseFunction = false;
    
    public PostgresDDLGenerator(DBMSHandlerPostgreSQL dbms)
    {
        super(dbms);
        // set Oracle specific data types
        initDataTypes();
    }

    /**
     * Returns whether the reverse function should be created with the database
     */
    public boolean isCreateReverseFunction()
    {
        return createReverseFunction;
    }

    /**
     * Set whether to create the reverse function with the database
     */
    public void setCreateReverseFunction(boolean createReverseFunction)
    {
        this.createReverseFunction = createReverseFunction;
    }

    /**
     * sets PostgreSQL specific data types
     */
    private void initDataTypes()
    {   // Override data types
        DATATYPE_BOOLEAN = "BOOLEAN";
        DATATYPE_CLOB = "TEXT";
        DATATYPE_BLOB = "BYTEA";
    }

    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, DBSQLBuilder sql)
    {
        switch (type)
        {
            case AUTOINC:
            { // Auto increment
                int bytes = Math.abs((int)size);
                if (bytes>= 8) {
                    sql.append(dbms.isUsePostgresSerialType() ? "BIGSERIAL" : DATATYPE_INT_BIG);
                } else {
                    sql.append(dbms.isUsePostgresSerialType() ? "SERIAL" : DATATYPE_INTEGER);
                }
                //String seqName = createSequenceName(c);
                //sql.append(" DEFAULT nextval('"+seqName+"')");
                break;
            }
            case FLOAT:
            {   // only use double precision
                sql.append("DOUBLE PRECISION");
                break;
            }
            case BLOB:
                sql.append(DATATYPE_BLOB);
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
        // Use SERIAL DataType?
        if (dbms.isUsePostgresSerialType()==false)
        {   // Not using SERIAL
            // Create all Sequences ourselves
            for (DBTable table : db.getTables())
            {
                for (DBColumn dbColumn : table.getColumns()) {
                    DBTableColumn c = (DBTableColumn) dbColumn;
                    if (c.getDataType() == DataType.AUTOINC) {
                        createSequence(db, c, script);
                    }
                }
            }
        }
        // create reverse function
        if (createReverseFunction)
            script.addStmt(CREATE_REVERSE_FUNCTION);
        // default processing
        super.createDatabase(db, script);
    }

    /**
     * Appends the DDL-Script for creating a sequence to an SQL-Script<br>
     * @param db the database to create
     * @param column the column for which to create the sequence
     * @param script the sql script to which to append the dll command(s)
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
        sql.append(" INCREMENT BY 1 START WITH 1 MINVALUE 0");
        // executeDLL
        script.addStmt(sql);
    }

    @Override
    protected void appendColumnDesc(DBTableColumn c, boolean alter, DBSQLBuilder sql)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        // Alter or create
        if (alter) {
            sql.append(" TYPE ");
        } else {
            sql.append(" ");
        }
        // Unknown data type
        if (!appendColumnDataType(c.getDataType(), c.getSize(), c, sql))
            return;
        // Default Value
        if (isDDLColumnDefaults() && !c.isAutoGenerated() && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.appendValue(c.getDataType(), c.getDefaultValue());
        }
        // Nullable
        if (c.isRequired() ||  c.isAutoGenerated())
            sql.append(" NOT NULL");
    }
    
}

