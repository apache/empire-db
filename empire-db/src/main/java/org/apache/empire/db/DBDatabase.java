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

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBRelation.DBCascadeAction;
import org.apache.empire.db.exceptions.DatabaseNotOpenException;
import org.apache.empire.db.exceptions.InternalSQLException;
import org.apache.empire.db.exceptions.QueryFailedException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.exceptions.StatementFailedException;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.MiscellaneousErrorException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This abstract class is the applicaton's interface for a particular database schema.
 * <P>
 * It provides access to the various database objects such as tables, views and relations.
 * <P>
 * It also provides methods to execute DQL and DML SQL-commands.
 * <P>
 *
 */
public abstract class DBDatabase extends DBObject
{
    private final static long serialVersionUID = 1L;
  
    /**
     * This class represents the database systems current date and time.
     * <P>
     * There is no need to use this class directly.<BR>
     * Instead you can use the constant {@link DBDatabase#SYSDATE}
     */
    public static final class DBSystemDate implements Serializable
    {
        private final static long serialVersionUID = 1L;
        // System Date Class for internal use
        private DBSystemDate() 
        { 
            /* no instances */ 
        }
        @Override
        public String toString()
        {   return "sysdate";
        }
    }
    
    // Database specific date
    public static final DBSystemDate SYSDATE  = new DBSystemDate();
    
    public static final String EMPTY_STRING = "\0"; // will be replaced by ''

    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBDatabase.class);
    
    /** 
     * global map of all database instances that have been allocated
     */
    private static HashMap<String, WeakReference<DBDatabase>> databaseMap = new HashMap<String, WeakReference<DBDatabase>>();
    
    /** 
     * find a database by id
     */
    public static DBDatabase findById(String dbIdent)
    {
        if (databaseMap.containsKey(dbIdent)==false)
            log.warn("Database {} not found!", dbIdent);
        // get reference
        WeakReference<DBDatabase> ref = databaseMap.get(dbIdent); 
        return ref.get();
    }
    
    /** 
     * find a database by id
     */
    public static DBDatabase findByClass(Class<? extends DBDatabase> cls)
    {
        for (WeakReference<DBDatabase> ref : databaseMap.values())
        {   // find database by class
            DBDatabase db = ref.get();
            if (db!=null && cls.isInstance(db))
                return db;
        }
        throw new ItemNotFoundException(cls.getName());
    }

    /** the database schema * */
    protected String           schema    = null; // database schema name
    protected String           linkName  = null; // database link name
    protected List<DBTable>    tables    = new ArrayList<DBTable>();
    protected List<DBRelation> relations = new ArrayList<DBRelation>();
    protected List<DBView>     views     = new ArrayList<DBView>();
    protected DBDatabaseDriver driver    = null;
    protected String           instanceId;
    
    /**   
     * Property that indicates whether to always use usePreparedStatements (Default is false!)
     * Note: This will only apply for update and insert commands as well as for read operations on a DBRecord.
     * For custom SQL commands parameters must be explicitly declared using cmd.addCmdParam();   
     */
    private boolean preparedStatementsEnabled = false;

    /**
     * Constructs a new DBDatabase object and sets the specified schema object.
     * 
     * @param schema the database schema
     * @param linkName the database link name
     */
    public DBDatabase(String schema, String linkName)
    {
        this.schema = schema;
        this.linkName = linkName;

        // register database in global map
        register(getClass().getSimpleName());
    }

    /**
     * Constructs a new DBDatabase object and sets the specified schema object.
     * 
     * @param schema the database schema
     */
    public DBDatabase(String schema)
    {
        this(schema, null);
    }

    /**
     * Constructs a new DBDatabase object set the variable 'schema' = null.
     */
    public DBDatabase()
    {
        this(null, null);
    }
    
    /**
     * registers the database in the global list of databases
     * @param dbid
     */
    protected void register(String dbid)
    {
        // Check if it exists
        if (databaseMap.containsValue(this))
            databaseMap.remove(instanceId);
        // find a unique key
        int inst=0;
        for (String key : databaseMap.keySet())
        {
            if (key.startsWith(dbid) && databaseMap.get(key).get()!=null)
                inst++;
        }
        if (inst>0)
            this.instanceId = dbid+":"+String.valueOf(inst+1);
        else
            this.instanceId = dbid;
        // register database in global map
        databaseMap.put(this.instanceId, new WeakReference<DBDatabase>(this));
    }

    /**
     * Returns the database instance id
     * @return the identifier of the database
     */
    public String getId()
    {
        return instanceId;
    }

    // ------------------------------
    // -- Database methods --
    // ------------------------------

    /**
     * Returns the driver for this database.
     * 
     * @return Returns the driver for this database
     */
    public DBDatabaseDriver getDriver()
    {
        return driver;
    }
    
    /**
     * return whether prepared statements are preferred over normal statements (Default is false)
     * Note: This will only apply for update and insert commands as well as for read operations on a DBRecord.
     * For custom SQL commands parameters must be explicitly declared using cmd.addCmdParam();   
     * @return true if prepared Statements are enabled or false if not
     */
    public boolean isPreparedStatementsEnabled()
    {
        return preparedStatementsEnabled;
    }

    /**
     * enables or disables the use of prepared statements for update and insert commands as well as for read operations on a DBRecord.
     * Note: For custom SQL commands parameters must be explicitly declared using cmd.addCmdParam();   
     * @param preparedStatementsEnabled
     */
    public void setPreparedStatementsEnabled(boolean preparedStatementsEnabled)
    {
        this.preparedStatementsEnabled = preparedStatementsEnabled;
        // log prepared statement 
        log.info("PreparedStatementsEnabled is " + preparedStatementsEnabled);
    }

    /**
     * Sets the database driver for this database. This will
     * set up the connection for use.<br>
     * 
     * @param driver the database driver
     * @param conn the connection
     */
    public void open(DBDatabaseDriver driver, Connection conn)
    {
        // Close Database if already open
        if (isOpen())
            close(conn);
        // Attach to driver
        driver.attachDatabase(this, conn);
        // set new driver
        this.driver = driver;
    }

    /**
     * closes this database object by detaching it from the driver
     * this is a shortcut for calling
     *  getDriver().closeDatabase(db, conn)
     *   
     * @param conn the connection to close
     */
    public void close(Connection conn)
    {
        if (driver != null)
            driver.detachDatabase(this, conn);
        // No diver
        this.driver = null;
    }

    /**
     * Creates a DDL Script for creating all database objects on the target database.<BR>
     * This function may be called even if the database has not been previously opened.<BR>
     * <P>
     * Once the database is open you can use getDriver().getDLLCommand()
     * to create, alter or delete other database objects<BR>
     * <P>
     * @param driver The driver for which to create a DDL Script
     * @param script the script object that will be completed
     */
    public synchronized void getCreateDDLScript(DBDatabaseDriver driver, DBSQLScript script)
    {
        DBDatabaseDriver prevDriver = this.driver;
        try {
            // Set driver
            if (this.driver!=null && this.driver!=driver && driver!=null)
            {   // The database belongs to a different driver
                throw new MiscellaneousErrorException("The database is attached to a different driver.");
            }
            // Temporarily change driver
            if (this.driver== null)
                this.driver = driver;
            // Get DDL Command
            driver.getDDLScript(DBCmdType.CREATE, this, script);
            
        } finally {
            this.driver = prevDriver; 
        }
    }
    
    /**
     * @see org.apache.empire.db.DBObject#getDatabase()
     */
    @Override
    public DBDatabase getDatabase()
    {
        return this;
    }

    /**
     * Returns the schema for SQL statements.
     * 
     * @return the schema
     */
    public String getSchema()
    {
        return schema;
    }

    /**
     * Sets the schema for SQL statements.
     * 
     * @param schema the schema to set
     */
    public void setSchema(String schema)
    {   // Database must not be open so far
        if (driver != null)
            throw new PropertyReadOnlyException("schema");
        // Set Schema 
        this.schema = schema;
    }

    /**
     * Returns the schema-prefix for SQL statements e.g. "SCHEMA."
     * or empty string if no schema is defined.
     * 
     * @return the schema-prefix
     */
    public String getSchemaPrefix()
    {
        if (schema == null)
        {
            return "";
        }
        return schema + ".";
    }

    /**
     * Returns the database link name.
     * 
     * @return the name of the database link
     */
    public String getLinkName()
    {
        return linkName;
    }

    /**
     * Sets the name of the database link used to identify objects.
     * 
     * @param linkName the database link name
     */
    public void setLinkName(String linkName)
    {   // Database must not be open so far
        if (driver != null)
            throw new PropertyReadOnlyException(linkName);
        // Set Link 
        this.linkName = linkName;
    }

    /**
     * Returns the full qualified object name including schema prefix
     * and database link postfix (if any).
     * 
     * @param name the object's name
     * 
     * @return the qualified object name
     */
    @Deprecated
    public String getQualifiedName(String name)
    {
        StringBuilder buf = new StringBuilder();
        boolean quoteName = (driver!=null) ? driver.detectQuoteName(name) : false;
        appendQualifiedName(buf, name, quoteName);
        return buf.toString();
    }
    
    /**
     * Adds a full qualified object name including schema prefix
     * and database link postfix (if any).
     * to the string buffer supplied
     * 
     * @param buf the string buffer to which to append the qualified object name
     * @param name the object's name
     * @param quoteName use quotes or not
     */
    public void appendQualifiedName(StringBuilder buf, String name, boolean quoteName)
    {
        // Check driver
        if (driver==null)
        {   // No driver attached!
            log.warn("No driver attached for appending qualified name {0}.", name);
            buf.append(name);
            return;
        }
        // Schema
        if (schema != null)
        { // Add Schema
            buf.append(schema);
            buf.append(".");
        }
        // Append the name
        driver.appendElementName(buf, name, quoteName);
        // Database Link
        if (linkName!=null)
        { // Add Schema
            buf.append(driver.getSQLPhrase(DBDatabaseDriver.SQL_DATABASE_LINK));
            buf.append(linkName);
        }
    }
    
    /**
     * Creates and returns a value object for the database systems
     * current date and time.
     * 
     * @return a DBValueExpr object
     */
    public DBValueExpr getSystemDateExpr()
    {
        return new DBValueExpr(this, SYSDATE, DataType.DATETIME);
    }

    /**
     * Creates and returns a value object for the given string value.
     * 
     * @param value the String value
     * @return the new DBValueExpr object
     */
    public DBValueExpr getValueExpr(String value)
    {
        return new DBValueExpr(this, value, DataType.TEXT);
    }

    /**
     * Creates and returns a value object for the given boolean value.
     * 
     * @param value the Boolean value
     * @return the new DBValueExpr object
     */
    public DBValueExpr getValueExpr(boolean value)
    {
        return new DBValueExpr(this, value, DataType.BOOL);
    }

    /**
     * Creates and returns a value object for the given integer value.
     * 
     * @param value the int value
     * @return the new DBValueExpr object
     */
    public DBValueExpr getValueExpr(int value)
    {
        return new DBValueExpr(this, Integer.valueOf(value), DataType.INTEGER);
    }

    /**
     * Creates and returns a value object for the given long value.
     * 
     * @param value the long value
     * @return the new DBValueExpr object
     */
    public DBValueExpr getValueExpr(long value)
    {
        return new DBValueExpr(this, Long.valueOf(value), DataType.INTEGER);
    }

    /**
     * Creates and returns a value object for the given value.
     * 
     * @param value the value
     * @param dataType the database systems data type used for this value
     * @return the new DBValueExpr object
     */
    public DBValueExpr getValueExpr(Object value, DataType dataType)
    {
        return new DBValueExpr(this, value, dataType);
    }

    /**
     * Adds a DBTable object to list of database tables.<BR>
     * This function is called internally from the DBTable's constructor.
     * <P> 
     * @param table the DBTable object
     */
    protected void addTable(DBTable table)
    { // find column by name
        if (table == null || table.getDatabase() != this)
            throw new InvalidArgumentException("table", table);
        if (tables.contains(table)==true)
            throw new ItemExistsException(table.getName());
        // Check for second instances
        DBTable existing = getTable(table.getName()); 
        if (existing!=null)
        {   // Check classes
            if (existing.getClass().equals(table.getClass()))
                return; // Ignore other instances 
            // Table exists with different class
            throw new ItemExistsException(table.getName());
        }
        // add now
        tables.add(table);
    }

    /**
     * Returns the tables which have been defined in the database.
     * 
     * @return db tables.
     */
    public List<DBTable> getTables()
    {
        return tables;
    }

    /**
     * Finds a DBRowSet object (DBTable or DBView) by name.
     * <P>
     * @param name the name of the table
     * @return the located DBTable object
     */
    public DBRowSet getRowSet(String name)
    { // find table by name
        DBRowSet rset = getTable(name);
        if (rset==null)
            rset = getView(name);
        return rset;
    }

    /**
     * Finds a DBTable object by name.
     * <P>
     * @param name the name of the table
     * @return the located DBTable object
     */
    public DBTable getTable(String name)
    { // find table by name
        for (int i = 0; i < tables.size(); i++)
        { // search for the table
            DBTable tab = tables.get(i);
            if (tab.getName().equalsIgnoreCase(name))
                return tab;
        }
        return null;
    }

    /**
     * Adds a foreign key relation to the database.
     * <P>
     * @param reference a reference for a source and target column pair
     */
    public final DBRelation addRelation(DBRelation.DBReference reference)
    {
        String table = reference.getSourceColumn().getRowSet().getName();
        String col1 = reference.getSourceColumn().getName();
        // Create Relation Name
        String name = table.substring(0, Math.min(table.length(), 14)) 
                    + "_" + col1.substring(0, Math.min(col1.length(), 12))
        			+ "_FK";
        return addRelation(name, new DBRelation.DBReference[] { reference });
    }

    /**
     * Add a foreign key relation to the database.
     * 
     * @param ref1 a reference for a source and target column pair
     * @param ref2 a reference for a source and target column pair
     */
    public final DBRelation addRelation(DBRelation.DBReference ref1, DBRelation.DBReference ref2)
    {
        String table = ref1.getSourceColumn().getRowSet().getName();
        String col1 = ref1.getSourceColumn().getName();
        String col2 = ref2.getSourceColumn().getName();
        // Create Relation Name
        String name = table.substring(0, Math.min(table.length(), 9))
                    + "_" + col1.substring(0, Math.min(col1.length(), 9))
                    + "_" + col2.substring(0, Math.min(col2.length(), 9))
                    + "_FK";
        return addRelation(name, new DBRelation.DBReference[] { ref1, ref2 });
    }

    /**
     * Adds a foreign key relation to the database.
     * 
     * @param name the relation name
     * @param references a list of source and target column pairs
     */
    public DBRelation addRelation(String name, DBRelation.DBReference[] references)
    {
    	// Check
    	if (getRelation(name)!=null)
            throw new ItemExistsException(name); // Relation already exists
    	// Get default cascade action
    	DBTable targetTable = (DBTable)references[0].getTargetColumn().getRowSet();
    	DBCascadeAction deleteAction = (targetTable.isCascadeDelete() ? DBCascadeAction.CASCADE_RECORDS : DBCascadeAction.NONE); 
        // Add a Relation
        DBRelation relation = new DBRelation(this, name, references, deleteAction);
        if (relations.contains(relation))
            throw new ItemExistsException(name); // Relation already exists
        // Add Reference column to table
        for (DBRelation.DBReference ref : references)
        {   // add the reference column
            DBRowSet rset = ref.getSourceColumn().getRowSet();
            rset.addColumnReference(ref.getSourceColumn(), ref.getTargetColumn());
        }
        // OK
        relations.add(relation);
        return relation;
    }

    /**
     * Returns the relations which have been defined in the database.
     * 
     * @return db relations.
     */
    public List<DBRelation> getRelations()
    {
        return relations;
    }

    /**
     * Returns the relation of a given name
     * 
     * @return db the relation of the given name
     */
    public DBRelation getRelation(String relationName)
    {
        for (DBRelation r : relations)
        {
        	String name = r.getName();
        	if (relationName.compareToIgnoreCase(name)==0)
        		return r; 
        }
        return null;
    }

    /**
     * Adds a DBView object to list of database views.<BR>
     * This function is called internally from the DBView's constructor.
     * <P> 
     * @param view the DBView object
     */
    protected void addView(DBView view)
    { // find column by name
        if (view == null || view.getDatabase() != this)
            throw new InvalidArgumentException("view", view);
        if (views.contains(view) == true)
            throw new ItemExistsException(view.getName());
        // add now
        views.add(view);
    }

    /**
     * Returns the views which have been defined in the database.
     * 
     * @return db views.
     */
    public List<DBView> getViews()
    {
        return views;
    }

    /**
     * Finds a DBView object by name.
     * 
     * @param name the name of the view
     * @return the located DBTable object
     */
    public DBView getView(String name)
    { // find table by name
        for (int i = 0; i < views.size(); i++)
        { // search for the table
            DBView view = views.get(i);
            if (view.getName().equalsIgnoreCase(name))
                return view;
        }
        return null;
    }

    /**
     * Indicates whether the database has been opened.
     * 
     * @return The name of the encoding or null if a single byte encoding is used.
     */
    public boolean isOpen()
    {
        return (driver != null);
    }

    /**
     * checks if the database has been opened or false otherwise 
     */
    protected void checkOpen()
    {
        if (isOpen()==false)
            throw new DatabaseNotOpenException(this);
    }
    
    /**
     * Creates a new Command object for this database
     * 
     * @return the command object.
     */
    public DBCommand createCommand()
    {
        checkOpen(); 
        return driver.createCommand(this);
    }

    /**
     * Returns a timestamp that is used for record updates.
     * 
     * @param conn the connection
     * @return the current date and time.
     */
    public java.sql.Timestamp getUpdateTimestamp(Connection conn)
    {
        // Ask driver
        checkOpen(); 
        return driver.getUpdateTimestamp(conn);
    }

    // Sequences
    public Object getNextSequenceValue(String seqName, Connection conn)
    {
        // Ask driver
        checkOpen(); 
        return driver.getNextSequenceValue(this, seqName, 1, conn);
    }

    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * 
     * @param sqlCmd the SQL-Command
     * @param sqlParams list of query parameter values
     * @param conn a valid connection to the database.
     * 
     * @return the first column in the current row as a Java object 
     *         or <code>null</code> if there was no value 
     */
    public Object querySingleValue(String sqlCmd, Object[] sqlParams, Connection conn)
    {
        checkOpen(); 
        ResultSet rs = null;
        try
        {   // Debug
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, sqlParams, false, conn);
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
            // Check Result
            if (rs.next() == false)
            {   // no result
                log.debug("querySingleValue returned no result");
                throw new QueryNoResultException(sqlCmd);
            }
            // No Value
            Object result = rs.getObject(1);
            if (log.isDebugEnabled())
	            log.debug("querySingleValue complete in " + (System.currentTimeMillis() - start) + " ms -> value=" + result);
            return result;
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(this, sqlCmd, sqle);
        } finally
        { // Cleanup
            closeResultSet(rs);
        }
    }

    /**
     * Returns the value of the first row/column of a sql-query as an object.
     * 
     * @param sqlCmd the SQL-Command
     * @param conn a valid connection to the database.
     * 
     * @return the first column in the current row as a Java object 
     *         or <code>null</code> if there was no value 
     */
    public Object querySingleValue(String sqlCmd, Connection conn)
    {
        return querySingleValue(sqlCmd, (Object[])null, conn);  
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * 
     * @param sqlCmd the SQL statement
     * @param sqlParams list of query parameter values
     * @param defVal the default value if no value was returned by the database
     * @param conn a valid connection to the database.
     *
     * @return the result as a int value, if no result the int value 0
     */
    public int querySingleInt(String sqlCmd, Object[] sqlParams, int defVal, Connection conn)
    { 
        Object value = querySingleValue(sqlCmd, sqlParams, conn);
        return ObjectUtils.getInteger(value, defVal);
    }

    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * 
     * @param sqlCmd the SQL statement
     * @param defVal the default value if no value was returned by the database
     * @param conn a valid connection to the database.
     *
     * @return the result as a int value, if no result the int value 0
     */
    public int querySingleInt(String sqlCmd, int defVal, Connection conn)
    { 
        Object value = querySingleValue(sqlCmd, conn);
        return ObjectUtils.getInteger(value, defVal);
    }

    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     *
     * @return the result as a int value, if no result the int value 0
     */
    public final int querySingleInt(String sqlCmd, Connection conn)
    { 
        return querySingleInt(sqlCmd, 0, conn);
    }

    /**
     * Returns the value of the first row/column of a sql-query as a long.
     * 
     * @param sqlCmd the SQL statement
     * @param sqlParams list of query parameter values
     * @param defVal the default value
     * @param conn a valid connection to the database.
     * 
     * @return the result as a long value, if no result the long value 0
     */
    public long querySingleLong(String sqlCmd, Object[] sqlParams, long defVal, Connection conn)
    { 
        Object value = querySingleValue(sqlCmd, sqlParams, conn);
        return ((value != null) ? Long.parseLong(value.toString()) : defVal);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a long.
     * 
     * @param sqlCmd the SQL statement
     * @param defVal the default value
     * @param conn a valid connection to the database.
     * 
     * @return the result as a long value, if no result the long value 0
     */
    public long querySingleLong(String sqlCmd, long defVal, Connection conn)
    { 
        Object value = querySingleValue(sqlCmd, conn);
        return ((value != null) ? Long.parseLong(value.toString()) : defVal);
    }

    /**
     * Returns the value of the first row/column of a sql-query as a long.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     *
     * @return the result as a long value, if no result the long value 0
     */
    public final long querySingleLong(String sqlCmd, Connection conn)
    { 
        return querySingleLong(sqlCmd, 0, conn);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a string.
     * 
     * @param sqlCmd the SQL statement
     * @param sqlParams list of query parameter values
     * @param defVal the default value if no value was returned by the database
     * @param conn a valid connection to the database.
     *
     * @return the result as a String object, if no result a empty String
     */
    public String querySingleString(String sqlCmd, Object[] sqlParams, String defVal, Connection conn)
    { 
        Object value = querySingleValue(sqlCmd, sqlParams, conn);
        return ((value != null) ? value.toString() : defVal);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a string.
     * 
     * @param sqlCmd the SQL statement
     * @param defVal the default value if no value was returned by the database
     * @param conn a valid connection to the database.
     *
     * @return the result as a String object, if no result a empty String
     */
    public String querySingleString(String sqlCmd, String defVal, Connection conn)
    { 
        Object value = querySingleValue(sqlCmd, conn);
        return ((value != null) ? value.toString() : defVal);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a string.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     *
     * @return the result as a String object, if no result a empty String
     */
    public final String querySingleString(String sqlCmd, Connection conn)
    { 
        return querySingleString(sqlCmd, "", conn);
    }
    
    /**
     * Adds the first column of a query result to a collection.
     * 
     * @param c the class type for the list 
     * @param <T> the type for the list
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * 
     * @return the number of elements that have been added to the collection or -1 if an error occurred 
     */
    public <T> int querySimpleList(Class<T> c, String sqlCmd, Connection conn, Collection<T> result)
    {   // Start query
        checkOpen();
        ResultSet rs = null;
        try
        {   // Log performance
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, null, false, conn);
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
            // Check Result
            int count=0;
            while (rs.next())
            {
                T item = ObjectUtils.convert(c, rs.getObject(1));
                result.add(item);
                count++;
            }
            // No Value
            if (log.isDebugEnabled())
                log.debug("querySimpleList retured " + count + " items. Query completed in " + (System.currentTimeMillis() - start) + " ms");
            return count;
        } catch (ClassCastException e) 
        {   log.error("querySingleValue cast exception: ", e);
            throw new InternalException(e);
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(this, sqlCmd, sqle);
        } finally
        { // Cleanup
            closeResultSet(rs);
        }
    }

    /**
     * Returns a one dimensional array from an sql query.
     * The array is filled with the values of the first column.
     * 
     * @param c the class type for the list 
     * @param <T> the type for the list
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * 
     * @return a list of the values of the first column of an sql query 
     */
    public <T> List<T> querySimpleList(Class<T> c, String sqlCmd, Connection conn)
    {   // Execute the  Statement
        List<T> result = new ArrayList<T>();
        if (querySimpleList(c, sqlCmd, conn, result)<0)
            return null;
        return result;
    }
    
    /**
     * Returns a one dimensional array from an sql query.
     * The array is filled with the values of the first column.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * @return a list of values of type Object 
     */
    public List<Object> querySimpleList(String sqlCmd, Connection conn)
    {   // Execute the  Statement
        return querySimpleList(Object.class, sqlCmd, conn);
    }
    
    /**
     * Fills an option list provided with the result from a query.
     * The option list is filled with the values of the first and second column.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public int queryOptionList(String sqlCmd, Connection conn, Options result)
    {   // Execute the  Statement
        checkOpen();
        ResultSet rs = null;
        try
        {   // Debug
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, null, false, conn);
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
            if (rs.getMetaData().getColumnCount()<2)
                throw new InvalidArgumentException("sqlCmd", sqlCmd);
            // Check Result
            int count = 0;
            while (rs.next())
            {
                Object value = rs.getObject(1);
                String text  = rs.getString(2);
                result.add(value, text, true);
                count++;
            }
            // No Value
            if (log.isDebugEnabled())
                log.debug("queryOptionList retured " + count + " items. Query completed in " + (System.currentTimeMillis() - start) + " ms");
            return count;
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(this, sqlCmd, sqle);
        } finally
        { // Cleanup
            closeResultSet(rs);
        }
    }

    /**
     * Returns a list of key value pairs from an sql query.
     * The option list is filled with the values of the first and second column.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public Options queryOptionList(String sqlCmd, Connection conn)
    {   // Execute the  Statement
        Options options = new Options();
        queryOptionList(sqlCmd, conn, options);
        return options; 
    }
    
    /**
     * Adds the result of a query to a given collection.<br/>
     * The individual rows will be added as an array of objects (object[])
     * <p>This function should only be used for small lists.
     * Otherwise a DBReader should be used!</p>
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * @return a list of object arrays 
     */
    public int queryObjectList(String sqlCmd, Connection conn, Collection<Object[]> result)
    {   // Perform query
        checkOpen();
        ResultSet rs = null;
        try
        {   // Log performance
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, null, false, conn);
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
            // Read List
            int colCount = rs.getMetaData().getColumnCount();
            int count = 0;
            while (rs.next())
            {
                Object[] item = new Object[colCount];
                for (int i=0; i<colCount; i++)
                {   // Read from Resultset
                    item[i] = rs.getObject(i+1);
                }
                result.add(item);
                count++;
            }
            // No Value
            if (log.isDebugEnabled())
                log.debug("queryObjectList retured " + count + " items. Query completed in " + (System.currentTimeMillis() - start) + " ms");
            return count;
        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(this, sqlCmd, sqle);
        } finally
        { // Cleanup
            closeResultSet(rs);
        }
    } 

    /**
     * Returns the result of a query as a list Object-Arrays 
     * This function should only be used for small lists.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * @return a list of object arrays 
     */
    public List<Object[]> queryObjectList(String sqlCmd, Connection conn)
    {   // Execute the  Statement
        List<Object[]> result = new ArrayList<Object[]>();
        if (queryObjectList(sqlCmd, conn, result)<0)
            return null; // error
        return result;
    }
    
    /**
     * Executes an update, insert or delete SQL-Statement.<BR>
     * We recommend to use a DBCommand object in order to build the sqlCmd.<BR>
     * <P>
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of objects to replace sql parameters
     * @param conn a valid connection to the database.
     * @param setGenKeys object to set the generated keys for
     * @return the row count for insert, update or delete or 0 for SQL statements that return nothing
     */
    public int executeSQL(String sqlCmd, Object[] sqlParams, Connection conn, DBDatabaseDriver.DBSetGenKeys setGenKeys)
    {
        checkOpen();
        try 
        {   // Debug
            if (log.isInfoEnabled())
                log.info("Executing: " + sqlCmd);
            // execute SQL
            long start = System.currentTimeMillis();
            int affected = driver.executeSQL(sqlCmd, sqlParams, conn, setGenKeys);
            // number of affected records
            if (affected < 0)
                throw new UnexpectedReturnValueException(affected, "driver.executeSQL()");
            // Log
            if (log.isInfoEnabled())
	            log.info("executeSQL affected " + affected + " Records / " + (System.currentTimeMillis() - start) + "ms");
            // Return number of affected records
            return affected;
            
	    } catch (SQLException sqle) 
        { 	// Error
            throw new StatementFailedException(this, sqlCmd, sqle);
	    }    
    }

    public final int executeSQL(String sqlCmd, Object[] sqlParams, Connection conn)
    {
        return executeSQL(sqlCmd, sqlParams, conn, null); 
    }
    
    /**
     * Executes an update, insert or delete SQL-Statement.<BR>
     * We recommend to use a DBCommand object in order to build the sqlCmd.<BR>
     * <P>
     * @param sqlCmd the SQL-Command
     * @param conn a valid connection to the database.
     * @return the row count for insert, update or delete or 0 for SQL statements that return nothing
     */
    public final int executeSQL(String sqlCmd, Connection conn)
    {
        return executeSQL(sqlCmd, null, conn); 
    }
    
    /**
     * Executes a select SQL-Statement and returns a resultset containing the query results.<BR>
     * This function returns a JDBC ResultSet.<BR>
     * Instead of using this function directly you should use a DBReader object instead.<BR>
     * <P>
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of parameters for parameter queries (may depend on driver)
     * @param scrollable true if the reader should be scrollable or false if not
     * @param conn a valid connection to the database.
     * @return the JDBC ResutSet
     */
    public ResultSet executeQuery(String sqlCmd, Object[] sqlParams, boolean scrollable, Connection conn)
    {
        checkOpen();
        try
        {   // Debug
            if (log.isDebugEnabled())
    	        log.debug("Executing: " + sqlCmd);
            // Execute the Statement
            long start = System.currentTimeMillis();
            ResultSet rs = driver.executeQuery(sqlCmd, sqlParams, scrollable, conn);
            if (rs == null)
                throw new UnexpectedReturnValueException(rs, "driver.executeQuery()");
            // Debug
            if (log.isDebugEnabled())
                log.debug("executeQuery successful in " + (System.currentTimeMillis() - start) + " ms");
            // Return number of affected records
            return rs;

        } catch (SQLException sqle) 
        {   // Error
            throw new QueryFailedException(this, sqlCmd, sqle);
        } 
    }

    /**
     * Makes all changes made since the previous commit/rollback
     * permanent and releases any database locks currently held by the
     * Connection.
     * 
     * @param conn a valid database connection
     */
    public void commit(Connection conn)
    {
        try
        {   // Check argument
            if (conn==null)
                throw new InvalidArgumentException("conn", conn);
            // Commit
            if (conn.getAutoCommit()==false)
                conn.commit();
            // Done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new InternalSQLException(this, sqle);
        }
    }

    /**
     * Discards all changes made since the previous commit/rollback
     * and releases any database locks currently held by this
     * Connection.
     * <P>
     * @param conn a valid database connection
     */
    public void rollback(Connection conn)
    {
        try
        {   // Check argument
            if (conn==null)
                throw new InvalidArgumentException("conn", conn);
            // rollback
            log.info("Database rollback issued!");
            conn.rollback();
            // Done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new InternalSQLException(this, sqle);
        }
    }

    /**
     * Convenience function for closing a JDBC Resultset<BR>
     * Use it instead of stmt.close()<BR> 
     * <P>
     * @param stmt a Statement object
     */
    public void closeStatement(Statement stmt)
    {
        try
        { // Statement close
            if (stmt != null)
                stmt.close();
            // done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new InternalSQLException(this, sqle);
        }
    }

    /**
     * Convenience function for closing a JDBC Resultset<BR>
     * Use it instead of rset.close() and stmt.close()<BR> 
     * <P>
     * @param rset a ResultSet object
     */
    public void closeResultSet(ResultSet rset)
    {
        try
        { 	// check ResultSet
            if (rset == null)
                return; // nothing to do
            // close Resultset
            Statement stmt = rset.getStatement();
            rset.close();
            // check Statement
            if (stmt == null)
                return;
            // close Statement
            stmt.close();
            // done
            return;
        } catch (SQLException sqle) { 
            // Commit failed!
            throw new InternalSQLException(this, sqle);
        }
    }

}