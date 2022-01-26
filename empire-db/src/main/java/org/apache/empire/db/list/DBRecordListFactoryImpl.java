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
    protected static <T extends DBRecord> Constructor<T> findRecordConstructor(Class<T> recordClass, Class<? extends DBContext> contextClass, Class<? extends DBRowSet> rowsetClass)
    {   try
        {   // find the constructor
            // Alternatively use ClassUtils.findMatchingAccessibleConstructor() 
            return recordClass.getDeclaredConstructor(contextClass, rowsetClass);
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            throw new UnsupportedTypeException(recordClass);
        }
    }
    
    /*
     * Members
     */
    protected final Constructor<T> constructor;
    protected final DBContext context;
    protected final DBRowSet rowset;

    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord constructor
     * @param constructor the DBRecord constructor
     * @param context the database context
     * @param rowset the rowset for the created records
     */
    public DBRecordListFactoryImpl(Constructor<T> constructor, DBContext context, DBRowSet rowset) 
    {
        this.constructor = constructor;
        this.context = context;
        this.rowset = rowset;
    }
    
    /**
     * Constructs a DBRecordListFactoryImpl based on an DBRecord constructor
     * @param recordClass the record Class to be created for this list
     * @param context the database context
     * @param rowset the rowset for the created records
     */
    public DBRecordListFactoryImpl(Class<T> recordClass, DBContext context, DBRowSet rowset) 
    {
        this(findRecordConstructor(recordClass, DBContext.class, DBRowSet.class), context, rowset);
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
        {   // create item
            T record = constructor.newInstance(context, rowset);
            rowset.initRecord(record, dataRow);
            return record;
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            throw new InternalException(e);
        }
    }
    
    @Override
    public void completeQuery(List<T> list)
    {
        
    }
    
}
