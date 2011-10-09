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
package org.apache.empire.db.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.Iterator;

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
import org.apache.empire.db.exceptions.InternalSQLException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotImplementedException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the PostgreSQL database system.
 * 
 *
 */
public class DBDatabaseDriverPostgreSQL extends DBDatabaseDriver
{
    private final static long serialVersionUID = 1L;
  
    private static final Logger log = LoggerFactory.getLogger(DBDatabaseDriverPostgreSQL.class);
    
    private static final String CREATE_REVERSE_FUNCTION =
        "CREATE OR REPLACE FUNCTION reverse(TEXT) RETURNS TEXT AS '\n" +
        "DECLARE\n" +
        "   original ALIAS FOR $1;\n" +
        "   reversed TEXT := \\'\\';\n" +
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
        "' LANGUAGE plpgsql IMMUTABLE RETURNS NULL ON NULL INPUT";    
    
    /**
     * Defines the PostgreSQL command type.
     */ 
    public static class DBCommandPostreSQL extends DBCommand
    {
        private final static long serialVersionUID = 1L;
      
        protected int limit = -1;
        protected int skip  = -1;
        
        public DBCommandPostreSQL(DBDatabase db)
        {
            super(db);
        }
        
        @Override
        public void limitRows(int numRows)
        {
            limit = numRows;
        }

        @Override
        public void skipRows(int numRows)
        {
            skip = numRows;
        }
         
        @Override
        public void clearLimit()
        {
            limit = -1;
            skip  = -1;
        }
        
        @Override
        public void getSelect(StringBuilder buf)
        {   // call base class
            super.getSelect(buf);
            // add limit and offset
            if (limit>=0)
            {   buf.append("\r\nLIMIT ");
                buf.append(String.valueOf(limit));
                // Offset
                if (skip>=0) 
                {   buf.append(" OFFSET ");
                    buf.append(String.valueOf(skip));
                }    
            }
        }
    }
    
    private String databaseName;
    
    /**
     * Constructor for the PostgreSQL database driver.<br>
     */
    public DBDatabaseDriverPostgreSQL()
    {
        setReservedKeywords();
    }
    
    private void addReservedKeyWord(final String keyWord){
        boolean added = reservedSQLKeywords.add(keyWord.toLowerCase());
        if(!added){
            log.debug("Existing keyWord added: " + keyWord);
        }
    }
    
    private void setReservedKeywords(){
        // list of reserved keywords
        // http://www.postgresql.org/docs/current/static/sql-keywords-appendix.html
        addReservedKeyWord("ALL".toLowerCase());
        addReservedKeyWord("ANALYSE".toLowerCase());
        addReservedKeyWord("ANALYZE".toLowerCase());
        addReservedKeyWord("AND".toLowerCase());
        addReservedKeyWord("ANY".toLowerCase());
        addReservedKeyWord("ARRAY".toLowerCase());
        addReservedKeyWord("AS".toLowerCase());
        addReservedKeyWord("ASC".toLowerCase());
        addReservedKeyWord("ASYMMETRIC".toLowerCase());
        addReservedKeyWord("AUTHORIZATION".toLowerCase());
        addReservedKeyWord("BETWEEN".toLowerCase());
        addReservedKeyWord("BINARY".toLowerCase());
        addReservedKeyWord("BOTH".toLowerCase());
        addReservedKeyWord("CASE".toLowerCase());
        addReservedKeyWord("CAST".toLowerCase());
        addReservedKeyWord("CHECK".toLowerCase());
        addReservedKeyWord("COLLATE".toLowerCase());
        //addReservedKeyWord("COLUMN".toLowerCase());
        //addReservedKeyWord("CONSTRAINT".toLowerCase());
        addReservedKeyWord("CREATE".toLowerCase());
        addReservedKeyWord("CROSS".toLowerCase());
        addReservedKeyWord("CURRENT_DATE".toLowerCase());
        addReservedKeyWord("CURRENT_ROLE".toLowerCase());
        addReservedKeyWord("CURRENT_TIME".toLowerCase());
        addReservedKeyWord("CURRENT_TIMESTAMP".toLowerCase());
        addReservedKeyWord("CURRENT_USER".toLowerCase());
        addReservedKeyWord("DEFAULT".toLowerCase());
        addReservedKeyWord("DEFERRABLE".toLowerCase());
        addReservedKeyWord("DESC".toLowerCase());
        addReservedKeyWord("DISTINCT".toLowerCase());
        addReservedKeyWord("DO".toLowerCase());
        addReservedKeyWord("ELSE".toLowerCase());
        addReservedKeyWord("END".toLowerCase());
        addReservedKeyWord("EXCEPT".toLowerCase());
        addReservedKeyWord("FALSE".toLowerCase());
        addReservedKeyWord("FOR".toLowerCase());
        addReservedKeyWord("FOREIGN".toLowerCase());
        addReservedKeyWord("FREEZE".toLowerCase());
        addReservedKeyWord("FROM".toLowerCase());
        addReservedKeyWord("FULL".toLowerCase());
        addReservedKeyWord("GRANT".toLowerCase());
        //addReservedKeyWord("GROUP".toLowerCase());
        addReservedKeyWord("HAVING".toLowerCase());
        addReservedKeyWord("ILIKE".toLowerCase());
        addReservedKeyWord("IN".toLowerCase());
        addReservedKeyWord("INITIALLY".toLowerCase());
        addReservedKeyWord("INNER".toLowerCase());
        addReservedKeyWord("INTERSECT".toLowerCase());
        addReservedKeyWord("INTO".toLowerCase());
        addReservedKeyWord("IS".toLowerCase());
        addReservedKeyWord("ISNULL".toLowerCase());
        addReservedKeyWord("JOIN".toLowerCase());
        addReservedKeyWord("LEADING".toLowerCase());
        addReservedKeyWord("LEFT".toLowerCase());
        addReservedKeyWord("LIKE".toLowerCase());
        addReservedKeyWord("LIMIT".toLowerCase());
        addReservedKeyWord("LOCALTIME".toLowerCase());
        addReservedKeyWord("LOCALTIMESTAMP".toLowerCase());
        addReservedKeyWord("NATURAL".toLowerCase());
        addReservedKeyWord("NEW".toLowerCase());
        addReservedKeyWord("NOT".toLowerCase());
        addReservedKeyWord("NOTNULL".toLowerCase());
        addReservedKeyWord("NULL".toLowerCase());
        addReservedKeyWord("OFF".toLowerCase());
        addReservedKeyWord("OFFSET".toLowerCase());
        addReservedKeyWord("OLD".toLowerCase());
        addReservedKeyWord("ON".toLowerCase());
        addReservedKeyWord("ONLY".toLowerCase());
        addReservedKeyWord("OR".toLowerCase());
        addReservedKeyWord("ORDER".toLowerCase());
        addReservedKeyWord("OUTER".toLowerCase());
        addReservedKeyWord("OVERLAPS".toLowerCase());
        addReservedKeyWord("PLACING".toLowerCase());
        addReservedKeyWord("PRIMARY".toLowerCase());
        addReservedKeyWord("REFERENCES".toLowerCase());
        addReservedKeyWord("RETURNING".toLowerCase());
        addReservedKeyWord("RIGHT".toLowerCase());
        //addReservedKeyWord("SELECT".toLowerCase());
        addReservedKeyWord("SESSION_USER".toLowerCase());
        addReservedKeyWord("SIMILAR".toLowerCase());
        addReservedKeyWord("SOME".toLowerCase());
        addReservedKeyWord("SYMMETRIC".toLowerCase());
        //addReservedKeyWord("TABLE".toLowerCase());
        addReservedKeyWord("THEN".toLowerCase());
        addReservedKeyWord("TO".toLowerCase());
        addReservedKeyWord("TRAILING".toLowerCase());
        addReservedKeyWord("TRUE".toLowerCase());
        addReservedKeyWord("UNION".toLowerCase());
        addReservedKeyWord("UNIQUE".toLowerCase());
        //addReservedKeyWord("USER".toLowerCase());
        addReservedKeyWord("USING".toLowerCase());
        addReservedKeyWord("VERBOSE".toLowerCase());
        addReservedKeyWord("WHEN".toLowerCase());
        addReservedKeyWord("WHERE".toLowerCase());
        addReservedKeyWord("WITH".toLowerCase()); 
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
     * Creates the reverse function in postgre sql that returns the reverse of a string value.
     * The reverse function may be helpful in SQL to analyze a text field from its end.
     * This function must be called manually by the application depending on whether it needs to use this function or not.<br/>
     * The current implementation does not check, whether the reverse function already exists.
     * If the functions exists it will be replaced and true is returned.
     * @param conn a valid database connection
     * @return true if the reverse function was created successfully or false otherwise
     */
    public void createReverseFunction(Connection conn)
    {
        try {
            log.info("Creating reverse function: " + CREATE_REVERSE_FUNCTION);
            executeSQL(CREATE_REVERSE_FUNCTION, null, conn, null);
        } catch(SQLException e) {
            log.error("Unable to create reverse function!", e);
            throw new InternalSQLException(this, e);
        }
    }
    
    /**
     * Creates a new PostgreSQL command object.
     * 
     * @return the new DBCommandPostgreSQL object
     */
    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        if (db == null)
            return null;
        // create command object
        return new DBCommandPostreSQL(db);
    }

    /**
     * Returns whether or not a particular feature is supported by this driver
     * @param type type of requested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA: 	return true;
            case SEQUENCES:     	return true;    
            case QUERY_LIMIT_ROWS:  return true;
            case QUERY_SKIP_ROWS:   return true;
            default:
                // All other features are not supported by default
                return false;
        }
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
            case SQL_CONCAT_EXPR:             return "? || {0}";
            // data types
            case SQL_BOOLEAN_TRUE:            return "TRUE";
            case SQL_BOOLEAN_FALSE:           return "FALSE";
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
            case SQL_FUNC_REVERSE:            return "reverse(?)"; // In order to use this function createReverseFunction() must be called first!
            case SQL_FUNC_STRINDEX:           return "strpos(?, {0})"; 
            case SQL_FUNC_STRINDEXFROM:       return "strindexfrom_not_available_in_pgsql({0}, ?, {1})"; // "locate({0}, ?, {1})"; 
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
        switch (destType)
        {
            case BOOL:     return "CAST(? AS BOOL)";
            case INTEGER:  return "CAST(? AS INTEGER)";
            case DECIMAL:  return "CAST(? AS DECIMAL)";
            case FLOAT:   return "CAST(? AS DOUBLE PRECISION)";
            case DATE:     return "CAST(? AS DATE)";
            case DATETIME: return "CAST(? AS TIMESTAMP)";
                // Convert to text
            case TEXT:     return "CAST(? AS CHAR)";
            case BLOB:     return "CAST(? AS bytea)";
            case CLOB:     return "CAST(? AS TEXT)";
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
    { 
        // Use PostgreSQL Sequences
        StringBuilder sql = new StringBuilder(80);
        sql.append("SELECT nextval('");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        sql.append("')");
        Object val = db.querySingleValue(sql.toString(), conn);
        if (val == null)
        { // Error!
            log.error("getNextSequenceValue: Invalid sequence value for sequence " + seqName);
        }
        return val;
    }
    
    /**
     * @see DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        // The Object's database must be attached to this driver
        if (dbo==null || dbo.getDatabase().getDriver()!=this)
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
                    dropObject(((DBDatabase) dbo).getSchema(), "DATABASE", script);
                    return;
                default:
                    throw new NotImplementedException(this, "getDDLScript."+dbo.getClass().getName()+"."+String.valueOf(type));
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
    protected boolean createDatabase(DBDatabase db, DBSQLScript script)
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
            createView(views.next(), script);
        }
        // Done
        return true;
    }
    
    protected String createSequenceName(DBTableColumn c){
    	 Object defValue = c.getDefaultValue();
         return (defValue != null) ? defValue.toString() : c.toString();
    }
    
    /**
     * Returns true if the sequence has been created successfully.
     * 
     * @return true if the sequence has been created successfully
     */
    protected void createSequence(DBDatabase db, DBTableColumn c, DBSQLScript script)
    {
    	String seqName = createSequenceName(c);
        // createSQL
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating sequence for column ");
        sql.append(c.getFullName());
        sql.append(" --\r\n");
        sql.append("CREATE SEQUENCE ");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        
//        create sequence foo_id_seq;
//        select setval('foo_id_seq', (select max(id) from foo));

        
        sql.append(" INCREMENT BY 1 START WITH 1 MINVALUE 0");
        // executeDLL
        script.addStmt(sql);
    }
    
    /**
     * Returns true if the table has been created successfully.
     * 
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
            sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
            if (appendColumnDesc(c, sql, false)==false)
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
        if (StringUtils.isNotEmpty(comment))
        {   // Add the table comment
            sql.append(" COMMENT = '");
            sql.append(comment);
            sql.append("'");
        }
        // Create the table
        script.addStmt(sql);
        // Create other Indexes (except primary key)
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
            script.addStmt(sql);
        }
    }
    
    /**
     * Appends a table column definition to a ddl statement
     * http://www.postgresql.org/docs/8.3/static/datatype.html
     * 
     * @param c the column which description to append
     * @param sql the sql builder object
     * @param alter is this for an alter statement
     * 
     * @return true if the column was successfully appended or false otherwise
     */
    protected boolean appendColumnDesc(DBTableColumn c, StringBuilder sql, boolean alter)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        if(alter)
        {
        	sql.append(" TYPE ");
        }else
        {
        	sql.append(" ");
        }
        switch (c.getDataType())
        {
            case INTEGER:
            {
                int size = (int) c.getSize();
                if (size >= 8) {
                    sql.append("BIGINT");
                } else {
                    sql.append("INT");
                }
//                if (size>0)
//                {   // Set Integer length
//                    sql.append("(");
//                    sql.append(String.valueOf(size));
//                    sql.append(")");
//                }
                break;
            }
            case AUTOINC:
            { // Auto increment
                int size = (int) c.getSize();
                if (size >= 8) {
                    sql.append("BIGSERIAL");
                } else {
                    sql.append("SERIAL");
                }

                //String seqName = createSequenceName(c);
                //sql.append(" DEFAULT nextval('"+seqName+"')");
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
                sql.append("TIMESTAMP");
                break;
            case BOOL:
                sql.append("BOOLEAN");
                break;
            case FLOAT:
                sql.append("DOUBLE PRECISION");
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
                sql.append("TEXT");
                break;
            case BLOB:
                sql.append("BYTEA");
                break;
            case UNIQUEID:
                // emulate using java.util.UUID
                sql.append("CHAR(36)");
                break;
            case UNKNOWN:
                log.warn("Cannot append column of Data-Type 'UNKNOWN'");
                return false;
        }
        // Default Value
        if (isDDLColumnDefaults() && !c.isAutoGenerated() && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.append(getValueString(c.getDefaultValue(), c.getDataType()));
        }
        // Nullable
        if (c.isRequired() ||  c.isAutoGenerated())
            sql.append(" NOT NULL");
        // Done
        return true;
    }

    /**
     * Returns true if the relation has been created successfully.
     * 
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
        script.addStmt(sql);
    }
    
    /**
     * Creates an alter table dll statement for adding, modifiying or droping a column.
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
                appendColumnDesc(col, sql, false);
                break;
            case ALTER:
                sql.append(" ALTER COLUMN ");
                appendColumnDesc(col, sql, true);
                break;
            case DROP:
                sql.append(" DROP COLUMN ");
                sql.append(col.getName());
                break;
        }
        // done
        script.addStmt(sql);
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
        script.addStmt(sql.toString());
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
    
}
