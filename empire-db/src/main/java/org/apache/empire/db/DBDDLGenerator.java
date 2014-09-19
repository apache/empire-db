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
package org.apache.empire.db;

import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.MiscellaneousErrorException;
import org.apache.empire.exceptions.NotImplementedException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DBDDLGenerator<T extends DBDatabaseDriver>
{
    private static final Logger log = LoggerFactory.getLogger(DBDDLGenerator.class);
    
    protected T driver;

    // Data types
    protected String DATATYPE_INT_SMALL  = "SMALLINT";  // Integer with small size (usually 16-bit)
    protected String DATATYPE_INTEGER    = "INT";       // Integer with default size (usually 32-bit) 
    protected String DATATYPE_INT_BIG    = "BIGINT";    // Integer with long size (usually 64-bit)
    protected String DATATYPE_CHAR       = "CHAR";      // Fixed length characters (unicode)
    protected String DATATYPE_VARCHAR    = "VARCHAR";   // variable length characters (unicode)      
    protected String DATATYPE_DATE       = "DATE";
    protected String DATATYPE_TIMESTAMP  = "TIMESTAMP";
    protected String DATATYPE_BOOLEAN    = "BIT";
    protected String DATATYPE_DECIMAL    = "DECIMAL";
    protected String DATATYPE_FLOAT      = "FLOAT";     // floating point number (double precision 8 bytes)
    protected String DATATYPE_CLOB       = "CLOB";
    protected String DATATYPE_BLOB       = "BLOB";
    protected String DATATYPE_UNIQUEID   = "CHAR(36)";  // Globally Unique Identifier

    // Options
    protected boolean namePrimaryKeyConstraint = false; // Add name for primary key constraint
    protected String  alterColumnPhrase  = " ALTER ";   // Phrase for altering a column
    protected String  databaseObjectName = "DATABASE";  // Database object name for DROP database
    
    protected DBDDLGenerator(T driver)
    {
        this.driver = driver;
    }

    // Add statements
    protected void addCreateTableStmt(DBTable table, StringBuilder sql, DBSQLScript script)
    {
        log.info("Adding create statmement for table {}.", table.getName());
        script.addStmt(sql);
    }
    protected void addCreateIndexStmt(DBIndex index, StringBuilder sql, DBSQLScript script)
    {
        log.info("Adding create statmement for index {}.", index.getName());
        script.addStmt(sql);
    }
    protected void addCreateRelationStmt(DBRelation rel, StringBuilder sql, DBSQLScript script)
    {
        log.info("Adding create statmement for relation {}.", rel.getName());
        script.addStmt(sql);
    }
    protected void addCreateViewStmt(DBView v, StringBuilder sql, DBSQLScript script)
    {
        log.info("Adding create statmement for view {}.", v.getName());
        script.addStmt(sql);
    }
    protected void addAlterTableStmt(DBColumn col, StringBuilder sql, DBSQLScript script)
    {
        log.info("Adding alter statmement for column {}.", col.getFullName());
        script.addStmt(sql);
    }

    
    /**
     * appends the data type of a column
     * @param type the type
     * @param size the size
     * @param sql the builder that we will append to
     * @return true if further column attributes may be added or false otherwise
     */
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, StringBuilder sql)
    {
        switch (type)
        {
            case INTEGER:
            case AUTOINC:
            {   int bytes = Math.abs((int)size);
                if (bytes>0 && bytes<3)
                    sql.append(DATATYPE_INT_SMALL);
                else if (bytes>4)
                    sql.append(DATATYPE_INT_BIG);
                else // Default
                    sql.append(DATATYPE_INTEGER);  // Default integer length
            }
                break;
            case TEXT:
            case CHAR:
            {   // Char or Varchar
                sql.append((type==DataType.CHAR) ? DATATYPE_CHAR : DATATYPE_VARCHAR);
                // get length (sign may be used for specifying (unicode>0) or bytes (non-unicode<0)) 
                int len = Math.abs((int)size);
                if (len == 0)
                    len = (type==DataType.CHAR) ? 1 : 100;
                sql.append("(");
                sql.append(String.valueOf(len));
                sql.append(")");
            }
                break;
            case DATE:
                sql.append(DATATYPE_DATE);
                break;
            case DATETIME:
                sql.append(DATATYPE_TIMESTAMP);
                break;
            case BOOL:
                sql.append(DATATYPE_BOOLEAN);
                break;
            case FLOAT:
            {   sql.append(DATATYPE_FLOAT);
                // append precision (if specified)
                int prec = Math.abs((int)size);
                if (prec>0) {
                    sql.append("(");
                    sql.append(String.valueOf(prec));
                    sql.append(")");
                }
                break;
            }    
            case DECIMAL:
            {   sql.append(DATATYPE_DECIMAL);
                int prec  = (int) size;
                int scale = c.getDecimalScale();
                if (prec>0) {
                    // append precision and scale
                    sql.append("(");
                    sql.append(String.valueOf(prec));
                    sql.append(",");
                    sql.append(String.valueOf(scale));
                    sql.append(")");
                }
            }
                break;
            case CLOB:
                sql.append(DATATYPE_CLOB);
                break;
            case BLOB:
                sql.append(DATATYPE_BLOB);
                if (size > 0) {
                    sql.append("(").append((long) size).append(") ");
                }    
                break;
            case UNIQUEID:
                // emulate using java.util.UUID
                sql.append(DATATYPE_UNIQUEID);
                break;
            default:
                // Error: Unable to append column of type UNKNOWN
                throw new MiscellaneousErrorException("Error: Unable to append column of type UNKNOWN");
        }
        // done. Add more attributes (like e.g. NULLABLE or NOT NULL)
        return true;
    }
    
    /**
     * Appends a table column definition to a ddl statement
     * @param c the column which description to append
     * @param alter true if altering an existing column or false otherwise
     * @param sql the sql builder object
     */
    protected void appendColumnDesc(DBTableColumn c, boolean alter, StringBuilder sql)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        sql.append(" ");
        // Unknown data type
        if (!appendColumnDataType(c.getDataType(), c.getSize(), c, sql))
            return;
        // Default Value
        if (driver.isDDLColumnDefaults() && !c.isAutoGenerated() && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.append(driver.getValueString(c.getDefaultValue(), c.getDataType()));
        }
        // Nullable
        if (c.isRequired() ||  c.isAutoGenerated())
            sql.append(" NOT NULL");
    }
    
    /**
     * Appends the required DLL commands to create, drop or alter an object to the supplied DBDQLScript.
     * @param type operation to perform (CREATE, DROP, ALTER)
     * @param dbo the object for which to perform the operation (DBDatabase, DBTable, DBView, DBColumn, DBRelation) 
     * @param script the script to which to add the DDL command(s)
     */
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        // The Object's database must be attached to this driver
        if (dbo==null || dbo.getDatabase().getDriver()!=driver)
            throw new InvalidArgumentException("dbo", dbo);
        // Check Type of object
        if (dbo instanceof DBDatabase)
        { // Database
            switch (type)
            {
                case CREATE:
                    createDatabase((DBDatabase) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBDatabase) dbo).getSchema(), databaseObjectName, script);
                    return;
                default:
                    throw new NotImplementedException(this, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBTable)
        { // Table
            switch (type)
            {
                case CREATE:
                    createTable((DBTable) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBTable) dbo).getFullName(), "TABLE", script);
                    return;
                default:
                    throw new NotImplementedException(this, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBView)
        { // View
            switch (type)
            {
                case CREATE:
                    createView((DBView) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBView) dbo).getFullName(), "VIEW", script);
                    return;
                case ALTER:
                    dropObject(((DBView) dbo).getFullName(), "VIEW", script);
                    createView((DBView) dbo, script);
                    return;
                default:
                    throw new NotImplementedException(this, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBRelation)
        { // Relation
            switch (type)
            {
                case CREATE:
                    createRelation((DBRelation) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBRelation) dbo).getFullName(), "CONSTRAINT", script);
                    return;
                default:
                    throw new NotImplementedException(this, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBIndex)
        { // Relation
            switch (type)
            {
                case CREATE:
                    createIndex(((DBIndex) dbo).getTable(), (DBIndex) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBIndex) dbo).getFullName(), "INDEX", script);
                    return;
                default:
                    throw new NotImplementedException(this, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBTableColumn)
        { // Table Column
            alterTable((DBTableColumn) dbo, type, script);
        } 
        else
        { // dll generation not supported for this type
            throw new NotSupportedException(this, "getDDLScript() for "+dbo.getClass().getName());
        }
    }
        
    /**
     * Appends the DDL-Script for creating the given database to an SQL-Script<br>
     * This includes the generation of all tables, views and relations.
     * @param db the database to create
     * @param script the sql script to which to append the dll command(s)
     */
    protected void createDatabase(DBDatabase db, DBSQLScript script)
    {
        // Create all Tables
        for (DBTable dbTable : db.getTables())
        {
            createTable(dbTable, script);
        }
        // Create Relations
        for (DBRelation dbRelation : db.getRelations())
        {
            createRelation(dbRelation, script);
        }
        // Create Views
        for (DBView v : db.getViews())
        {
            try {
                createView(v, script);
            } catch (NotSupportedException e) {
                // View command not implemented
                log.warn("Error creating the view {0}. This view will be ignored.", v.getName());
            }
        }
    }

    /**
     * Appends the DDL-Script for dropping a database to the given script object 
     * @param db the database to drop
     * @param script the sql script to which to append the dll command(s)
     */
    protected void dropDatabase(DBDatabase db, DBSQLScript script)
    {
        dropObject(db.getSchema(), "DATABASE", script);
    }
    
    /**
     * Appends the DDL-Script for creating the given table to an SQL-Script 
     * @param t the table to create
     * @param script the sql script to which to append the dll command(s)
     */
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
        for (DBColumn dbColumn : t.getColumns()) {
            DBTableColumn c = (DBTableColumn) dbColumn;
            if (c.getDataType() == DataType.UNKNOWN)
                continue; // Ignore and continue;
            // Append column
            sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
            appendColumnDesc(c, false, sql);
            addSeparator = true;
        }
        // Primary Key
        DBIndex pk = t.getPrimaryKey();
        if (pk != null)
        { // add the primary key
            sql.append(",\r\n");
            if (namePrimaryKeyConstraint) {
                sql.append(" CONSTRAINT ");
                appendElementName(sql, pk.getName());
            }
            sql.append(" PRIMARY KEY (");
            addSeparator = false;
            // columns
            DBColumn[] keyColumns = pk.getColumns();
            for (DBColumn keyColumn : keyColumns) {
                sql.append(addSeparator ? ", " : "");
                keyColumn.addSQL(sql, DBExpr.CTX_NAME);
                addSeparator = true;
            }
            sql.append(")");
        }
        sql.append(")");
        // Create the table
        addCreateTableStmt(t, sql, script);
        // Create all Indexes
        createTableIndexes(t, pk, script);        
    }

    /**
     * Appends the DDL-Script for creating all indexes of table (except the primary key) to an SQL-Script 
     * @param t the table to create
     * @param pk the primary key index to ignore
     * @param script the sql script to which to append the dll command(s)
     */
    protected void createTableIndexes(DBTable t, DBIndex pk, DBSQLScript script)
    {
        // Create other Indexes (except primary key)
        for (DBIndex idx : t.getIndexes())
        {
            if (idx == pk || idx.getType() == DBIndex.PRIMARYKEY)
                continue;

            // Create Index
            createIndex(t, idx, script);
        }
    }

    /**
     * Appends the DDL-Script for creating a single index to an SQL-Script 
     * @param t the table
     * @param idx the index to create
     * @param script the sql script to which to append the dll command(s)
     */
    protected void createIndex(DBTable t, DBIndex idx, DBSQLScript script)
    {
        StringBuilder sql = new StringBuilder();

        // Create Index
        sql.append((idx.getType() == DBIndex.UNIQUE) ? "CREATE UNIQUE INDEX " : "CREATE INDEX ");
        appendElementName(sql, idx.getName());
        sql.append(" ON ");
        t.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" (");

        // columns
        boolean addSeparator = false;
        DBExpr[] idxColumns = idx.getExpressions();
        for (DBExpr idxColumn : idxColumns)
        {
            sql.append(addSeparator ? ", " : "");
            idxColumn.addSQL(sql, DBExpr.CTX_NAME);
            sql.append("");
            addSeparator = true;
        }
        sql.append(")");
        // Create Index
        addCreateIndexStmt(idx, sql, script);
    }
    
    /**
     * Appends the DDL-Script for creating the given foreign-key relation to an SQL-Script 
     * @param r the relation to create
     * @param script the sql script to which to append the dll command(s)
     */
    protected void createRelation(DBRelation r, DBSQLScript script)
    {
        DBTable sourceTable = (DBTable) r.getReferences()[0].getSourceColumn().getRowSet();
        DBTable targetTable = (DBTable) r.getReferences()[0].getTargetColumn().getRowSet();

        StringBuilder sql = new StringBuilder();
        sql.append("-- creating foreign key constraint ");
        sql.append(r.getName());
        sql.append(" --\r\n");
        sql.append("ALTER TABLE ");
        sourceTable.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" ADD CONSTRAINT ");
        appendElementName(sql, r.getName());
        sql.append(" FOREIGN KEY (");
        // Source Names
        boolean addSeparator = false;
        DBRelation.DBReference[] refs = r.getReferences();
        for (DBRelation.DBReference ref1 : refs)
        {
            sql.append((addSeparator) ? ", " : "");
            ref1.getSourceColumn().addSQL(sql, DBExpr.CTX_NAME);
            addSeparator = true;
        }
        // References
        sql.append(") REFERENCES ");
        targetTable.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" (");
        // Target Names
        addSeparator = false;
        for (DBRelation.DBReference ref : refs)
        {
            sql.append((addSeparator) ? ", " : "");
            ref.getTargetColumn().addSQL(sql, DBExpr.CTX_NAME);
            addSeparator = true;
        }
        sql.append(")");
        // On Delete Action
        if (r.getOnDeleteAction()==DBRelation.DBCascadeAction.CASCADE)
        {
            sql.append(" ON DELETE CASCADE");
        }
        // done
        addCreateRelationStmt(r, sql, script);
    }

    /**
     * Appends the DDL-Script for altering a table to an SQL-Script 
     * @param col the column which to add, modify or drop
     * @param type the type of operation to perform (CREATE | MODIFY | DROP)
     * @param script the sql script to which to append the dll command(s)
     */
    protected void alterTable(DBTableColumn col, DBCmdType type, DBSQLScript script)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ");
        col.getRowSet().addSQL(sql, DBExpr.CTX_FULLNAME);
        switch(type)
        {
            case CREATE:
                sql.append(" ADD ");
                appendColumnDesc(col, false, sql);
                break;
            case ALTER:
                sql.append(alterColumnPhrase);
                /*
                sql.append(" ALTER "); // Derby, H2,
                sql.append(" MODIFY "); // MySQL, Oracle
                sql.append(" ALTER COLUMN ");   // HSQL, Postgre, SQLServer
                */                  
                appendColumnDesc(col, true, sql);
                break;
            case DROP:
                sql.append(" DROP COLUMN ");
                sql.append(col.getName());
                break;
        }
        // done
        addAlterTableStmt(col, sql, script);
    }
    
    /**
     * Appends the DDL-Script for creating the given view to an SQL-Script 
     * @param v the view to create
     * @param script the sql script to which to append the dll command(s)
     */
    protected void createView(DBView v, DBSQLScript script)
    {
        // Create the Command
        DBCommandExpr cmd = v.createCommand();
        if (cmd==null)
        {   // Check whether Error information is available
            log.error("No command has been supplied for view " + v.getName());
            throw new NotSupportedException(this, v.getName() + ".createCommand");
        }
        // Make sure there is no OrderBy
        cmd.clearOrderBy();

        // Build String
        StringBuilder sql = new StringBuilder();
        sql.append( "CREATE VIEW ");
        v.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append( " (" );
        boolean addSeparator = false;
        for(DBColumn c : v.getColumns())
        {
            if (addSeparator)
                sql.append(", ");
            // Add Column name
            c.addSQL(sql, DBExpr.CTX_NAME);
            // next
            addSeparator = true;
        }
        sql.append(")\r\nAS\r\n");
        cmd.addSQL( sql, DBExpr.CTX_DEFAULT);
        // done
        addCreateViewStmt(v, sql, script);
    }
    
    /**
     * Appends the DDL-Script for dropping a database object to an SQL-Script 
     * @param name the name of the object to delete
     * @param objType the type of object to delete (TABLE, COLUMN, VIEW, RELATION, etc)
     * @param script the sql script to which to append the dll command(s)
     */
    protected void dropObject(String name, String objType, DBSQLScript script)
    {
        if (name == null || name.length() == 0)
            throw new InvalidArgumentException("name", name);
        // Create Drop Statement
        StringBuilder sql = new StringBuilder();
        sql.append("DROP ");
        sql.append(objType);
        sql.append(" ");
        appendElementName(sql, name);
        script.addStmt(sql);
    }
    
    // Internal helpers 
    protected boolean detectQuoteName(String name)
    {
        return driver.detectQuoteName(name);
    }

    protected void appendElementName(StringBuilder sql, String name)
    {
        driver.appendElementName(sql, name);
    }

}
