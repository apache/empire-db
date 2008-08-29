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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.empire.commons.Errors;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBDriverFeature;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;


/**
 * This class provides support for the MySQL database system.
 * 
 *
 */
public class DBDatabaseDriverMySQL extends DBDatabaseDriver
{
    /**
     * Defines the MySQL command type.
     */ 
    public static class DBCommandMySQL extends DBCommand
    {
        public DBCommandMySQL(DBDatabase db)
        {
            super(db);
        }
    }
    
    // Properties
    private String databaseName = null;
    private String characterSet = "utf8";
    // Sequence treatment
    // When set to 'false' (default) MySQL's autoincrement feature is used.
    private boolean useSequenceTable = false;
    private String sequenceTableName = "Sequences";
    
    /**
     * Constructor for the MySQL database driver.<br>
     */
    public DBDatabaseDriverMySQL()
    {
        // Default Constructor
    }

    /**
     * returns the name for the database / schema
     * @return the database / schema name
     */
    public String getDatabaseName()
    {
        return databaseName;
    }

    /**
     * Sets the name for the database / schema<br>
     * This names is required for creating a database.<br>
     * When a name is set, the driver will automatically execute 'USE dbname' when the database is opened.
     * @param databaseName the name of the database
     */
    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    /**
     * returns whether a sequence table is used for record identiy management.<br>
     * Default is false. In this case the AutoIncrement feature of MySQL is used.
     * @return true if a sequence table is used instead of identity columns.
     */
    public boolean isUseSequenceTable()
    {
        return useSequenceTable;
    }

    /**
     * If set to true a special table is used for sequence number generation.<br>
     * Otherwise the AutoIncrement feature of MySQL is used identiy fields. 
     * @param useSequenceTable true to use a sequence table or false otherwise.
     */
    public void setUseSequenceTable(boolean useSequenceTable)
    {
        this.useSequenceTable = useSequenceTable;
    }

    /**
     * returns the name of the sequence table
     * @return the name of the table used for sequence number generation
     */
    public String getSequenceTableName()
    {
        return sequenceTableName;
    }

    /**
     * Sets the name of the sequence table.
     * Only applicable if useSequenceTable is set to true.
     * @param sequenceTableName the name of the table used for sequence number generation
     */
    public void setSequenceTableName(String sequenceTableName)
    {
        this.sequenceTableName = sequenceTableName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.empire.db.DBDatabaseDriver#openDatabase(org.apache.empire.db.DBDatabase, java.sql.Connection)
     */
    @Override
    public boolean attachDatabase(DBDatabase db, Connection conn)
    {
        // Prepare
        try
        {   // Set Database
            if (StringUtils.isValid(databaseName))
                executeSQL("USE " + databaseName, null, conn);
            // Sequence Table
            if (useSequenceTable && db.getTable(sequenceTableName)==null)
                new DBSeqTable(sequenceTableName, db);
            // call Base implementation
            return super.attachDatabase(db, conn);
            
        } catch (SQLException e)
        {
            return error(e);
        }
    }

    /**
     * Creates a new MySQL command object.
     * 
     * @return the new DBCommandMySQL object
     */
    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        if (db == null)
            return null;
        // create command object
        return new DBCommandMySQL(db);
    }

    /**
     * Returns whether or not a particular feature is supported by this driver
     * @param type type of requrested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA: return true;
            case SEQUENCES:     return useSequenceTable;    
        }
        return false;
    }
    
    /**
     * Gets an sql phrase template for this database system.<br>
     * @see DBDatabaseDriver#getSQLPhrase(int)
     * @return the phrase template
     */
    @Override
    public String getSQLPhrase(int phrase)
    {
        switch (phrase)
        {
            // sql-phrases
            case SQL_NULL_VALUE:              return "null";
            case SQL_PARAMETER:               return " ? ";
            case SQL_RENAME_TABLE:            return " ";
            case SQL_RENAME_COLUMN:           return " AS ";
            case SQL_DATABASE_LINK:           return "@";
            case SQL_QUOTES_OPEN:             return "\"";
            case SQL_QUOTES_CLOSE:            return "\"";
            case SQL_CONCAT_EXPR:             return "concat(?, {0})";
            // data types
            case SQL_BOOLEAN_TRUE:            return "1";
            case SQL_BOOLEAN_FALSE:           return "0";
            case SQL_CURRENT_DATE:            return "CURRENT_DATE()";
            case SQL_DATE_PATTERN:            return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:           return "'{0}'";
            case SQL_CURRENT_DATETIME:        return "NOW()";
            case SQL_DATETIME_PATTERN:        return "yyyy-MM-dd HH:mm:ss";
            case SQL_DATETIME_TEMPLATE:       return "'{0}'";
            // functions
            case SQL_FUNC_COALESCE:           return "coalesce(?, {0})";
            case SQL_FUNC_SUBSTRING:          return "substring(?, {0})";
            case SQL_FUNC_SUBSTRINGEX:        return "substring(?, {0}, {1})";
            case SQL_FUNC_REPLACE:            return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:            return "reverse(?)"; 
            case SQL_FUNC_STRINDEX:           return "instr(?, {0})"; 
            case SQL_FUNC_STRINDEXFROM:       return "locate({0}, ?, {1})"; 
            case SQL_FUNC_LENGTH:             return "length(?)";
            case SQL_FUNC_UPPER:              return "upper(?)";
            case SQL_FUNC_LOWER:              return "lcase(?)";
            case SQL_FUNC_TRIM:               return "trim(?)";
            case SQL_FUNC_LTRIM:              return "ltrim(?)";
            case SQL_FUNC_RTRIM:              return "rtrim(?)";
            case SQL_FUNC_ESCAPE:             return "? escape '{0}'";
            // Numeric
            case SQL_FUNC_ABS:                return "abs(?)";
            case SQL_FUNC_ROUND:              return "round(?,{0})";
            case SQL_FUNC_TRUNC:              return "truncate(?,{0})";
            case SQL_FUNC_CEILING:            return "ceiling(?)";
            case SQL_FUNC_FLOOR:              return "floor(?)";
            // Date
            case SQL_FUNC_DAY:                return "day(?)";
            case SQL_FUNC_MONTH:              return "month(?)";
            case SQL_FUNC_YEAR:               return "year(?)";
            // Aggregation
            case SQL_FUNC_SUM:                return "sum(?)";
            case SQL_FUNC_COUNT:              return "count(?)";
            case SQL_FUNC_MAX:                return "max(?)";
            case SQL_FUNC_MIN:                return "min(?)";
            case SQL_FUNC_AVG:                return "avg(?)";
            // Others
            case SQL_FUNC_DECODE:             return "case ? {0} end";
            case SQL_FUNC_DECODE_SEP:         return " ";
            case SQL_FUNC_DECODE_PART:        return "when {0} then {1}";
            case SQL_FUNC_DECODE_ELSE:        return "else {0}";
            // Not defined
            default:
                log.error("SQL phrase " + String.valueOf(phrase) + " is not defined!");
                return "?";
        }
    }

    /**
     * @see DBDatabaseDriver#getConvertPhrase(DataType, DataType, Object)
     */
    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        switch(destType)
        {
           case BOOL:      return "CAST(? AS UNSIGNED)";
           case INTEGER:   return "CAST(? AS SIGNED)";
           case DECIMAL:   return "CAST(? AS DECIMAL)";
           case DOUBLE:    return "CAST(? AS DECIMAL)";
           case DATE:      return "CAST(? AS DATE)";
           case DATETIME:  return "CAST(? AS DATETIME)";
           // Convert to text
           case TEXT:
                return "CAST(? AS CHAR)";
           case BLOB:
                return "CAST(? AS BLOB)";
           // Unknown Type                                       
           default:
                log.error("getConvertPhrase: unknown type (" + String.valueOf(destType));
                return "?";
        }
    }
    
    /**
     * @see DBDatabaseDriver#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    {   //Use Oracle Sequences
        if (useSequenceTable)
        {   // Use a sequence Table to generate Sequences
            DBTable t = db.getTable(sequenceTableName);
            return ((DBSeqTable)t).getNextValue(seqName, minValue, conn);
        }
        else
        {   // Post Detection
            return null;
        }
    }
    
    @Override
    public Object getPostInsertAutoIncValue(DBDatabase db, Connection conn)
    {
        return db.querySingleInt("SELECT LAST_INSERT_ID()", conn);
    }

    /**
     * @see DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)  
     */
    @Override
    public boolean getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        // The Object's database must be attached to this driver
        if (dbo==null || dbo.getDatabase().getDriver()!=this)
            return error(Errors.InvalidArg, dbo, "dbo");
        // Check Type of object
        if (dbo instanceof DBDatabase)
        { // Database
            switch (type)
            {
                case CREATE:
                    return createDatabase((DBDatabase) dbo, script, true);
                case DROP:
                    return dropObject(((DBDatabase) dbo).getSchema(), "DATABASE", script);
                default:
                    return error(Errors.NotImplemented, "getDDLScript."+dbo.getClass().getName()+"."+String.valueOf(type));
            }
        } 
        else if (dbo instanceof DBTable)
        { // Table
            switch (type)
            {
                case CREATE:
                    return createTable((DBTable) dbo, script);
                case DROP:
                    return dropObject(((DBTable) dbo).getName(), "TABLE", script);
                default:
                    return error(Errors.NotImplemented, "getDDLCommand."+dbo.getClass().getName()+"."+String.valueOf(type));
            }
        } 
        else if (dbo instanceof DBView)
        { // View
            switch (type)
            {
                case CREATE:
                    return createView((DBView) dbo, script);
                case DROP:
                    return dropObject(((DBView) dbo).getName(), "VIEW", script);
                default:
                    return error(Errors.NotImplemented, "getDDLCommand."+dbo.getClass().getName()+"."+String.valueOf(type));
            }
        } 
        else if (dbo instanceof DBRelation)
        { // Relation
            switch (type)
            {
                case CREATE:
                    return createRelation((DBRelation) dbo, script);
                case DROP:
                    return dropObject(((DBRelation) dbo).getName(), "CONSTRAINT", script);
                default:
                    return error(Errors.NotImplemented, "getDDLCommand."+dbo.getClass().getName()+"."+String.valueOf(type));
            }
        } 
        else if (dbo instanceof DBTableColumn)
        { // Table Column
            return alterTable((DBTableColumn) dbo, type, script);
        } 
        else
        { // an invalid argument has been supplied
            return error(Errors.InvalidArg, dbo, "dbo");
        }
    }

    /**
     * Overridden. Returns a timestamp that is used for record updates created by the database server.
     * 
     * @return the current date and time of the database server.
     */
    @Override
    public java.sql.Timestamp getUpdateTimestamp(Connection conn)
    {
        // Default implementation
        GregorianCalendar cal = new GregorianCalendar();
        return new java.sql.Timestamp(cal.getTimeInMillis());
    }

    /*
     * return the sql for creating a Database
     */
    private boolean createDatabase(DBDatabase db, DBSQLScript script, boolean createSchema)
    {
        // User Master to create Database
        if (createSchema)
        {   // check database Name
            if (StringUtils.isValid(databaseName)==false)
                return error(Errors.InvalidProperty, "databaseName");
            // Create Database
            script.addStmt("CREATE DATABASE " + databaseName + " CHARACTER SET " + characterSet);
            script.addStmt("USE " + databaseName);
            // appendDDLStmt(db, "SET DATEFORMAT ymd", buf);
            // Sequence Table
            if (useSequenceTable && db.getTable(sequenceTableName)==null)
                new DBSeqTable(sequenceTableName, db);
        }
        // Create all Tables
        Iterator<DBTable> tables = db.getTables().iterator();
        while (tables.hasNext())
        {
            if (!createTable(tables.next(), script))
                return false;
        }
        // Create Relations
        Iterator<DBRelation> relations = db.getRelations().iterator();
        while (relations.hasNext())
        {
            if (!createRelation(relations.next(), script))
                return false;
        }
        // Create Views
        Iterator<DBView> views = db.getViews().iterator();
        while (views.hasNext())
        {
            if (!createView(views.next(), script))
                return false;
        }
        // Done
        return true;
    }
    
    /**
     * Returns true if the table has been created successfully.
     * 
     * @return true if the table has been created successfully
     */
    private boolean createTable(DBTable t, DBSQLScript script)
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
            sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
            if (appendColumnDesc(c, sql)==false)
                continue; // Ignore and continue;
            addSeparator = true;
        }
        // Primary Key
        DBIndex pk = t.getPrimaryKey();
        if (pk != null)
        { // add the primary key
            sql.append(", PRIMARY KEY (");
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
        // Comment?
        String comment = t.getComment();
        if (StringUtils.isValid(comment))
        {   // Add the table comment
            sql.append(" COMMENT = '");
            sql.append(comment);
            sql.append("'");
        }
        // Create the table
        if (script.addStmt(sql) == false)
            return false;
        // Create other Indizes (except primary key)
        Iterator<DBIndex> indexes = t.getIndexes().iterator();
        while (indexes.hasNext())
        {
            DBIndex idx = indexes.next();
            if (idx == pk || idx.getType() == DBIndex.PRIMARYKEY)
                continue;

            // Cretae Index
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
                addSeparator = true;
            }
            sql.append(")");
            // Create Index
            if (script.addStmt(sql) == false)
                return false;
        }
        // done
        return success();
    }
    
    /**
     * Appends a table column defintion to a ddl statement
     * @param c the column which description to append
     * @param sql the sql builder object
     * @return true if the column was successfully appended or false otherwise
     */
    private boolean appendColumnDesc(DBTableColumn c, StringBuilder sql)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        sql.append(" ");
        switch (c.getDataType())
        {
            case INTEGER:
            { // Integer type
                sql.append("INT");
                int size = (int)c.getSize();
                if (size>0)
                {   // Set Integer length
                    sql.append("(");
                    sql.append(String.valueOf(size));
                    sql.append(")");
                }
                break;
            }    
            case AUTOINC:
            { // Auto increment
                sql.append("INT");
                if (useSequenceTable==false)
                    sql.append(" AUTO_INCREMENT");
                break;
            }    
            case TEXT:
            { // Check fixed or variable length
                int size = Math.abs((int) c.getSize());
                if (size == 0)
                    size = 100;
                sql.append("VARCHAR(");
                sql.append(String.valueOf(size));
                sql.append(")");
            }
                break;
            case CHAR:
            { // Check fixed or variable length
                int size = Math.abs((int) c.getSize());
                if (size == 0)
                    size = 1;
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
                sql.append("BIT");
                break;
            case DOUBLE:
                sql.append("DOUBLE");
                break;
            case DECIMAL:
            { // Decimal
                sql.append("DECIMAL(");
                int prec = (int) c.getSize();
                int scale = (int) ((c.getSize() - prec) * 10 + 0.5);
                // sql.append((prec+scale).ToString());sql.append(",");
                sql.append(String.valueOf(prec));
                sql.append(",");
                sql.append(String.valueOf(scale));
                sql.append(")");
            }
                break;
            case CLOB:
                sql.append("LONGTEXT");
                break;
            case BLOB:
                sql.append("BLOB");
                if (c.getSize() > 0)
                    sql.append(" (" + String.valueOf((long) c.getSize()) + ") ");
                break;
            case UNKNOWN:
                 log.error("Cannot append column of Data-Type 'UNKNOWN'");
                 return false;
        }
        // Default Value
        if (isDDLColumnDefaults() && c.getDataType()!=DataType.AUTOINC && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.append(getValueString(c.getDefaultValue(), c.getDataType()));
        }
        // Nullable
        if (c.isRequired())
            sql.append(" NOT NULL");
        // Done
        return true;
    }

    /**
     * Returns true if the relation has been created successfully.
     * 
     * @return true if the relation has been created successfully
     */
    private boolean createRelation(DBRelation r, DBSQLScript script)
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
        if (script.addStmt(sql) == false)
            return false;
        // done
        return success();
    }

    /**
     * Creates an alter table dll statement for adding, modifiying or droping a column.
     * @param col the column which to add, modify or drop
     * @param type the type of operation to perform
     * @param buf buffer to which to append the sql statement to
     * @return true if the statement was successfully appended to the buffer
     */
    private boolean alterTable(DBTableColumn col, DBCmdType type, DBSQLScript script)
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
        return script.addStmt(sql);
    }

    /**
     * Returns true if the view has been created successfully.
     * 
     * @return true if the view has been created successfully
     */
    private boolean createView(DBView v, DBSQLScript script)
    {
        // Create the Command
        DBCommandExpr cmd = v.createCommand();
        if (cmd==null)
        {   // Check whether Error information is available
            log.error("No command has been supplied for view " + v.getName());
            if (v.hasError())
                return error(v);
            // No error information available: Use Errors.NotImplemented
            return error(Errors.NotImplemented, v.getName() + ".createCommand");
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
        return script.addStmt(sql.toString());
    }
    
    /**
     * Returns true if the object has been dropped successfully.
     * 
     * @return true if the object has been dropped successfully
     */
    private boolean dropObject(String name, String objType, DBSQLScript script)
    {
        if (name == null || name.length() == 0)
            return error(Errors.InvalidArg, name, "name");
        // Create Drop Statement
        StringBuilder sql = new StringBuilder();
        sql.append("DROP ");
        sql.append(objType);
        sql.append(" ");
        appendElementName(sql, name);
        return script.addStmt(sql);
    }

}
