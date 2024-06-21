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

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBRelation.DBCascadeAction;
import org.apache.empire.db.exceptions.DatabaseNotOpenException;
import org.apache.empire.db.exceptions.FieldIllegalValueException;
import org.apache.empire.db.exceptions.FieldNotNullException;
import org.apache.empire.db.exceptions.FieldValueException;
import org.apache.empire.db.exceptions.FieldValueOutOfRangeException;
import org.apache.empire.db.exceptions.FieldValueTooLongException;
import org.apache.empire.db.expr.column.DBCaseExpr;
import org.apache.empire.db.expr.column.DBCaseMapExpr;
import org.apache.empire.db.expr.column.DBCaseWhenExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemExistsException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.PropertyReadOnlyException;
import org.apache.empire.exceptions.InvalidOperationException;
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
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    /**
     * This class represents the database systems current date and time.
     * <P>
     * There is no need to use this class directly.<BR>
     * Instead you can use the constant {@link DBDatabase#SYSDATE}
     */
    public static final class DBSystemDate // *Deprecated* implements Serializable
    {
        // *Deprecated* private static final long serialVersionUID = 1L;
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
    public static DBDatabase findByIdentifier(String dbIdent)
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
    public static DBDatabase findByClass(Class<? extends DBDatabase> cls)
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

    // properties
    private String schema;          // database schema name
    private String linkName;        // database link name
    private String instanceId;      // internal instance id
    
    // Collections
    protected final List<DBTable>    tables    = new ArrayList<DBTable>();
    protected final List<DBRelation> relations = new ArrayList<DBRelation>();
    protected final List<DBView>     views     = new ArrayList<DBView>();
    
    protected DBMSHandler dbms    = null;
    
    /**   
     * Property that indicates whether to use Prepared Statements for the read and update operations in DBRecord (Default is true!).
     * Note: This will not affect statements generated via DBContext.createCommand()
     * However statement parameters can always be manually declared using cmd.addCmdParam();
     */
    private boolean autoPrepareStmt = true;

    /**
     * Flag indicating whether Bean getters / setters use java.util.Date or Java types (LocalDate, LocalDateTime)
     * True (default) when java.util.Date is used
     */
    protected boolean legacyDate  = true; 

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
    public synchronized void discard()
    {
        if (isOpen())
            throw new InvalidOperationException("Database is open. Discard not possible.");
        // unregister
        databaseMap.remove(this.instanceId);
        this.instanceId = null;
        // clear all 
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
        // Remove all invalid key
        for (String key : invalidKeys)
        {
            databaseMap.remove(key);
        }
        invalidKeys.clear();
        // Find a unique key
        if (findByIdentifier(dbid)!=null)
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
     * returns the default database identifier
     * Override this to customize
     * @return the defaultId
     */
    protected String getDefaultIdentifier()
    {
        return getClass().getSimpleName(); 
    }
    
    /**
     * Returns the database instance identifier
     * @return the identifier of the database
     */
    public String getIdentifier()
    {
        return instanceId;
    }

    // ------------------------------
    // -- Database methods --
    // ------------------------------

    /**
     * Returns the DBMS Handler for this database.
     * 
     * @return Returns the DBMS Handler for this database
     */
    @SuppressWarnings("unchecked")
    public <T extends DBMSHandler> T getDbms()
    {
        return (T)dbms;
    }
    
    /**
     * Returns whether Prepared Statements are enabled for the read and update operations in DBRecord.
     * Note: This will not affect statements generated via DBContext.createCommand()
     * However statement parameters can always be manually declared using cmd.addCmdParam();
     *    
     * @return true if prepared Statements are enabled or false if not
     */
    public boolean isPreparedStatementsEnabled()
    {
        return autoPrepareStmt;
    }

    /**
     * Enables or Disables the use of Prepared Statements only for the read and update operations in DBRecord.
     * For general use of Prepared Statements please use DBContext.createCommand()
     * However statement parameters can always be manually declared using cmd.addCmdParam();   
     *    
     * @param autoPrepareStmt flag whether to automatically convert literal values to prepared statement params
     */
    public void setPreparedStatementsEnabled(boolean autoPrepareStmt)
    {
        this.autoPrepareStmt = autoPrepareStmt;
        // log prepared statement 
        log.info("PreparedStatementsEnabled is " + autoPrepareStmt);
    }

    /**
     * Checks if the database exists
     * The implementation depends on the DBMSHandler
     * @return true if the database exists or false otherwise 
     */
    public boolean checkExists(DBContext context)
    {
        return context.getDbms().checkExists(this, context.getConnection());
    }
    
    /**
     * Attaches the Database to the DBMS Handler provided with the context  
     * and allows the Database and the Handler to perform initialization tasks
     * 
     * @param context the DBContext
     */
    public void open(DBContext context)
    {
        DBMSHandler dbms = context.getDbms();
        if (dbms==this.dbms)
        {
            log.warn("Database already attached to this dbms");
        }
        else if (this.dbms!=null)
        {
            log.error("Database already attached to another dbms {}", this.dbms.getClass().getName());
            throw new NotSupportedException(this, "open");
        }
        else
        {   // Attach to dbms
            dbms.attachDatabase(this, context.getConnection());
            // set latest dbms
            this.dbms = dbms;
        }
    }

    /**
     * Closes this database object by detaching it from the dbms
     *   
     * @param context the DBContext
     */
    public void close(DBContext context)
    {
        DBMSHandler dbms = context.getDbms();
        if (this.dbms == null)
        {
            log.warn("Database not attached to a dbms");
        }
        else if (dbms!=this.dbms)
        {
            log.error("Database attached to another dbms {}", this.dbms.getClass().getName());
            throw new NotSupportedException(this, "close");
        }
        else
        {   // Detach
            this.dbms.detachDatabase(this, context.getConnection());
            // No diver
            this.dbms = null;
        }
    }

    /**
     * Creates a DDL Script for creating all database objects on the target database.<BR>
     * This function may be called even if the database has not been previously opened.<BR>
     * <P>
     * Once the database is open you can use getDbms().getDLLCommand()
     * to create, alter or delete other database objects<BR>
     * <P>
     * @param script the script object that will be completed
     */
    public synchronized void getCreateDDLScript(DBSQLScript script)
    {
        DBMSHandler orgHandler = this.dbms;
        DBMSHandler ddlHandler = script.getContext().getDbms();
        try {
            // Set dbms
            if (this.dbms!=null && this.dbms!=ddlHandler && ddlHandler!=null)
            {   // The database belongs to a different dbms
                throw new InvalidOperationException("The database is attached to a different dbms.");
            }
            // Temporarily change dbms
            if (this.dbms== null)
                this.dbms = ddlHandler;
            // Get DDL Command
            generateDDLScript(script);
            
        } finally {
            // restore original handler
            this.dbms = orgHandler; 
        }
    }

    /**
     * Override this to change or add DDL commands
     * @param script
     */
    protected void generateDDLScript(DBSQLScript script)
    {
        this.dbms.getDDLScript(DDLActionType.CREATE, this, script); 
    }
    
    /**
     * @see org.apache.empire.db.DBObject#getDatabase()
     */
    @SuppressWarnings("unchecked")
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
        if (dbms != null)
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
        if (dbms != null)
            throw new PropertyReadOnlyException(linkName);
        // Set Link 
        this.linkName = linkName;
    }
    
    /**
     * Adds a full qualified object name including schema prefix
     * and database link postfix (if any).
     * to the string buffer supplied
     * 
     * @param sql the SQL Builder to which to append the qualified object name
     * @param name the object's name
     * @param quoteName use quotes or not. If null detectQuoteName() is called
     */
    public void appendQualifiedName(DBSQLBuilder sql, String name, Boolean quoteName)
    {
        // Schema
        if (schema != null)
        { // Add Schema
            sql.append(schema);
            sql.append(".");
        }
        // Check dbms
        if (dbms==null)
        {   // No dbms attached!
            log.warn("No dbms attached for appending qualified name {0}.", name);
            sql.append(name);
            return;
        }
        // Append the name
        dbms.appendObjectName(sql, name, quoteName);
        // Database Link
        if (linkName!=null)
        {   // Add Link
            sql.append(dbms.getSQLPhrase(DBSqlPhrase.SQL_DATABASE_LINK));
            sql.append(linkName);
        }
    }
    
    /**
     * Returns the java class type for a given dataType
     * @param expr the column expression for witch to return the java type
     * @return return the java class used for storing values of this dataType 
     */
    public Class<?> getColumnJavaType(DBColumnExpr expr)
    {
        switch(expr.getDataType())
        {
            case AUTOINC:
                return Long.class;
            case INTEGER:
            {   // Check Integer size
                DBColumn source = expr.getUpdateColumn();
                int size = (source!=null ? (int)source.getSize() : DBTable.INT_SIZE_BIG);
                if (size<=DBTable.INT_SIZE_SMALL)
                    return Short.class;
                if (size<=DBTable.INT_SIZE_MEDIUM)
                    return Integer.class;
                // Default to Long
                return Long.class;
            }
            case VARCHAR:
            case CLOB:
            case CHAR:
                return String.class;
            case DATE:
                return (legacyDate ? Date.class : LocalDate.class);
            case TIME:
                return (legacyDate ? Date.class : LocalTime.class);
            case DATETIME:
                return (legacyDate ? Date.class : LocalDateTime.class);
            case TIMESTAMP:
                return Timestamp.class;
            case FLOAT:
                return Double.class;
            case DECIMAL:
                return java.math.BigDecimal.class;
            case BOOL:
                return Boolean.class;
            case BLOB:
                return byte[].class;
            default:
                return Object.class;
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
     * Creates and returns a value object for the given string value.
     * 
     * @param value the String value
     * @return the new DBValueExpr object
     */
    public DBValueExpr getValueExpr(BigDecimal value)
    {
        return new DBValueExpr(this, value, DataType.DECIMAL);
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
        if (value instanceof DBValueExpr)
            return ((DBValueExpr)value); // No nesting
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
        if (table == null || table.db != this)
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
     * Removes a table from the list of database tables
     * @param table to remove
     */
    public void removeTable(DBTable table)
    {
        if (table==null || table.getDatabase()!=this)
            throw new InvalidArgumentException("table", table);
        // remove
        if (tables.contains(table))
            tables.remove(table);
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
     * @param relation the relation to remove
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
     * @return the list of relations.
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
        if (view == null || view.db != this)
            throw new InvalidArgumentException("view", view);
        if (views.contains(view) == true)
            throw new ItemExistsException(view.getName());
        // add now
        views.add(view);
    }

    /**
     * Removes a view from the list of database views
     * @param view to remove
     */
    public void removeView(DBView view)
    {
        if (view==null || view.getDatabase()!=this)
            throw new InvalidArgumentException("view", view);
        // remove
        if (views.contains(view))
            views.remove(view);
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
        return (dbms != null);
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
     * Deprecated instead use context.createCommand();
     * 
     * @return the command object.
     */
    public DBCommand createCommand()
    {
        checkOpen(); 
        // For compatiblity with 2.x dont use isPreparedStatementsEnabled() 
        return dbms.createCommand(false);
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
     * Checks whether the supplied value is valid for the given column.
     * If the type of the value supplied does not match the columns
     * data type the value will be checked for compatibility.
     * If the value is not valid a FieldIllegalValueException is thrown
     * 
     * @param column the column to check
     * @param value the checked to check for validity
     * @return the (possibly converted) value
     * 
     * @throws FieldValueException
     */
    protected Object validateValue(DBTableColumn column, Object value)
    {
        DataType type = column.getDataType();
        // Check for NULL
        if (ObjectUtils.isEmpty(value))
        {   // Null value   
            if (column.isRequired())
                throw new FieldNotNullException(column);
            // Null is allowed
            return null;
        }
        // Check for Column expression
        if (value instanceof DBColumnExpr)
        {   DataType funcType = ((DBColumnExpr)value).getDataType();
            if (!type.isCompatible(funcType))
            {   // Incompatible data types
                log.info("Incompatible data types in expression for column {} using function {}!", column.getName(), value.toString());
                throw new FieldIllegalValueException(column, String.valueOf(value));
            }
            // allowed
            return value; 
        }
        // Check for Command expression
        if (value instanceof DBCommandExpr)
        {   List<DBColumnExpr> exprList = ((DBCommandExpr)value).getSelectExpressions();
            if (exprList.size()!=1)
            {   // Incompatible data types
                log.info("Invalid command expression for column {} using command {}!", column.getName(), ((DBCommandExpr)value).getSelect());
                throw new FieldIllegalValueException(column, ((DBCommandExpr)value).getSelect());
            }
            // Compare types
            if (!type.isCompatible(exprList.get(0).getDataType()))
            {   // Incompatible data types
                log.info("Incompatible data types in expression for column {} using function {}!", column.getName(), value.toString());
                throw new FieldIllegalValueException(column, String.valueOf(value));
            }
            // allowed
            return value; 
        }
        // Is value valid
        switch (type)
        {
            case DATE:
                // Check for LocalDate
                if (value instanceof LocalDate)
                    break;
                if (value instanceof LocalDateTime)
                {   value = ((LocalDateTime)value).toLocalDate();
                    break;
                }
            case DATETIME:
            case TIMESTAMP:
                // Check whether value is a valid date/time value!
                if (!(value instanceof LocalDateTime) && !(value instanceof Date) && !DBDatabase.SYSDATE.equals(value))
                {   try {
                        // Parse Date
                        value = ObjectUtils.toDate(value);
                    } catch(ParseException e) {
                        log.info("Parsing '{}' to Date failed for column {}. Message is "+e.toString(), value, column.getName());
                        throw new FieldIllegalValueException(column, String.valueOf(value), e);
                    }
                }    
                break;

            case DECIMAL:
                // check enum
                if (value instanceof Enum<?>)
                {   // convert enum   
                    value = ((Enum<?>)value).ordinal();
                }
                // check number
                if (!(value instanceof java.lang.Number))
                {   try
                    {   // Convert to Decimal
                        value = ObjectUtils.toDecimal(value);
                        // throws NumberFormatException if not a number!
                    } catch (NumberFormatException e) {
                        log.info("Parsing '{}' to Decimal failed for column {}. Message is "+e.toString(), value, column.getName());
                        throw new FieldIllegalValueException(column, String.valueOf(value), e);
                    }
                }
                // validate Number
                value = validateNumber(column, type, (Number)value);
                break;

            case FLOAT:
                if (!(value instanceof java.lang.Number))
                {   try
                    {   // Convert to Double
                        value = ObjectUtils.toDouble(value);
                        // throws NumberFormatException if not a number!
                    } catch (NumberFormatException e) {
                        log.info("Parsing '{}' to Double failed for column {}. Message is "+e.toString(), value, column.getName());
                        throw new FieldIllegalValueException(column, String.valueOf(value), e);
                    }
                }
                // validate Number
                value = validateNumber(column, type, (Number)value);
                break;

            case INTEGER:
                // check enum
                if (value instanceof Enum<?>)
                {   // convert enum   
                    value = ((Enum<?>)value).ordinal();
                }
                // check number
                if (!(value instanceof java.lang.Number))
                {   try
                    {   // Convert to Long
                        value = ObjectUtils.toLong(value);
                    } catch (NumberFormatException e) {
                        log.info("Parsing '{}' to Integer failed for column {}. Message is "+e.toString(), value, column.getName());
                        throw new FieldIllegalValueException(column, String.valueOf(value), e);
                    }
                }
                // validate Number
                value = validateNumber(column, type, (Number)value);
                break;

            case VARCHAR:
            case CHAR:
                // check enum
                if (value instanceof Enum<?>)
                {   // convert enum   
                    value = ObjectUtils.getString((Enum<?>)value);
                }
                // check length
                if (value.toString().length() > (int)column.getSize())
                {
                    throw new FieldValueTooLongException(column);
                }
                break;
                
            default:
                if (log.isTraceEnabled())
                    log.trace("No column validation has been implemented for data type " + type);
                break;
        }
        return value;
    }
    
    protected Number validateNumber(DBTableColumn column, DataType type, Number n)
    {
        // Check Range
        Object min = column.getAttribute(Column.COLATTR_MINVALUE);
        Object max = column.getAttribute(Column.COLATTR_MAXVALUE);
        boolean belowMin = (min instanceof Number) ? ObjectUtils.compare(n, min)<0 : false;
        boolean aboveMax = (max instanceof Number) ? ObjectUtils.compare(n, max)>0 : false;
        if (belowMin && aboveMax)
        {   // Out of Range
            throw new FieldValueOutOfRangeException(column, (Number)min, (Number)max);
        }
        else if (belowMin)
        {   // Check Min Value
            throw new FieldValueOutOfRangeException(column, (Number)min, false);
        }
        else if (aboveMax)
        {   // Check Max Value
            throw new FieldValueOutOfRangeException(column, (Number)max, true);
        }
        // Check overall
        if (type==DataType.DECIMAL)
        {   // Convert to Decimal
            BigDecimal dv = ObjectUtils.toDecimal(n);
            int prec = dv.precision();
            int scale = dv.scale();
            // check precision and scale
            double size = column.getSize();
            int reqPrec = (int)size;
            int reqScale = column.getDecimalScale();
            if (scale>reqScale)
            {   // Round if scale is exceeded
                dv = dv.setScale(reqScale, RoundingMode.HALF_UP);
                prec  = dv.precision();
                scale = dv.scale();
                n = dv;
            }
            if ((prec-scale)>(reqPrec-reqScale))
            {   
                throw new FieldValueOutOfRangeException(column);
            }
        }
        return n;
    }
    
    /**
     * Creates a SQL case expression
     * in the form "case when [condition] then [trueValue] else [falseValue] end"
     * This is a helper function to simplify client usage
     * @param condition the compare expression
     * @param trueValue the value to select if the condition is true
     * @param falseValue the value to select if the condition is false
     * @return the case expression
     */
    public DBCaseExpr caseWhen(DBCompareExpr condition, Object trueValue, Object falseValue)
    {
        return new DBCaseWhenExpr(condition, trueValue, falseValue);
    }
    
    /**
     * Creates a SQL case expression
     * in the form "case when [mapKey] then [mapValue] else [elseValue] end"
     * @param whenMap the map with constraints
     * @param elseValue the else expression
     * @return the expression
     */
    public DBCaseExpr caseWhen(Map<DBCompareExpr, ? extends Object> whenMap, Object elseValue)
    {
        return new DBCaseWhenExpr(this, whenMap, elseValue);
    }

    /**
     * Creates a SQL case expression that check whether a column or column expression is null
     * "case when [condition] is null then [trueValue] else [falseValue] end"
     * This is a helper function to simplify client usage
     * @param column a column or column expression
     * @param trueValue the value to select if the condition is true
     * @param falseValue the value to select if the condition is false
     * @return an sql case expression
     */
    public DBCaseExpr caseWhenNull(DBColumnExpr column, Object trueValue, Object falseValue)
    {
        // return caseMap(column, null, trueValue, falseValue);
        return caseWhen(column.is(null), trueValue, falseValue);
    }
    
    /**
     * Creates a SQL case expression
     * in the form "case [Expr] when [mapKey] then [mapValue] else [elseValue] end"
     * @param valueMap map of key value pairs
     * @param elseValue the else expression
     * @return the expression
     */
    public DBCaseExpr caseMap(DBColumnExpr column, Map< ? extends Object,  ? extends Object> valueMap, Object elseValue)
    {
        return new DBCaseMapExpr(column, valueMap, elseValue);
    }
    
    /**
     * Creates a SQL case expression
     * in the form "case [Expr] when [optionValue] then [optionText] else [elseValue] end"
     * @param column a column or column expression
     * @param options the options to map
     * @param elseValue the else expression
     * @return the expression
     */
    public DBCaseExpr caseMap(DBColumnExpr column, Options options, Object elseValue)
    {
        return new DBCaseMapExpr(column, options.map(), elseValue);
    }
    
    /**
     * Creates a SQL case expression
     * in the form "case [Expr] when [compareValue] then [trueValue] else [elseValue] end"
     * @param column a column or column expression
     * @param cmpValue the value to compare the column value with
     * @param trueValue the true expression
     * @param falseValue the false expression
     * @return the expression
     */
    public DBCaseExpr caseMap(DBColumnExpr column, Object cmpValue, Object trueValue, Object falseValue)
    {
        return new DBCaseMapExpr(column, cmpValue, trueValue, falseValue);
    }

}