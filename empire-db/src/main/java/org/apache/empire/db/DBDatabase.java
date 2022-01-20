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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBRelation.DBCascadeAction;
import org.apache.empire.db.exceptions.DatabaseNotOpenException;
import org.apache.empire.db.expr.column.DBCaseWhenExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.exceptions.MiscellaneousErrorException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
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
    private static Map<String, WeakReference<DBDatabase>> databaseMap = new LinkedHashMap<String, WeakReference<DBDatabase>>();
    
    /** 
     * find a database by id
     */
    public static synchronized DBDatabase findById(String dbIdent)
    {
        WeakReference<DBDatabase> ref = databaseMap.get(dbIdent);
        if (ref==null)
            return null; // not found
        DBDatabase db = ref.get();
        if (db==null) 
        {   // object reference not valid
            log.warn("Database width id='{}' habe been destroyed!", dbIdent);
            databaseMap.remove(dbIdent);
        }
        return db;
    }
    
    /** 
     * find a database by id
     */
    public static synchronized DBDatabase findByClass(Class<? extends DBDatabase> cls)
    {
        for (WeakReference<DBDatabase> ref : databaseMap.values())
        {   // find database by class
            DBDatabase db = ref.get();
            if (db!=null && cls.isInstance(db))
                return db;
        }
        log.warn("Database of class {} not found!", cls.getSimpleName());
        return null;
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
     * Frees all resources and unregisters database in global map.
     * Do not reuse this object afterwards!
     * Hint: Database must be closed!
     */
    public synchronized void destroy()
    {
        if (isOpen())
            throw new MiscellaneousErrorException("Database is open. Destroy not possible.");
        // unregister
        databaseMap.remove(this.instanceId);
        this.instanceId = null;
        // clear all 
        this.schema = null;
        this.linkName = null;
        tables.clear();
        relations.clear();
        views.clear();
    }
    
    /**
     * registers the database in the global list of databases
     * @param dbid
     */
    protected synchronized void register(String dbid)
    {
        // Check if it exists
        Set<String> invalidKeys = new HashSet<String>();
        for (Map.Entry<String, WeakReference<DBDatabase>> e : databaseMap.entrySet())
        {   
            DBDatabase dbInst = e.getValue().get(); 
            if (dbInst==this)
            {   // Remove from set
                log.error("Instance of database "+getClass().getName()+" already registered. Not registering same instance twice!");
                throw new ItemExistsException(e.getKey());
            }
            else if (dbInst==null) 
            {   // remove this instance
                invalidKeys.add(e.getKey());
            }
        }
        // Remove all invalid keys
        for (String key : invalidKeys)
        {
            databaseMap.remove(key);
        }
        invalidKeys.clear();
        // Find a unique key
        if (findById(dbid)!=null)
        {   int maxInstId=1;
            String instPrefix = dbid+":";
            for (String key : databaseMap.keySet())
            {
                if (databaseMap.get(key).get()==null)
                {   // not valid any more
                    log.warn("Database width id='{}' habe been destroyed!", key);
                    continue; 
                }
                else if (key.startsWith(instPrefix))
                {   // parse inst
                    int instId = Integer.parseInt(key.substring(instPrefix.length()));
                    if (instId > maxInstId)
                        maxInstId = instId; 
                }
            }
            // set global id
            this.instanceId = dbid+":"+String.valueOf(maxInstId+1);
        }
        else
        {   // use provided dbid
            this.instanceId = dbid;
        }
        // register database in global map
        log.info("Instance of database {} registered with instanceid={}", getClass().getName(), this.instanceId);
        databaseMap.put(this.instanceId, new WeakReference<DBDatabase>(this));
    }

    /**
     * returns the default database id
     * Override this to customize
     * @return the defaultId
     */
    protected String getDefaultId()
    {
        return getClass().getSimpleName(); 
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
    public void open(DBContext context)
    {
        // Close Database if already open
        if (isOpen())
            close(context);
        // Attach to driver
        DBDatabaseDriver driver = context.getDriver();
        driver.attachDatabase(this, context.getConnection());
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
    public void close(DBContext context)
    {
        if (driver != null)
            driver.detachDatabase(this, context.getConnection());
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
    public synchronized void getCreateDDLScript(DBSQLScript script)
    {
        DBDatabaseDriver prevDriver = this.driver;
        DBDatabaseDriver ddlDriver = script.getContext().getDriver();
        try {
            // Set driver
            if (this.driver!=null && this.driver!=ddlDriver && ddlDriver!=null)
            {   // The database belongs to a different driver
                throw new MiscellaneousErrorException("The database is attached to a different driver.");
            }
            // Temporarily change driver
            if (this.driver== null)
                this.driver = ddlDriver;
            // Get DDL Command
            generateDDLScript(script);
            
        } finally {
            this.driver = prevDriver; 
        }
    }

    /**
     * Override this to change or add DDL commands
     * @param script
     */
    protected void generateDDLScript(DBSQLScript script)
    {
        this.driver.getDDLScript(DBCmdType.CREATE, this, script); 
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
        return new DBValueExpr(this, value, DataType.VARCHAR);
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
     * Creates and returns a value object for the given scalar value.
     * 
     * @param value the scalar value
     * @param dataType the database systems data type used for this value
     * @return the new DBValueExpr object
     */
    public DBValueExpr getValueExpr(Object value, DataType dataType)
    {
        return new DBValueExpr(this, value, dataType);
    }

    /**
     * Creates and returns a value expression for a command parameter
     * 
     * @param param the command parameter
     * @return the corresponding DBValueExpr object
     */
    public DBValueExpr getParamExpr(DBCmdParam param)
    {
        return new DBValueExpr(this, param, param.getDataType());
    }    

    /**
     * Creates and returns a value expression for NULL
     * 
     * @return the corresponding DBValueExpr object
     */
    public DBValueExpr getNullExpr()
    {
        return new DBValueExpr(this, null, DataType.UNKNOWN);
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
        return Collections.unmodifiableList(this.tables);        
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
    public DBRelation addRelation(String name, DBRelation.DBReference... references)
    {
    	// Check
    	if (getRelation(name)!=null)
            throw new ItemExistsException(name); // Relation already exists
    	// Get default cascade action
    	DBTable targetTable = (DBTable)references[0].getTargetColumn().getRowSet();
    	DBCascadeAction deleteAction = targetTable.getDefaultCascadeDeleteAction(); 
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
     * removes a relation from the list of relations
     * @param name
     */
    public void removeRelation(DBRelation relation)
    {
        if (relation==null || relation.getDatabase()!=this)
            throw new InvalidArgumentException("relation", relation);
        // remove
        this.relations.remove(relation);
    }

    /**
     * Returns the relations which have been defined in the database.
     * 
     * @return db relations.
     */
    public List<DBRelation> getRelations()
    {
        return Collections.unmodifiableList(this.relations);        
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
        return Collections.unmodifiableList(this.views);        
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
    public final DBCommand createCommand()
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
    
    /**
     * Detects the DataType of a given value.
     * @param value the value to detect
     * @return the DataType enum for the value
     */
    public DataType detectDataType(Object value)
    {
        if (value instanceof DBColumnExpr)
            return ((DBColumnExpr)value).getDataType();
        if (value instanceof String)
            return DataType.VARCHAR;
        if ((value instanceof Integer) || (value instanceof Long))
            return DataType.INTEGER;
        if (value instanceof Number)
            return DataType.DECIMAL;
        if (value instanceof Boolean)
            return DataType.BOOL;
        if (value instanceof Date)
            return DataType.DATETIME;
        if (value instanceof Character)
            return DataType.CHAR;
        if (value instanceof byte[])
            return DataType.BLOB;
        return DataType.UNKNOWN;
    }
    
    /**
     * Creates a case column expression
     * "case when <condition> then <trueValue> else <falseValue> end"
     * This is a helper function to simplify client usage
     * @param condition
     * @param trueValue the value to select if the condition is true
     * @param falseValue the value to select if the condition is false
     * @return an sql case expression
     */
    public DBColumnExpr caseWhen(DBCompareExpr condition, Object trueValue, Object falseValue)
    {
        DataType dataType = detectDataType((trueValue!=null ? trueValue : falseValue)); 
        DBColumnExpr trueExpr = ((trueValue  instanceof DBColumnExpr) ? (DBColumnExpr)trueValue : this.getValueExpr(trueValue, dataType));
        return trueExpr.when(condition, falseValue);
    }
    
    
    public DBColumnExpr caseWhen(Map<DBCompareExpr, DBColumnExpr> whenMap, DBColumnExpr elseValue)
    {
        return new DBCaseWhenExpr(whenMap, elseValue);
    }

    /**
     * Creates a case column expression that check whether a column or column expression is null
     * "case when <condition> is null then <trueValue> else <falseValue> end"
     * This is a helper function to simplify client usage
     * @param expr a column or column expression
     * @param trueValue the value to select if the condition is true
     * @param falseValue the value to select if the condition is false
     * @return an sql case expression
     */
    public DBColumnExpr caseWhenNull(DBColumnExpr expr, Object trueValue, Object falseValue)
    {
        return caseWhen(expr.is(null), trueValue, falseValue);
    }

}