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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.beanutils.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * <P>
 * This class is used to perform database queries from a DBCommand object and access the results.<BR>
 * In oder to perform a query call the open() function or - for single row queries - call getRecordData();<BR>
 * You can iterate through the rows using moveNext() or an iterator.<BR>
 * <P>
 * However take care: A reader must always be explcitly closed using the close() method!<BR>
 * Otherwise you may lock the JDBC connection and run out of resources.<BR>
 * Use <PRE>try { ... } finally { reader.close(); } </PRE> to make sure the reader is closed.<BR>
 * <P>
 * To access and work with the query result you can do one of the following:<BR>
 * <ul>
 *  <li>access field values directly by using one of the get... functions (see {@link DBRecordData})</li> 
 *  <li>get the rows as a list of Java Beans using by using {@link DBReader#getBeanList(Class, int)}</li> 
 *  <li>get the rows as an XML-Document using {@link DBReader#getXmlDocument()} </li> 
 *  <li>initialize a DBRecord with the current row data using {@link DBReader#initRecord(DBRowSet, DBRecord)}<br/>
 *      This will allow you to modify and update the data. 
 *  </li> 
 * </ul>
 *
 *
 */
public class DBReader extends DBRecordData
{
    public abstract class DBReaderIterator implements Iterator<DBRecordData>
    {
        protected int curCount = 0;
        protected int maxCount = 0;

        public DBReaderIterator(int maxCount)
        {
            if (maxCount < 0)
                maxCount = 0x7FFFFFFF; // Highest positive number
            // Set Maxcount
            this.maxCount = maxCount;
        }

        /**
         * Implements the Iterator Interface Method remove not implemented and not applicable.
         */
        public void remove()
        {
            log.error("DBReader.remove ist not implemented!");
        }

        /**
         * Disposes the iterator.
         */
        public void dispose()
        {
            curCount = maxCount = -1;
        }
    }

    /**
     * This is an iterator for scrolling resultsets.
     * This iterator has no such limitations as the forward iterator.
     */
    public class DBReaderScrollableIterator extends DBReaderIterator
    {
        public DBReaderScrollableIterator(int maxCount)
        {
            super(maxCount);
        }

        /**
         * Implements the Iterator Interface.
         * 
         * @return true if there is another record to read
         */
        public boolean hasNext()
        {
            try
            { // clear previous error
                clearError();
                // Check position
                if (curCount >= maxCount)
                    return false;
                // Check Recordset
                if (rset == null || rset.isLast() || rset.isAfterLast())
                    return false;
                // there are more records
                return true;
            } catch (SQLException e)
            {
                return error(e);
            }
        }

        /**
         * Implements the Iterator Interface.
         * 
         * @return the current Record interface
         */
        public DBRecordData next()
        {
            if ((curCount < maxCount && moveNext()))
            {
                curCount++;
                return DBReader.this;
            }
            // Past the end!
            return null;
        }
    }

    /**
     * This is an iterator for forward only resultsets.
     * There is an important limitation on this iterator: After calling
     * hasNext() the caller may not use any functions on the current item any more. i.e.
     * Example:
     *  while (i.hasNext())
     *  {
     *      DBRecordData r = i.next(); 
     *      Object o  = r.getValue(0);  // ok
     *      
     *      bool last = i.hasNext();    // ok
     *      Object o  = r.getValue(0);  // Illegal call!
     *  }
     */
    public class DBReaderForwardIterator extends DBReaderIterator
    {
        private boolean getCurrent = true;
        private boolean hasCurrent = false;

        public DBReaderForwardIterator(int maxCount)
        {
            super(maxCount);
        }

        /**
         * Implements the Iterator Interface.
         * 
         * @return true if there is another record to read
         */
        public boolean hasNext()
        {
            // Check position
            if (curCount >= maxCount)
                return false;
            if (rset == null)
                return error(Errors.ObjectNotValid, getClass().getName());
            // Check next Record
            if (getCurrent == true)
            {
                getCurrent = false;
                hasCurrent = moveNext();
            }
            return hasCurrent;
        }

        /**
         * Implements the Iterator Interface.
         * 
         * @return the current Record interface
         */
        public DBRecordData next()
        {
            if (hasCurrent == false)
                return null; // Past the end!
            // next called without call to hasNext ?
            if (getCurrent && !moveNext())
            { // No more records
                hasCurrent = false;
                getCurrent = false;
                return null;
            }
            // Move forward
            curCount++;
            getCurrent = true;
            return DBReader.this;
        }
    }

    // Logger
    @SuppressWarnings("hiding")
    protected static final Logger    log               = LoggerFactory.getLogger(DBReader.class);
    
    /**
     * Support for finding code errors where a DBRecordSet is opened but not closed
     * @author bond
     */
    private static ThreadLocal<Map<DBReader, Exception>> threadLocalOpenResultSets = new ThreadLocal<Map<DBReader, Exception>>();
    
    // Object references
    private DBDatabase     db                = null;
    private DBColumnExpr[] colList           = null;

    // Direct column access
    protected ResultSet    rset              = null;

    /**
     * Constructs an empty DBRecordSet object.
     */
    public DBReader()
    {
        // Default Constructor
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return db;
    }
    
    public boolean getScrollable()
    {
        try
        {
            // Check Resultset
            return (rset!=null && rset.getType()!=ResultSet.TYPE_FORWARD_ONLY); 
        } catch (SQLException e)
        {
            log.error("Cannot determine Resultset type", e);
            return false;
        }
    }

    /**
     * Returns the index value by a specified DBColumnExpr object.
     * 
     * @return the index value
     */
    @Override
    public int getFieldIndex(ColumnExpr column)
    {
        if (colList != null)
        {
            // First chance: Try to find an exact match
            for (int i = 0; i < colList.length; i++)
            {
                if (colList[i].equals(column))
                    return i;
            }
            // Second chance: Try Update Column
            if (column instanceof DBColumn)
            {
                for (int i = 0; i < colList.length; i++)
                {
                    DBColumn updColumn = colList[i].getUpdateColumn();
                    if (updColumn!=null && updColumn.equals(column))
                        return i;
                }
            }
        }
        return -1;
    }

    /** Get the column Expression at position */
    @Override
    public DBColumnExpr getColumnExpr(int iColumn)
    {
        if (colList == null || iColumn < 0 || iColumn >= colList.length)
            return null; // Index out of range
        // return column Expression
        return colList[iColumn];
    }

    /**
     * Returns the index value by a specified column name.
     * 
     * @param column the column name
     * @return the index value
     */
    @Override
    public int getFieldIndex(String column)
    {
        if (colList != null)
        {
            for (int i = 0; i < colList.length; i++)
                if (colList[i].getName().equalsIgnoreCase(column))
                    return i;
        }
        // not found
        return -1;
    }

    /**
     * Checks wehter a column value is null Unlike the base
     * class implementation, this class directly check the value fromt the
     * resultset.
     * 
     * @param index index of the column
     * @return true if the value is null or false otherwise
     */
    @Override
    public boolean isNull(int index)
    {
        if (index < 0 || index >= colList.length)
        { // Index out of range
            log.error("Index out of range: " + index);
            return true;
        }
        try
        { // Check Value on Resultset
            clearError();
            rset.getObject(index + 1);
            return rset.wasNull();
        } catch (Exception e)
        {
            log.error("isNullValue exception", e);
            return super.isNull(index);
        }
    }

    /**
     * Returns a data value identified by the column index.
     * 
     * @param index index of the column
     * @return the value
     */
    @Override
    public Object getValue(int index)
    {
        if (index < 0 || index >= colList.length)
        { // Index out of range
            error(Errors.OutOfRange, index);
            return null;
        }
        try
        { // Get Value from Resultset
            clearError();
            DataType dataType = colList[index].getDataType();
            return db.driver.getResultValue(rset, index + 1, dataType);

        } catch (Exception e)
        { // Operation failed
            error(e);
            return null;
        }
    }

    /** 
     * Checks if the rowset is open
     *  
     * @return true if the rowset is open
     */
    public boolean isOpen()
    {
        return (rset != null);
    }

    /**
     * Opens the reader by executing the given SQL command.<BR>
     * After the reader is open, the reader's position is before the first record.<BR>
     * Use moveNext or iterator() to step through the rows.<BR>
     * Data of the current row can be accessed through the functions on the RecordData interface.<BR>
     * <P>
     * ATTENTION: After using the reader it must be closed using the close() method!<BR>
     * Use <PRE>try { ... } finally { reader.close(); } </PRE> to make sure the reader is closed.<BR>
     * <P>
     * @param cmd the SQL-Command with cmd.getSelect()
     * @param scrollable true if the reader should be scrollable or false if not
     * @param conn a valid JDBC connection.
     * @return true if successful
     */
    public boolean open(DBCommandExpr cmd, boolean scrollable, Connection conn)
    {
        if (isOpen())
            close();
        // SQL Command
        String sqlCmd = cmd.getSelect();
        // Create Statement
        db = cmd.getDatabase();
        rset = db.executeQuery(sqlCmd, cmd.getParamValues(), scrollable, conn);
        if (rset==null)
            return error(db);
        // successfully opened
        colList = cmd.getSelectExprList();
        addOpenResultSet();
        return success();
    }

    /**
     * Opens the reader by executing the given SQL command.<BR>
     * <P>
     * see {@link DBReader#open(DBCommandExpr, boolean, Connection)}
     * </P>
     * @param cmd the SQL-Command with cmd.getSelect()
     * @param conn a valid JDBC connection.
     * @return true if successful
     */
    public boolean open(DBCommandExpr cmd, Connection conn)
    {
        return open(cmd, false, conn);
    }

    /**
     * <P>
     * Opens the reader by executing the given SQL command and moves to the first row.<BR>
     * If true is returned data of the row can be accessed through the functions on the RecordData interface.<BR>
     * This function is intended for single row queries and provided for convenience.<BR>
     * However it behaves exacly as calling reader.open() and reader.moveNext()<BR>
     * <P>
     * ATTENTION: After using the reader it must be closed using the close() method!<BR>
     * Use <PRE>try { ... } finally { reader.close(); } </PRE> to make sure the reader is closed.<BR>
     * <P>
     * @param cmd the SQL-Command with cmd.getSelect()
     * @param conn a valid JDBC connection.
     * @return true if successful
     */
    public boolean getRecordData(DBCommandExpr cmd, Connection conn)
    { // Open the record
        if (!open(cmd, conn))
            return false;
        // Get First Record
        if (!moveNext())
        { // Close
            return error(DBErrors.QueryNoResult, cmd.getSelect());
        }
        return success();
    }

    /**
     * Closes the DBRecordSet object, the Statement object and detach the columns.<BR>
     * A reader must always be closed immediately after using it.
     */
    @Override
    public void close()
    {
        try
        { // Dispose iterator
            if (iterator != null)
            {
                iterator.dispose();
                iterator = null;
            }
            // Close Recordset
            if (rset != null)
            {
                getDatabase().closeResultSet(rset);
                removeOpenResultSet();
            }
            // Detach columns
            colList = null;
            rset = null;
            // Done
        } catch (Exception e)
        { // What's wrong here?
            log.warn(e.toString());
        }
    }

    /**
     * Moves the cursor down the given number of rows.
     * 
     * @param count the number of rows to skip 
     * 
     * @return true if the reader is on a valid record or false otherwise
     */
    public boolean skipRows(int count)
    {
        try
        { // clear previous error
            clearError();
            // Check Recordset
            if (rset == null)
                return error(Errors.ObjectNotValid, getClass().getName());
            // Forward only cursor?
            int type = rset.getType();
            if (type == ResultSet.TYPE_FORWARD_ONLY)
            {
                if (count < 0)
                    return error(Errors.InvalidArg, count, "count");
                // Move
                for (; count > 0; count--)
                {
                    if (!moveNext())
                        return false;
                }
                return true;
            }
            // Scrollable Cursor
            if (count > 0)
            { // Move a single record first
                if (rset.next() == false)
                    return false;
                // Move relative
                if (count > 1)
                    return rset.relative(count - 1);
            } 
            else if (count < 0)
            { // Move a single record first
                if (rset.previous() == false)
                    return false;
                // Move relative
                if (count < -1)
                    return rset.relative(count + 1);
            }
            return true;

        } catch (SQLException e)
        { // an error ocurred
            return error(e);
        }
    }

    /**
     * Moves the cursor down one row from its current position.
     * 
     * @return true if the reader is on a valid record or false otherwise
     */
    public boolean moveNext()
    {
        try
        { // clear previous error
            clearError();
            // Check Recordset
            if (rset == null)
                return error(Errors.ObjectNotValid, getClass().getName());
            // Move Next
            if (rset.next() == false)
            { // Close recordset automatically after last record
                close();
                clearError();
                return false;
            }
            return true;

        } catch (SQLException e)
        { // an error ocurred
            return error(e);
        }
    }

    private DBReaderIterator iterator = null; // there can only be one!

    /**
     * Returns an row iterator for this reader.<BR>
     * There can only be one iterator at a time.
     * <P>
     * @param maxCount the maximum number of item that shold be returned by this iterator
     * @return the row interator
     */
    public Iterator<DBRecordData> iterator(int maxCount)
    {
        if (iterator == null && rset != null)
        {
            if (getScrollable())
                iterator = new DBReaderScrollableIterator(maxCount);
            else
                iterator = new DBReaderForwardIterator(maxCount);
        }
        return iterator;
    }

    /**
     * <PRE>
     * Returns an row iterator for this reader.
     * There can only be one iterator at a time.
     * </PRE>
     * @return the row interator
     */
    public final Iterator<DBRecordData> iterator()
    {
        return iterator(-1);
    }

    /**
     * <PRE>
     * initializes a DBRecord object with the values of the current row.
     * At least all primary key columns of the target rowset must be provided by this reader.
     * This function is equivalent to calling rowset.initRecord(rec, reader) 
     * set also {@link DBRowSet#initRecord(DBRecord, DBRecordData)});
     * </PRE>
     * @param rowset the rowset to which to attach
     * @param rec the record which to initialize
     * @return true if the record has been initialized sucessfully or false otherwise
     */
    public boolean initRecord(DBRowSet rowset, DBRecord rec)
    {
    	if (rowset==null)
    		return error(Errors.InvalidArg, rowset, "rowset");
    	if (rowset.initRecord(rec, this)==false)
    		return error(rowset);
    	return success();
    }

    /**
     * Returns the result of a query as a list of objects restricted
     * to a maximum number of objects (unless maxCount is -1).
     * 
     * @param c the collection to add the objects to
     * @param t the class type of the objects in the list
     * @param maxCount the maximum number of objects
     * 
     * @return the list of <T>
     */
    @SuppressWarnings("unchecked")
    public <C extends Collection<T>, T> C getBeanList(C c, Class<T> t, int maxCount)
    {
        // Check Recordset
        if (rset == null)
        {   // Resultset not available
            error(Errors.ObjectNotValid, getClass().getName());
            return null;
        }
        // Query List
        try
        {
            // Check whether we can use a constructor
            Class[] paramTypes = new Class[getFieldCount()];
            for (int i = 0; i < colList.length; i++)
                paramTypes[i] = DBExpr.getValueClass(colList[i].getDataType()); 
            // Find Constructor
            Constructor ctor = findMatchingAccessibleConstructor(t, paramTypes);
            Object[] args = (ctor!=null) ? new Object[getFieldCount()] : null; 
            
            // Create a list of beans
            while (moveNext() && maxCount != 0)
            { // Create bean an init
                if (ctor!=null)
                {   // Use Constructor
                    for (int i = 0; i < getFieldCount(); i++)
                        args[i] = getValue(i);
                    T bean = (T)ctor.newInstance(args);
                    c.add(bean);
                }
                else
                {   // Use Property Setters
                    T bean = t.newInstance();
                    if (getBeanProperties(bean)==false)
                        return null;
                    c.add(bean);
                }
                // Decrease count
                if (maxCount > 0)
                    maxCount--;
            }
            // done
            return c;
        } catch (InvocationTargetException e)
        {
            error(e);
            return null;
        } catch (IllegalAccessException e)
        {
            error(e);
            return null;
        } catch (InstantiationException e)
        {
            error(e);
            return null;
        }
    }
    
    /**
     * Returns the result of a query as a list of objects.
     * 
     * @param t the class type of the objects in the list
     * @param maxItems the maximum number of objects
     * 
     * @return the list of <T>
     */
    public final <T> ArrayList<T> getBeanList(Class<T> t, int maxItems) {
        return getBeanList(new ArrayList<T>(), t, maxItems);
    }
    
    /**
     * Returns the result of a query as a list of objects.
     * 
     * @param t the class type of the objects in the list
     * 
     * @return the list of <T>
     */
    public final <T> ArrayList<T> getBeanList(Class<T> t) {
        return getBeanList(t, -1);
    }
    
    /**
     * Moves the cursor down one row from its current position.
     * 
     * @return true if successful
     */
    @Override
    public boolean addColumnDesc(Element parent)
    {
        if (colList == null)
            return error(Errors.ObjectNotValid, getClass().getName());
        // Add Field Description
        for (int i = 0; i < colList.length; i++)
            colList[i].addXml(parent, 0);
        return success();
    }

    /**
     * Adds all children to a parent.
     * 
     * @param parent the parent element below which to search the child
     * @return true if successful
     */
    @Override
    public boolean addRowValues(Element parent)
    {
        if (rset == null)
            return error(Errors.ObjectNotValid, getClass().getName());
        // Add all children
        for (int i = 0; i < colList.length; i++)
        { // Read all
            String name = colList[i].getName();
            String idColumnAttr = getXmlDictionary().getRowIdColumnAttribute();
            if (name.equalsIgnoreCase("id"))
            { // Add Attribute
                parent.setAttribute(idColumnAttr, getString(i));
            } 
            else
            { // Add Element
                String value = getString(i);
                Element elem = XMLUtil.addElement(parent, name, value);
                if (value == null)
                    elem.setAttribute("null", "yes"); // Null-Value
            }
        }
        return success();
    }

    /**
     * Adds all children to a parent.
     * 
     * @param parent the parent element below which to search the child
     * @return true if successful
     */
    public int addRows(Element parent)
    {
        int count = 0;
        if (rset == null)
            return 0;
        // Add all rows
        String rowElementName = getXmlDictionary().getRowElementName();
        while (moveNext())
        {
            addRowValues(XMLUtil.addElement(parent, rowElementName));
            count++;
        }
        return count;
    }
    
    /**
     * returns the DBXmlDictionary that should used to generate XMLDocuments<BR>
     * @return the DBXmlDictionary
     */
    protected DBXmlDictionary getXmlDictionary()
    {
        return DBXmlDictionary.getInstance();
    }

    /**
     * Returns a XML document with the field descriptiona an values of this record.
     * 
     * @return the new XML Document object
     */
    @Override
    public Document getXmlDocument()
    {
        if (rset == null)
            return null;
        // Create Document
        String rowsetElementName = getXmlDictionary().getRowSetElementName();
        Element root = XMLUtil.createDocument(rowsetElementName);
        // Add Field Description
        if (!addColumnDesc(root))
            return null;
        // Add row rset
        addRows(root);
        // return Document
        return root.getOwnerDocument();
    }

    /** returns the number of the elements of the colList array */
    @Override
    public int getFieldCount()
    {
        return (colList != null) ? colList.length : 0;
    }

    /**
     * Support for finding code errors where a DBRecordSet is opened but not closed.
     * 
     * @author bond
     */
    private synchronized void addOpenResultSet()
    {
        // add this to the vector of open resultsets on this thread
        Map<DBReader, Exception> openResultSets = threadLocalOpenResultSets.get();
        if (openResultSets == null)
        {
            // Lazy initialization of the
            openResultSets = new HashMap<DBReader, Exception>(2);
            threadLocalOpenResultSets.set(openResultSets);
        }

        Exception stackException = openResultSets.get(this);
        if (stackException != null)
        {
            log
               .error(
                      "DBRecordSet.addOpenResultSet called for an object which is already in the open list. This is the stack of the method opening the object which was not previously closed.",
                      stackException);
            // the code continues and overwrites the logged object with the new one
        }
        // get the current stack trace
        openResultSets.put(this, new Exception());
    }

    /**
     * Support for finding code errors where a DBRecordSet is opened but not closed.
     * 
     * @author bond
     */
    private synchronized void removeOpenResultSet()
    {
        Map<DBReader, Exception> openResultSets = threadLocalOpenResultSets.get();
        if (openResultSets.containsKey(this) == false)
        {
            log
               .error(
                      "DBRecordSet.removeOpenResultSet called for an object which is not in the open list. Here is the current stack.",
                      new Exception());
        } 
        else
        {
            openResultSets.remove(this);
        }
    }
    
    /**
     * copied from org.apache.commons.beanutils.ConstructorUtils since it's private there
     */
    @SuppressWarnings("unchecked")
    private static Constructor findMatchingAccessibleConstructor(Class clazz, Class[] parameterTypes)
    {
        // See if we can find the method directly
        // probably faster if it works
        // (I am not sure whether it's a good idea to run into Exceptions)
        // try {
        //     Constructor ctor = clazz.getConstructor(parameterTypes);
        //     try {
        //         // see comment in org.apache.commons.beanutils.ConstructorUtils
        //         ctor.setAccessible(true);
        //     } catch (SecurityException se) { /* ignore */ }
        //     return ctor;
        // } catch (NoSuchMethodException e) { /* SWALLOW */ }

        // search through all constructors 
        int paramSize = parameterTypes.length;
        Constructor[] ctors = clazz.getConstructors();
        for (int i = 0, size = ctors.length; i < size; i++)
        {   // compare parameters
            Class[] ctorParams = ctors[i].getParameterTypes();
            int ctorParamSize = ctorParams.length;
            if (ctorParamSize == paramSize)
            {   // Param Size matches
                boolean match = true;
                for (int n = 0; n < ctorParamSize; n++)
                {
                    if (!ObjectUtils.isAssignmentCompatible(ctorParams[n], parameterTypes[n]))
                    {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    // get accessible version of method
                    Constructor ctor = ConstructorUtils.getAccessibleConstructor(ctors[i]);
                    if (ctor != null) {
                        try {
                            ctor.setAccessible(true);
                        } catch (SecurityException se) { /* ignore */ }
                        return ctor;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * <PRE>
     * Call this if you want to check whether there are any unclosed resultsets
     * It logs stack traces to help find piece of code 
     * where a DBReader was opened but not closed.
     * </PRE>
     */
    public static synchronized void checkOpenResultSets()
    {
        Map<DBReader, Exception> openResultSets = threadLocalOpenResultSets.get();
        if (openResultSets != null && openResultSets.isEmpty() == false)
        {
            // we have found a(n) open result set(s). Now show the stack trace(s)
            Object keySet[] = openResultSets.keySet().toArray();
            for (int i = 0; i < keySet.length; i++)
            {
                Exception stackException = openResultSets.get(keySet[i]);
                log.error("A DBReader was not closed. Stack of opening code is ", stackException);
            }
            openResultSets.clear();
        }
    }
}