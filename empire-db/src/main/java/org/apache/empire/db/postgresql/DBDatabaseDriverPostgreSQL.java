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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * This class provides support for the PostgreSQL database system.
 * 
 *
 */
public class DBDatabaseDriverPostgreSQL extends DBDatabaseDriver
{
    private static final Log log = LogFactory.getLog(DBDatabaseDriverPostgreSQL.class);
    
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
        public DBCommandPostreSQL(DBDatabase db)
        {
            super(db);
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
    public boolean createReverseFunction(Connection conn)
    {
        try {
            log.info("Creating reverse function: " + CREATE_REVERSE_FUNCTION);
            return (executeSQL(CREATE_REVERSE_FUNCTION, null, conn, null)>=0);
        } catch(SQLException e) {
            log.error("Unable to create reverse function!", e);
            return error(e);
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
     * @param type type of requrested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA: return true;
            case SEQUENCES:     return true;    
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
            case DOUBLE:   return "CAST(? AS DECIMAL)";
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
                    return createDatabase((DBDatabase) dbo, script);
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
    
    protected String createSequenceName(DBTableColumn c){
    	 Object defValue = c.getDefaultValue();
         return (defValue != null) ? defValue.toString() : c.toString();
    }
    
    /**
     * Returns true if the sequence has been created successfully.
     * 
     * @return true if the sequence has been created successfully
     */
    protected boolean createSequence(DBDatabase db, DBTableColumn c, DBSQLScript script)
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
        return script.addStmt(sql);
    }
    
    /**
     * Returns true if the table has been created successfully.
     * 
     * @return true if the table has been created successfully
     */
    protected boolean createTable(DBTable t, DBSQLScript script)
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
                sql.append("TEXT");
                break;
            case BLOB:
                sql.append("bytea");
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
    protected boolean createRelation(DBRelation r, DBSQLScript script)
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
    protected boolean alterTable(DBTableColumn col, DBCmdType type, DBSQLScript script)
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
        return script.addStmt(sql);
    }
    
    

    /**
     * Returns true if the view has been created successfully.
     * 
     * @return true if the view has been created successfully
     */
    protected boolean createView(DBView v, DBSQLScript script)
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
    protected boolean dropObject(String name, String objType, DBSQLScript script)
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
