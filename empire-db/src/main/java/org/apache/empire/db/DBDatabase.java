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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.DataType;
import org.apache.empire.db.expr.column.DBValueExpr;


/**
 * This abstract class is the applicaton's interface for a particular database schema.
 * <P>
 * It provides access to the various database objects such als tables, views and relations.
 * <P>
 * It also provides methods to execute DQL and DML SQL-commands.
 * <P>
 *
 */
public abstract class DBDatabase extends DBObject
{
    /**
     * This class represents the database systems current date and time.
     * <P>
     * There is no need to use this class directly.<BR>
     * Instead you can use the constant {@link DBDatabase#SYSDATE}
     */
    public static final class DBSystemDate 
    {   
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
    private static final Log log = LogFactory.getLog(DBDatabase.class);

    /** the database schema * */
    protected String           schema    = null; // database schema name
    protected String           linkName  = null; // database link name
    protected List<DBTable>    tables    = new ArrayList<DBTable>();
    protected List<DBRelation> relations = new ArrayList<DBRelation>();
    protected List<DBView>     views     = new ArrayList<DBView>();
    protected DBDatabaseDriver driver    = null;

    /**
     * Constructs a new DBDatabase object set the variable 'schema' = null.
     */
    public DBDatabase()
    {
        this.schema = null;
    }

    /**
     * Constructs a new DBDatabase object and sets the specified schema object.
     * 
     * @param schema the database schema
     */
    public DBDatabase(String schema)
    {
        this.schema = schema;
    }

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
     * Sets the database driver for this database. This will
     * set up the connection for use.<br>
     * 
     * @param driver the databae driver
     * @param conn the connection
     * 
     * @return true on succes
     */
    public boolean open(DBDatabaseDriver driver, Connection conn)
    {
        // Close Database if already open
        if (isOpen())
            close(conn);
        // Attach to driver
        if (driver.attachDatabase(this, conn)==false)
            return error(driver);
        // set new driver
        this.driver = driver;
        return success();
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
     * 
     * @return the DLL script for creating the entire database schema
     */
    public synchronized boolean getCreateDDLScript(DBDatabaseDriver driver, DBSQLScript script)
    {
        DBDatabaseDriver prevDriver = this.driver;
        try {
            // Set driver
            if (this.driver!=null && this.driver!=driver && driver!=null)
            {   // The database belongs to a different driver
                return error(Errors.Internal, "The database is attached to a different driver.");
            }
            // Temporarily change driver
            if (this.driver== null)
                this.driver = driver;
            // Get DDL Command
            if (driver.getDDLScript(DBCmdType.CREATE, this, script)==false)
            {   // DDL-creation failed 
                return error(driver);
            }
            return success();
            
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
     * 
     * @return true on succes
     */
    public boolean setSchema(String schema)
    {   // Database must not be open so far
        if (driver != null)
            return error(Errors.NoAccess);
        // Set Schema 
        this.schema = schema;
        return success();
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
     * 
     * @return true on succes
     */
    public boolean setLinkName(String linkName)
    {   // Database must not be open so far
        if (driver != null)
            return error(Errors.NoAccess);
        // Set Link 
        this.linkName = linkName;
        return success();
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
     * to the string buffer suppield
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
            error(Errors.ObjectNotValid, name);
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
        return new DBValueExpr(this, new Integer(value), DataType.INTEGER);
    }

    /**
     * Creates and returns a value object for the given long value.
     * 
     * @param value the long value
     * @return the new DBValueExpr object
     */
    public DBValueExpr getValueExpr(long value)
    {
        return new DBValueExpr(this, new Long(value), DataType.INTEGER);
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
     * There is usually no need to call this function directly 
     * since it is internally called from the DBTable's constructor.
     * <P> 
     * @param table the DBTable object
     * @return true if successful
     */
    public boolean addTable(DBTable table)
    { // find column by name
        if (table == null || table.getDatabase() != this)
            return error(Errors.InvalidArg, table, "table");
        if (tables.contains(table)==true)
            return error(Errors.ItemExists, table.getName());
        // Check for second instances
        DBTable existing = getTable(table.getName()); 
        if (existing!=null)
        {   // Check classes
            if (existing.getClass().equals(table.getClass()))
                return success(); // Ingore other instances 
            // Table exists with different class
            return error(Errors.ItemExists, table.getName());
        }
        // add now
        tables.add(table);
        return true;
    }

    /**
     * Returns the tables which has been defined in the database.
     * 
     * @return db tables.
     */
    public List<DBTable> getTables()
    {
        return tables;
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
     * @return true if the relations was successfully created.
     */
    public final boolean addRelation(DBRelation.DBReference reference)
    {
        String table = reference.getSourceColumn().getRowSet().getName();
        String col1 = reference.getSourceColumn().getName();
        // Create Relation Name
        String name = table.substring(0, Math.min(table.length(), 14)) + "_" + col1.substring(0, Math.min(col1.length(), 12))
        			  + "_FK";
        return addReleation(name, new DBRelation.DBReference[] { reference });
    }

    /**
     * Add a foreign key relation to the database.
     * 
     * @param ref1 a reference for a source and target column pair
     * @param ref2 a reference for a source and target column pair
     * @return true if the relations was successfully created.
     */
    public final boolean addRelation(DBRelation.DBReference ref1, DBRelation.DBReference ref2)
    {
        String table = ref1.getSourceColumn().getRowSet().getName();
        String col1 = ref1.getSourceColumn().getName();
        String col2 = ref2.getSourceColumn().getName();
        // Create Relation Name
        String name = table.substring(0, Math.min(table.length(), 9))
                    + "_" + col1.substring(0, Math.min(col1.length(), 9))
                    + "_" + col2.substring(0, Math.min(col2.length(), 9)) + "_FK";
        return addReleation(name, new DBRelation.DBReference[] { ref1, ref2 });
    }

    /**
     * Adds a foreign key relation to the database.
     * 
     * @param name the relation name
     * @param references a list of source and target column pairs
     * 
     * @return true if the relations was successfully created.
     */
    public boolean addReleation(String name, DBRelation.DBReference[] references)
    {
        // Add a Relation
        DBRelation relation = new DBRelation(this, name, references);
        if (relations.contains(relation))
            return error(Errors.ItemExists, name); // Itemn already exists
        // Add Reference column to table
        for (DBRelation.DBReference ref : references)
        {   // add the reference column
            DBRowSet rset = ref.getSourceColumn().getRowSet();
            rset.addColumnReference(ref.getSourceColumn(), ref.getTargetColumn());
        }
        // OK
        relations.add(relation);
        return true;
    }

    /**
     * Returns the tables which has been defined in the database.
     * 
     * @return db tables.
     */
    public List<DBRelation> getRelations()
    {
        return relations;
    }

    /**
     * Adds a DBView object to list of database views.<BR>
     * There is usually no need to call this function directly 
     * since it is internally called from the DBView's constructor.
     * <P> 
     * @param view the DBView object
     * @return true if successful
     */
    public boolean addView(DBView view)
    { // find column by name
        if (view == null || view.getDatabase() != this)
            return error(Errors.InvalidArg, view, "view");
        if (views.contains(view) == true)
            return error(Errors.ItemExists, view.getName());
        // add now
        views.add(view);
        return true;
    }

    /**
     * Returns the tables which has been defined in the database.
     * 
     * @return db tables.
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
     * @return true if the database has been opened or false otherwise 
     */
    protected boolean checkOpen()
    {
        if (driver == null)
            return error(DBErrors.DatabaseNotOpen);
        return success();
    }
    
    /**
     * Creates a new Command object for this database
     * 
     * @return the command obejct.
     */
    public DBCommand createCommand()
    {
        if (checkOpen()==false)
            return null;
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
        // Default implementation
        if (checkOpen()==false)
            return null;
        // Ask driver
        return driver.getUpdateTimestamp(conn);
    }

    // Sequences
    public Object getNextSequenceValue(String seqName, Connection conn)
    {
        // Default implementation
        if (checkOpen()==false)
            return null;
        // Ask driver
        return driver.getNextSequenceValue(this, seqName, 1, conn);
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
        ResultSet rs = null;
        try
        {	// Check Open
            if (checkOpen()==false)
                return null;
            // Debug
            long start = System.currentTimeMillis();
            if (log.isDebugEnabled())
                log.debug("executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, null, false, conn);
            if (rs == null)
            { // Error
                error(driver);
                return null;
            }
            // Check Result
            if (rs.next() == false)
            {
                //log.warn("querySingleValue returned no result : Stack", new Exception("Just to show the stack"));
                log.debug("querySingleValue returned no result");
                error(DBErrors.QueryNoResult, sqlCmd);
                return null;
            }
            // No Value
            clearError();
            Object result = rs.getObject(1);
            if (log.isDebugEnabled())
	            log.debug("querySingleValue complete in " + String.valueOf(System.currentTimeMillis() - start) + " ms -> value="
	                        + String.valueOf(result));
            return result;
        } catch (SQLException e) 
        {
            log.error("querySingleValue exception: " + e.toString());
            error(DBErrors.QueryFailed, e);
            return null;
        } finally
        { // Cleanup
            closeResultSet(rs);
        }
    }

    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * 
     * @param sqlCmd the SQL statement
     * @param defVal the default value if no value was returned by the database
     * @param conn a valid connection to the database.
     * @return the result as a int value, if no result the int value 0
     */
    public int querySingleInt(String sqlCmd, int defVal, Connection conn)
    { 
        Object value = querySingleValue(sqlCmd, conn);
        return ((value != null) ? Integer.parseInt(value.toString()) : defVal);
    }

    /**
     * Returns the value of the first row/column of a sql-query as an int.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
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
     * @return the result as a long value, if no result the long value 0
     */
    public final long querySingleLong(String sqlCmd, Connection conn)
    { 
        return querySingleLong(sqlCmd, 0, conn);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a double.
     * 
     * @param sqlCmd the SQL statement
     * @param defVal the default value
     * @param conn a valid connection to the database.
     * 
     * @return the result as a long value, if no result the long value 0
     */
    public double querySingleDouble(String sqlCmd, double defVal, Connection conn)
    { 
        Object value = querySingleValue(sqlCmd, conn);
        return ((value != null) ? Double.parseDouble(value.toString()) : defVal);
    }

    /**
     * Returns the value of the first row/column of a sql-query as a double.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * 
     * @return the result as a long value, if no result the long value 0
     */
    public final double querySingleDouble(String sqlCmd, Connection conn)
    { 
        return querySingleDouble(sqlCmd, 0.0, conn);
    }
    
    /**
     * Returns the value of the first row/column of a sql-query as a string.
     * 
     * @param sqlCmd the SQL statement
     * @param defVal the default value if no value was returned by the database
     * @param conn a valid connection to the database.
     * @return the result as a String object, if no result a emtpy String
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
     * @return the result as a String object, if no result a emtpy String
     */
    public final String querySingleString(String sqlCmd, Connection conn)
    { 
        return querySingleString(sqlCmd, "", conn);
    }
    
    /**
     * Returns a one dimensional array from an sql query.
     * The array is filled with the values of the first column.
     * 
     * @param c the class type for the list 
     * @param <T> the type for th list
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * 
     * @return a list of the values of the first column of an sql query 
     */
    public <T> List<T> querySimpleList(Class<T> c, String sqlCmd, Connection conn)
    {   // Execute the  Statement
        if (checkOpen()==false)
            return null;
        // Debug
        ResultSet rs = null;
        try
        {	// Check Open
            if (checkOpen()==false)
                return null;
            // Debug
            long start = System.currentTimeMillis();
            if (log.isInfoEnabled())
                log.info("executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, null, false, conn);
            if (rs == null)
            { // Error
                error(driver);
                return null;
            }
            // Check Result
            List<T> result = new ArrayList<T>();
            while (rs.next())
            {
                T item = ObjectUtils.convert(c, rs.getObject(1));
                result.add(item);
            }
            // No Value
            if (log.isInfoEnabled())
                log.info("querySimpleList retured "+String.valueOf(result.size())+" items. Query completed in " + String.valueOf(System.currentTimeMillis() - start) + " ms");
            clearError();
            return result;
        } catch (ClassCastException e) 
        {   
            log.error("querySingleValue cast exception: ", e);
            error(Errors.Exception, e);
            return null;
        } catch (SQLException e) 
        {
            log.error("querySimpleList exception: ", e);
            error(DBErrors.QueryFailed, e);
            return null;
        } finally
        { // Cleanup
            closeResultSet(rs);
        }
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
     * Returns a list of key value pairs from an sql query.
     * The opton list is filled with the values of the first and second column.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * @return an Options object containing a set a of values and their corresponding names 
     */
    public Options queryOptionList(String sqlCmd, Connection conn)
    {   // Execute the  Statement
        if (checkOpen()==false)
            return null;
        // Debug
        ResultSet rs = null;
        try
        {   // Check Open
            if (checkOpen()==false)
                return null;
            // Debug
            long start = System.currentTimeMillis();
            if (log.isInfoEnabled())
                log.info("executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, null, false, conn);
            if (rs == null)
            { // Error
                error(driver);
                return null;
            }
            if (rs.getMetaData().getColumnCount()<2)
            {   // Not enough columns
                error(Errors.InvalidArg, sqlCmd, "sqlCmd");
                return null;
            }
            // Check Result
            Options result = new Options();
            while (rs.next())
            {
                Object value = rs.getObject(1);
                String text  = rs.getString(2);
                result.add(value, text, true);
            }
            // No Value
            if (log.isInfoEnabled())
                log.info("queryOptionList retured "+String.valueOf(result.size())+" items. Query completed in " + String.valueOf(System.currentTimeMillis() - start) + " ms");
            clearError();
            return result;
        } catch (SQLException e) 
        {
            error(DBErrors.QueryFailed, e);
            return null;
        } finally
        { // Cleanup
            closeResultSet(rs);
        }
    }

    /**
     * Returns a list Object-Arrays holding the result of a query.
     * This function should only be used for small lists.
     * 
     * @param sqlCmd the SQL statement
     * @param conn a valid connection to the database.
     * @return a list of object arrays 
     */
    public List<Object[]> queryObjectList(String sqlCmd, Connection conn)
    {   // Execute the  Statement
        if (checkOpen()==false)
            return null;
        // Debug
        ResultSet rs = null;
        try
        {   // Check Open
            if (checkOpen()==false)
                return null;
            // Debug
            long start = System.currentTimeMillis();
            if (log.isInfoEnabled())
                log.info("executing: " + sqlCmd);
            // Get the next Value
            rs = driver.executeQuery(sqlCmd, null, false, conn);
            if (rs == null)
            { // Error
                error(driver);
                return null;
            }
            // Read List
            int colCount = rs.getMetaData().getColumnCount(); 
            List<Object[]> result = new ArrayList<Object[]>();
            while (rs.next())
            {
                Object[] item = new Object[colCount];
                for (int i=0; i<colCount; i++)
                {   // Read from Resultset
                    item[i] = rs.getObject(i+1);
                }
                result.add(item);
            }
            // No Value
            if (log.isInfoEnabled())
                log.info("queryObjectList retured "+String.valueOf(result.size())+" items. Query completed in " + String.valueOf(System.currentTimeMillis() - start) + " ms");
            clearError();
            return result;
        } catch (SQLException e) 
        {
            error(DBErrors.QueryFailed, e);
            return null;
        } finally
        { // Cleanup
            closeResultSet(rs);
        }
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
        try 
        {
            // check driver
            if (checkOpen()==false)
                return -1;
            // Debug
            if (log.isInfoEnabled())
                log.info("Executing: " + sqlCmd);
            // execute SQL
            long start = System.currentTimeMillis();
            int affected = driver.executeSQL(sqlCmd, sqlParams, conn, setGenKeys);
            // Log
            if (log.isInfoEnabled())
	            log.info("executeSQL affected " + String.valueOf(affected) + " Records / " + (System.currentTimeMillis() - start) + "ms");
            // number of affected records
            if (affected < 0)
            {
                error(driver);
                return -1;
            }
            // Return number of affected records
            return affected;
            
	    } catch (SQLException sqle) 
        { 	// Error
	        error(sqle);
	        return -1;
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
     * Executes a select SQL-Statement and returns a resulset containing the query results.<BR>
     * This function returns a JDBC ResultSet.<BR>
     * Insteam of using this function directly you should use a DBReader object instead.<BR>
     * <P>
     * @param sqlCmd the SQL-Command
     * @param sqlParams a list of parameters for parameter queries (may depend on driver)
     * @param scrollable true if the reader should be scrollable or false if not
     * @param conn a valid connection to the database.
     * @return the JDBC ResutSet
     */
    public ResultSet executeQuery(String sqlCmd, Object[] sqlParams, boolean scrollable, Connection conn)
    {
        try
        {   // check driver
            if (checkOpen()==false)
                return null;
            // Debug
            if (log.isDebugEnabled())
    	        log.debug("Executing: " + sqlCmd);
            // Execute the Statement
            long start = System.currentTimeMillis();
            ResultSet rs = driver.executeQuery(sqlCmd, sqlParams, scrollable, conn);
            if (rs == null)
            {   error(driver);
                return null;
            }
            // Debug
            if (log.isDebugEnabled())
                log.debug("executeQuery successful in " + String.valueOf(System.currentTimeMillis() - start) + " ms");
            // Return number of affected records
            success();
            return rs;

        } catch (SQLException e) 
        {   // SQL Query failed!
            log.error("execute query exception! sql = " + sqlCmd);
            error(DBErrors.QueryFailed, e);
            return null; 
        }
    }

    /**
     * Makes all changes made since the previous commit/rollback
     * permanent and releases any database locks currently held by the
     * Connection.
     * 
     * @param conn a valid database connection
     * 
     * @return true if successful
     */
    public boolean commit(Connection conn)
    {
        try
        {   // Check arguement
            if (conn==null)
                return error(Errors.InvalidArg, null, "conn");
            // Commit
            conn.commit();
            return true;
        } catch (SQLException sqle) 
        { 
            // Commit failed!
            return error(sqle);
        }
    }

    /**
     * Discards all changes made since the previous commit/rollback
     * and releases any database locks currently held by this
     * Connection.
     * <P>
     * @param conn a valid database connection
     * 
     * @return true if successful
     */
    public boolean rollback(Connection conn)
    {
        try
        {   // Check arguement
            if (conn==null)
                return error(Errors.InvalidArg, null, "conn");
            // Rollback
            log.info("Database rollback issued!");
            conn.rollback();
            return true;
        } catch (SQLException sqle) 
        {
            return error(sqle);
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
            success();
        } catch (SQLException sqle)
        {
            error(sqle);
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
            // close Resutlset
            Statement stmt = rset.getStatement();
            rset.close();
            // check Statement
            if (stmt == null)
                return;
            // close Statement
            stmt.close();
            success();
            
        } catch (SQLException sqle) 
        {
            error(sqle);
        }
    }

}