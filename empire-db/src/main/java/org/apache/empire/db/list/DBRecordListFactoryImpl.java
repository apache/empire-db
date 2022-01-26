/*
 * ESTEAM Software GmbH, 26.01.2022
 */
package org.apache.empire.db.list;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.UnsupportedTypeException;

/**
 * DBRecordListFactory
 * @author rainer
 */
public class DBRecordListFactoryImpl<T extends DBRecord> implements DBRecordListFactory<T>
{
    /**
     * findEntryConstructor
     * @param recordClass
     * @param listHeadClass
     * @return the constructor
     */
    @SuppressWarnings("unchecked")
    protected static <T extends DBRecord> Constructor<T> findEntryConstructor(Class<?> recordClass, Class<? extends DBContext> contextClass, Class<? extends DBRowSet> rowsetClass)
    {
        /*
        Constructor<?> constructor = ClassUtils.findMatchingAccessibleConstructor(recordClass, new Class<?>[] { listHeadClass, int.class, Object[].class });
        if (constructor==null)
            throw new UnsupportedTypeException(recordClass);
        return constructor;
        */
        try
        {   // Find the constructor
            return (Constructor<T>)recordClass.getDeclaredConstructor(contextClass, rowsetClass);
        }
        catch (NoSuchMethodException e)
        {
            throw new UnsupportedTypeException(recordClass);
        }
        catch (SecurityException e)
        {
            throw new UnsupportedTypeException(recordClass);
        }
    }
    
    protected final Constructor<T> constructor;
    protected final DBContext context;
    protected final DBRowSet rowset;

    /**
     * Constructs a DataListHead based on an DataListEntry constructor
     * @param constructor the DataListEntry constructor
     * @param columns the list entry columns
     */
    public DBRecordListFactoryImpl(Constructor<T> constructor, DBContext context, DBRowSet rowset) 
    {
        this.constructor = constructor;
        this.context = context;
        this.rowset = rowset;
    }
    
    public DBRecordListFactoryImpl(Class<T> recordClass, DBContext context, DBRowSet rowset) 
    {
        this(findEntryConstructor(recordClass, DBContext.class, DBRowSet.class), context, rowset);
    }
    
    @Override
    public void prepareQuery(DBCommand cmd)
    {
        if (cmd.hasSelectExpr())
        {   // Already has select expressions. 
            // Check against Rowset
            return;
        }
        // otherwise select
        cmd.select(rowset.getColumns());
    }
    
    @Override
    public List<T> newList(int capacity)
    {
        return new ArrayList<T>(capacity);        
    }
    
    @Override
    public T newRecord(int rownum, DBRecordData dataRow)
    {   try
        {   // check
            if (dataRow==null)
                throw new InvalidArgumentException("data", dataRow);
            // must override newEntry if no recordClass is provided
            if (constructor==null)
                throw new NotSupportedException(this, "newEntry");
            // create item
            T record = constructor.newInstance(context, rowset);
            rowset.initRecord(record, dataRow);
            return record;
        }
        catch (InstantiationException e)
        {
            throw new InternalException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new InternalException(e);
        }
        catch (IllegalArgumentException e)
        {
            throw new InternalException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new InternalException(e);
        }
    }
    
    @Override
    public void completeQuery(List<T> list)
    {
        
    }
    
}
