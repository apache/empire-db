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

import java.util.Iterator;

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
     * @param c
     * @param size
     * @param sql
     */
    protected void appendColumnDataType(DataType c, double size, StringBuilder sql)
    {
        switch (c)
        {
            case INTEGER:
                sql.append("INTEGER");
                break;
            case AUTOINC:
                sql.append("INTEGER");
                break;
            case TEXT:
            { // Check fixed or variable length
                int len = Math.abs((int)size);
                if (len == 0)
                    len = 100;
                sql.append("VARCHAR(");
                sql.append(String.valueOf(len));
                sql.append(")");
            }
                break;
            case CHAR:
            { // Check fixed or variable length
                int len = Math.abs((int)size);
                if (len == 0)
                    len = 1;
                sql.append("CHAR(");
                sql.append(String.valueOf(size));
                sql.append(")");
            }
                break;
            case DATE:
                sql.append("DATE");
                break;
            case DATETIME:
                sql.append("DATETIME");
                break;
            case BOOL:
                sql.append("BOOLEAN");
                break;
            case DOUBLE:
                sql.append("DOUBLE");
                break;
            case DECIMAL:
            {
                sql.append("NUMBER(");
                int prec = (int) size;
                int scale = (int) ((size - prec) * 10 + 0.5);
                // sql.append((prec+scale).ToString());sql.append(",");
                sql.append(String.valueOf(prec));
                sql.append(",");
                sql.append(String.valueOf(scale));
                sql.append(")");
            }
                break;
            case CLOB:
                sql.append("CLOB");
                break;
            case BLOB:
                sql.append("BLOB");
                if (size > 0)
                    sql.append(" (" + ((long)size) + ") ");
                break;
            case UNIQUEID:
                // emulate using java.util.UUID
                sql.append("CHAR(36)");
                break;
            default:
                // Error: Unable to append column of type UNKNOWN
                throw new MiscellaneousErrorException("Error: Unable to append column of type UNKNOWN");
        }
    }
    
    /**
     * Appends a table column definition to a ddl statement
     * @param c the column which description to append
     * @param sql the sql builder object
     * @return true if the column was successfully appended or false otherwise
     */
    protected void appendColumnDesc(DBTableColumn c, StringBuilder sql)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        sql.append(" ");
        // Unknown data type
        appendColumnDataType(c.getDataType(), c.getSize(), sql);
        // Default Value
        if (driver.isDDLColumnDefaults() && !c.isAutoGenerated() && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.append(driver.getValueString(c.getDefaultValue(), c.getDataType()));
        }
        // Nullable
        if (c.isRequired() ||  c.isAutoGenerated())
            sql.append(" NOT NULL");
    }
    
    // GetDDL
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
                    dropObject(((DBDatabase) dbo).getSchema(), "USER", script);
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
                    dropObject(((DBTable) dbo).getName(), "TABLE", script);
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
                    dropObject(((DBView) dbo).getName(), "VIEW", script);
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
                    dropObject(((DBRelation) dbo).getName(), "CONSTRAINT", script);
                    return;
                default:
                    throw new NotImplementedException(this, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBTableColumn)
        { // Table Column
            alterTable((DBTableColumn) dbo, type, script);
            return;
        } 
        else
        { // dll generation not supported for this type
            throw new NotSupportedException(this, "getDDLScript() for "+dbo.getClass().getName());
        }
    }

    /**
     * Returns true if the database has been created successfully.
     * 
     * @return true if the database has been created successfully
     */
    protected void createDatabase(DBDatabase db, DBSQLScript script)
    {
        // Create all Tables
        Iterator<DBTable> tables = db.getTables().iterator();
        while (tables.hasNext())
        {
            createTable(tables.next(), script);
        }
        // Create Relations
        Iterator<DBRelation> relations = db.getRelations().iterator();
        while (relations.hasNext())
        {
            createRelation(relations.next(), script);
        }
        // Create Views
        Iterator<DBView> views = db.getViews().iterator();
        while (views.hasNext())
        {
            try {
                createView(views.next(), script);
            } catch(NotImplementedException e) {
                // View command not implemented
                log.warn("Error creating the view {0}. This view will be ignored.");
                continue;
            }
        }
    }
    
    /**
     * Returns true if the table has been created successfully.
     * @return true if the table has been created successfully
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
        Iterator<DBColumn> columns = t.getColumns().iterator();
        while (columns.hasNext())
        {
            DBTableColumn c = (DBTableColumn) columns.next();
            if (c.getDataType()==DataType.UNKNOWN)
                continue; // Ignore and continue;
            // Append column
            sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
            appendColumnDesc(c, sql);
            addSeparator = true;
        }
        // Primary Key
        DBIndex pk = t.getPrimaryKey();
        if (pk != null)
        { // add the primary key
            sql.append(",\r\n CONSTRAINT ");
            appendElementName(sql, pk.getName());
            sql.append(" PRIMARY KEY (");
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
        // Create the table
        addCreateTableStmt(t, sql, script);
        // Create other Indexes (except primary key)
        Iterator<DBIndex> indexes = t.getIndexes().iterator();
        while (indexes.hasNext())
        {
            DBIndex idx = indexes.next();
            if (idx == pk || idx.getType() == DBIndex.PRIMARYKEY)
                continue;

            // Create Index
            sql.setLength(0);
            sql.append((idx.getType() == DBIndex.UNIQUE) ? "CREATE UNIQUE INDEX " : "CREATE INDEX ");
            appendElementName(sql, idx.getName());
            sql.append(" ON ");
            t.addSQL(sql, DBExpr.CTX_FULLNAME);
            sql.append(" (");
            addSeparator = false;

            // columns
            DBColumn[] idxColumns = idx.getColumns();
            for (int i = 0; i < idxColumns.length; i++)
            {
                sql.append((addSeparator) ? ", " : "");
                idxColumns[i].addSQL(sql, DBExpr.CTX_NAME);
                sql.append("");
                addSeparator = true;
            }
            sql.append(")");
            // Create Index
            addCreateIndexStmt(idx, sql, script);
        }
    }

    /**
     * Creates a sql string for creating a relation and appends it to the supplied buffer
     * @return true if the relation has been created successfully
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
        for (int i = 0; i < refs.length; i++)
        {
            sql.append((addSeparator) ? ", " : "");
            refs[i].getSourceColumn().addSQL(sql, DBExpr.CTX_NAME);
            addSeparator = true;
        }
        // References
        sql.append(") REFERENCES ");
        targetTable.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" (");
        // Target Names
        addSeparator = false;
        for (int i = 0; i < refs.length; i++)
        {
            sql.append((addSeparator) ? ", " : "");
            refs[i].getTargetColumn().addSQL(sql, DBExpr.CTX_NAME);
            addSeparator = true;
        }
        // done
        sql.append(")");
        // done
        addCreateRelationStmt(r, sql, script);
    }

    /**
     * Creates an alter table dll statement for adding, modifying or dropping a column.
     * @param col the column which to add, modify or drop
     * @param type the type of operation to perform
     * @param script to which to append the sql statement to
     * @return true if the statement was successfully appended to the buffer
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
                appendColumnDesc(col, sql);
                break;
            case ALTER:
                sql.append(" MODIFY ");
                appendColumnDesc(col, sql);
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
     * Returns true if the view has been created successfully.
     * 
     * @return true if the view has been created successfully
     */
    protected void createView(DBView v, DBSQLScript script)
    {
        // Create the Command
        DBCommandExpr cmd = v.createCommand();
        if (cmd==null)
        {   // Check whether Error information is available
            log.error("No command has been supplied for view " + v.getName());
            throw new NotImplementedException(this, v.getName() + ".createCommand");
        }
        // Make sure there is no OrderBy
        cmd.clearOrderBy();

        // Build String
        StringBuilder sql = new StringBuilder();
        sql.append( "CREATE OR REPLACE VIEW ");
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
     * Returns true if the object has been dropped successfully.
     * 
     * @return true if the object has been dropped successfully
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
