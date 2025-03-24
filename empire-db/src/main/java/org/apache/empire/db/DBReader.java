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

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.data.EntityType;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.db.exceptions.NoPrimaryKeyException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.list.DataBean;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.exceptions.BeanInstantiationException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.InvalidOperationException;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * <P>
 * This class is used to perform database queries from a DBCommand object and access the results.<BR>
 * In oder to perform a query call the open() function or - for single row queries - call getRecordData();<BR>
 * You can iterate through the rows using moveNext() or an iterator.<BR>
 * <P>
 * However take care: A reader must always be explicitly closed using the close() method!<BR>
 * Otherwise you may lock the JDBC connection and run out of resources.<BR>
 * Use <PRE>try { ... } finally { reader.close(); } </PRE> to make sure the reader is closed.<BR>
 * <P>
 * To access and work with the query result you can do one of the following:<BR>
 * <ul>
 *  <li>access field values directly by using one of the get... functions (see {@link DBRecordData})</li> 
 *  <li>get the rows as a list of Java Beans using by using {@link DBReader#getBeanList(Class, int)}</li> 
 *  <li>get the rows as an XML-Document using {@link DBReader#getXmlDocument()} </li> 
 *  <li>initialize a DBRecord with the current row data using {@link DBReader#initRecord(DBRecordBase)}<br>
 *      This will allow you to modify and update the data. 
 *  </li> 
 * </ul>
 *
 *
 */
public class DBReader extends DBRecordData implements Closeable
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    /**
     * DBReaderIterator
     * Base class for DBReader interators
     * @author rainer
     */
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
        @Override
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
        @Override
        public boolean hasNext()
        {
            try
            {   // Check position
                if (curCount >= maxCount)
                    return false;
                // Check Recordset
                if (rset == null || rset.isLast() || rset.isAfterLast())
                    return false;
                // there are more records
                return true;
            } catch (SQLException e) {
                // Error
                throw new EmpireSQLException(context.getDbms(), e);
            }
        }

        /**
         * Implements the Iterator Interface.
         * 
         * @return the current Record interface
         */
        @Override
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
        @Override
        public boolean hasNext()
        {
            // Check position
            if (curCount >= maxCount)
                return false;
            if (rset == null)
                throw new ObjectNotValidException(this);
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
        @Override
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
    protected static final Logger log = LoggerFactory.getLogger(DBReader.class);
    
    private static boolean trackOpenResultSets = false; 
    
    /**
     * Support for finding code errors where a DBRecordSet is opened but not closed
     */
    private static ThreadLocal<Map<DBReader, Exception>> threadLocalOpenResultSets = new ThreadLocal<Map<DBReader, Exception>>();

    // the context
    protected final DBContext context;
    
    // Object references
    private DBDatabase     db      = null;
    private DBColumnExpr[] columns = null;
    private ResultSet      rset    = null;
    private DBMSHandler    dbms    = null;
    // the field index map
    private Map<ColumnExpr, Integer> fieldIndexMap = null;

    /**
     * Constructs an empty DBRecordSet object.
     * @param context the database context
     * @param useFieldIndexMap flag whether to use a fieldIndexMap
     */
    public DBReader(DBContext context, boolean useFieldIndexMap)
    {
        this.context = context;
        if (useFieldIndexMap)
            fieldIndexMap = new HashMap<ColumnExpr, Integer>();
    }

    /**
     * Constructs a default DBReader object with the fieldIndexMap enabled.
     * @param context the database context
     */
    public DBReader(DBContext context)
    {
        // Simple Constructor
        this(context, true);
    }

    /**
     * Returns the current Context
     * @return the database context
     */
    @Override
    public DBContext getContext()
    {
        return context;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
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
        if (fieldIndexMap==null)
            return findFieldIndex(column);
        // Use fieldIndexMap
        Integer index = fieldIndexMap.get(column);
        if (index==null)
        {   // add to field Index map
            index = findFieldIndex(column);
            fieldIndexMap.put(column, index);
        }
        return index;
    }
    
    /** Get the column Expression at position */
    @Override
    public DBColumnExpr getColumn(int iColumn)
    {
        if (columns == null || iColumn < 0 || iColumn >= columns.length)
            return null; // Index out of range
        // return column Expression
        return columns[iColumn];
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
        if (columns != null)
        {
            for (int i = 0; i < columns.length; i++)
                if (columns[i].getName().equalsIgnoreCase(column))
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
        if (index < 0 || index >= columns.length)
        { // Index out of range
            log.error("Index out of range: " + index);
            return true;
        }
        try
        { // Check Value on Resultset
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
        // Check params
        if (index < 0 || index >= columns.length)
            throw new InvalidArgumentException("index", index);
        try
        {   // Get Value from Resultset
            DataType dataType = columns[index].getDataType();
            return dbms.getResultValue(rset, index + 1, dataType);

        } catch (SQLException e) {
            // Operation failed
            throw new EmpireSQLException(context.getDbms(), e);
        }
    }
    
    /**
     * Returns the record key for a type of entity
     * @param entityType the entity type or rowset for which to get key
     * @return the record key
     */
    public Object[] getRecordKey(EntityType entityType)
    {
        Column[] keyColumns = entityType.getKeyColumns();
        if (keyColumns==null || keyColumns.length==0)
            throw new NoPrimaryKeyException(entityType);
        // Collect key
        Object[] key = new Object[keyColumns.length];
        for (int i=0; i<key.length; i++)
            key[i] = this.get(keyColumns[i]);
        return key;
    }

    /**
     * Returns the record id for a type of entity which has a single numeric primary key
     * @param entityType the entity type or rowset for which to get key
     * @return the record id
     * @throws InvalidArgumentException if the entity has not a single numeric primary key
     */
    public long getRecordId(EntityType entityType)
    {
        Column[] keyColumns = entityType.getKeyColumns();
        if (keyColumns==null || keyColumns.length!=1)
            throw new InvalidArgumentException("entityType", entityType.getEntityName());
        // return id
        return this.getLong(keyColumns[0]);
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
     */
    public void open(DBCommandExpr cmd, boolean scrollable)
    {
        if (isOpen())
            close();
        // Get the query statement
        String sqlCmd = cmd.getSelect();
        Object[] paramValues = cmd.getParamValues();
        // Collect the query parameters
        /*
        List<Object> subqueryParamValues = (cmd instanceof DBCommand) ? findSubQueryParams((DBCommand)cmd) : null;
        if (subqueryParamValues!=null && !subqueryParamValues.isEmpty())
        {   // Check Count
            if (paramValues==null)
            {   // use subquery params
                paramValues = subqueryParamValues.toArray();
            }
            else if (paramValues.length!=subqueryParamValues.size())
            {   // number of params do not match
                String msg = MessageFormat.format("Invalid number of parameters query: provided={0}, required={1}; query="+cmd.getSelect(), paramValues.length, subqueryParamValues.size());
                throw new UnspecifiedErrorException(msg);
            }
        }
        */
        // Execute the query
        DBUtils utils = context.getUtils();
        ResultSet queryRset = utils.executeQuery(sqlCmd, paramValues, scrollable);
        if (queryRset==null)
            throw new QueryNoResultException(sqlCmd);
        // init
        init(cmd.getDatabase(), cmd.getSelectExprList(), queryRset);
    }

    /**
     * Opens the reader by executing the given SQL command.<BR>
     * <P>
     * see {@link DBReader#open(DBCommandExpr, boolean)}
     * </P>
     * @param cmd the SQL-Command with cmd.getSelect()
     */
    public final void open(DBCommandExpr cmd)
    {
        open(cmd, false);
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
     */
    public void getRecordData(DBCommandExpr cmd)
    { // Open the record
        open(cmd);
        // Get First Record
        if (!moveNext())
        { // Close
            throw new QueryNoResultException(cmd.getSelect());
        }
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
            {   iterator.dispose();
                iterator = null;
            }
            // Close JDBC-Resultset
            if (rset != null)
            {   // call dbms
                context.getDbms().closeResultSet(rset);
                // remove from tracking-list
                endTrackingThisResultSet();
            }
            // Detach columns
            columns = null;
            rset = null;
            dbms = null;
            // clear FieldIndexMap
            if (fieldIndexMap!=null)
                fieldIndexMap.clear();
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
        {   // Check Recordset
            if (rset == null)
                throw new ObjectNotValidException(this);
            // Forward only cursor?
            int type = rset.getType();
            if (type == ResultSet.TYPE_FORWARD_ONLY)
            {
                if (count < 0)
                    throw new InvalidArgumentException("count", count);
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

        } catch (SQLException e) {
            // an error occurred
            throw new EmpireSQLException(context.getDbms(), e);
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
        {   // Check Recordset
            if (rset == null)
                throw new ObjectNotValidException(this);
            // Move Next
            if (rset.next() == false)
            { // Close recordset automatically after last record
                close();
                return false;
            }
            return true;

        } catch (SQLException e) {
            // an error occurred
            throw new EmpireSQLException(context.getDbms(), e);
        }
    }

    private DBReaderIterator iterator = null; // there can only be one!

    /**
     * Returns an row iterator for this reader.<BR>
     * There can only be one iterator at a time.
     * <P>
     * @param maxCount the maximum number of item that should be returned by this iterator
     * @return the row iterator
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
     * @return the row iterator
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
     * set also {@link DBRowSet#initRecord(DBRecordBase, DBRecordData)});
     * </PRE>
     * @param rec the record which to initialize
     */
    public void initRecord(DBRecordBase rec)
    {
        // Check Open
        if (!isOpen())
        {   // Resultset not available
            throw new ObjectNotValidException(this);
        }
        // init Record
        DBRowSet rowset = rec.getRowSet();
    	rowset.initRecord(rec, this);
    }
    
    /**
     * Returns the result of a query as a list of objects restricted
     * to a maximum number of objects (unless maxCount is -1).
     * 
     * @param <L> the list type
     * @param <T> the list item type
     * 
     * @param list the collection to add the objects to
     * @param t the class type of the objects in the list
     * @param parent the bean parent
     * @param maxCount the maximum number of objects
     * 
     * @return the list of T
     */
    @SuppressWarnings("unchecked")
    public <L extends List<T>, T> L getBeanList(L list, Class<T> t, Object parent, int maxCount)
    {
        // Check Open
        if (!isOpen())
        {   // Resultset not available
            throw new ObjectNotValidException(this);
        }
        // Query List
        try
        {   // Find Constructor
            Constructor<?> ctor = findBeanConstructor(t);
            Object[] args = (ctor!=null) ? new Object[getFieldCount()] : null; 
            Class<?>[] ctorParamTypes = (ctor!=null) ? ctor.getParameterTypes() : null;
            
            // Create a list of beans
            int rownum = 0;
            while (moveNext() && maxCount != 0)
            {   // Create bean an init
                T bean;
                if (ctor!=null)
                {   // Use Constructor
                    for (int i = 0; i < getFieldCount(); i++)
                        args[i] = ObjectUtils.convert(ctorParamTypes[i], getValue(i));
                    bean = (T)ctor.newInstance(args);
                }
                else
                {   // Use Property Setters
                    bean = t.newInstance();
                    setBeanProperties(bean);
                }
                // add
                list.add(bean);
                rownum++;
                // post processing
                if (bean instanceof DataBean<?>)
                    ((DataBean<?>)bean).initialize(((DBObject)this).getDatabase(), context, rownum, parent);
                // Decrease count
                if (maxCount > 0)
                    maxCount--;
            }
            // done
            return list;
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            // ReflectiveOperationException
            throw new BeanInstantiationException(t, e);
        }
    }
    
    /**
     * Returns the result of a query as a list of objects.
     * @param <T> the list item type
     * @param t the class type of the objects in the list
     * @param maxItems the maximum number of objects
     * @return the list of T
     */
    public final <T> List<T> getBeanList(Class<T> t, int maxItems) 
    {
        return getBeanList(new ArrayList<T>(), t, null, maxItems);
    }
    
    /**
     * Returns the result of a query as a list of objects.
     * @param <T> the list item type
     * @param t the class type of the objects in the list
     * @return the list of T
     */
    public final <T> List<T> getBeanList(Class<T> t) 
    {
        return getBeanList(t, -1);
    }
    
    /**
     * Moves the cursor down one row from its current position.
     * 
     * @return the number of column descriptions added to the Element
     */
    @Override
    public int addXmlMeta(Element parent)
    {
        if (columns == null)
            throw new ObjectNotValidException(this);
        // Add Field Description
        for (int i = 0; i < columns.length; i++)
            columns[i].addXml(parent, 0);
        // return count
        return columns.length; 
    }

    /**
     * Adds all children to a parent.
     * 
     * @param parent the parent element below which to search the child
     * @return the number of row values added to the element
     */
    @Override
    public int addXmlData(Element parent)
    {
        if (rset == null)
            throw new ObjectNotValidException(this);
        // Add all children
        for (int i = 0; i < columns.length; i++)
        { // Read all
            String name = columns[i].getName();
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
        // return count
        return columns.length; 
    }

    /**
     * Adds all children to a parent.
     * 
     * @param parent the parent element below which to search the child
     * @return the number of rows added to the element
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
            addXmlData(XMLUtil.addElement(parent, rowElementName));
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
     * Returns a XML document with the field description an values of this record.
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
        addXmlMeta(root);
        // Add row rset
        addRows(root);
        // return Document
        return root.getOwnerDocument();
    }

    /** returns the number of the elements of the colList array */
    @Override
    public int getFieldCount()
    {
        return (columns != null) ? columns.length : 0;
    }

    /**
     * Initialize the reader from an open JDBC-ResultSet 
     * @param db the database
     * @param columns the query column expressions
     * @param rset the JDBC-ResultSet
     */
    protected void init(DBDatabase db, DBColumnExpr[] columns, ResultSet rset)
    {
        this.db = db;
        this.dbms = db.getDbms();
        this.columns = columns;
        this.rset = rset;
        // clear fieldIndexMap         
        if (fieldIndexMap!=null)
            fieldIndexMap.clear();
        // add to tracking list (if enabled)
        trackThisResultSet();
    }

    /**
     * Access the column expression list
     * @return the column expression list
     */
    protected final DBColumnExpr[] getColumnExprList()
    {
        return columns;
    }

    /**
     * Access the JDBC-ResultSet
     * @return the JDBC-ResultSet
     */
    protected final ResultSet getResultSet()
    {
        return rset;
    }

    /**
     * finds the field Index of a given column expression
     * Internally used as helper for getFieldIndex()
     * @param column the column to find
     * @return the index value
     */
    protected int findFieldIndex(ColumnExpr column)
    {
        if (columns == null)
            throw new ObjectNotValidException(this);
        // First chance: Try to find an expression match
        int index = ObjectUtils.indexOf(columns, column);
        if (index>= 0)
            return index;
        // Second chance: Try Update Column
        if (column instanceof DBColumn)
        {
            for (int i = 0; i < columns.length; i++)
            {
                DBColumn updColumn = columns[i].getUpdateColumn();                    
                if (updColumn!=null && updColumn.equals(column))
                    return i;
                 // Query Expression?
                if (updColumn instanceof DBQueryColumn)
                {   updColumn = ((DBQueryColumn)updColumn).getExpr().getUpdateColumn();
                    if (updColumn!=null && updColumn.equals(column))
                        return i;
                }
            }
        }
        // not found!
        return -1;
    }

    /**
     * internal helper function to find parameterized subqueries
     * @param cmd the command
     * @return a list of parameter arrays, one for each subquery
    protected List<Object> findSubQueryParams(DBCommand cmd)
    {
        List<Object> subQueryParams = null;
        List<DBJoinExpr> joins = cmd.getJoins();
        if (joins==null)
            return null;  // no joins
        // check the joins
        for (DBJoinExpr j : joins)
        {
            DBRowSet rsl = j.getLeftTable();
            DBRowSet rsr = j.getRightTable();
            if (rsl instanceof DBQuery)
            {   // the left join is a query
                subQueryParams = addSubQueryParams((DBQuery)rsl, subQueryParams);
            }
            if (rsr instanceof DBQuery)
            {   // the right join is a query
                subQueryParams = addSubQueryParams((DBQuery)rsr, subQueryParams);
            }
        }
        return subQueryParams; 
    }
     */
    
    /**
     * Adds any subquery params to the supplied list
     * @param query the subquery
     * @param list the current list of parameters
     * @return the new list of parameters
    private List<Object> addSubQueryParams(DBQuery query, List<Object> list)
    {
        DBCommandExpr sqcmd = query.getCommandExpr();
        Object[] params = query.getCommandExpr().getParamValues();
        if (params!=null && params.length>0)
        {   // add params
            if (list== null)
                list = new ArrayList<Object>();
            for (Object p : params)
                list.add(p);    
        }
        // recurse
        if (sqcmd instanceof DBCommand)
        {   // check this command too
            List<Object> sqlist = findSubQueryParams((DBCommand)sqcmd);
            if (sqlist!=null && !sqlist.isEmpty())
            {   // make one list
                if (list!= null)
                    list.addAll(sqlist);
                else 
                    list = sqlist;
            }
        }
        return list;
    }
     */

    /**
     * Returns a constructor for a bean class for the set of parameters or null if no suitable constructor is found
     * @param beanClass the bean class
     * @return a constructor for the readers columns or null if not suitable constructor is available
     */
    protected Constructor<?> findBeanConstructor(Class<?> beanClass)
    {
        // Check whether we can use a constructor
        Class<?>[] paramTypes = new Class[getFieldCount()];
        for (int i = 0; i < columns.length; i++)
            paramTypes[i] = columns[i].getJavaType(); 
        // Find Constructor
        Constructor<?> ctor = ClassUtils.findMatchingConstructor(beanClass, -1, paramTypes);
        return ctor;
    }

    /**
     * Support for finding code errors where a DBRecordSet is opened but not closed.
     * 
     * @author bond
     */
    protected synchronized void trackThisResultSet()
    {
        // check if enabled
        if (trackOpenResultSets==false)
            return;
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
            log.error("DBRecordSet.addOpenResultSet called for an object which is already in the open list. This is the stack of the method opening the object which was not previously closed.", stackException);
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
    protected synchronized void endTrackingThisResultSet()
    {
        // check if enabled
        if (trackOpenResultSets==false)
            return;
        // remove
        Map<DBReader, Exception> openResultSets = threadLocalOpenResultSets.get();
        if (openResultSets.containsKey(this) == false)
        {
            log.error("DBRecordSet.removeOpenResultSet called for an object which is not in the open list. Here is the current stack.", new Exception());
        } 
        else
        {
            openResultSets.remove(this);
        }
    }

    /**
     * Enables or disabled tracking of open ResultSets
     * @param enable true to enable or false otherwise
     * @return the previous state of the trackOpenResultSets
     */
    public static synchronized boolean enableOpenResultSetTracking(boolean enable)
    {
        boolean prev = trackOpenResultSets;
        trackOpenResultSets = enable;
        return prev;
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
        // check if enabled
        if (trackOpenResultSets==false)
            throw new InvalidOperationException("Open-ResultSet-Tracking has not been enabled. Use DBReader.enableOpenResultSetTracking() to enable or disable.");
        // Check map
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