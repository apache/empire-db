package org.apache.empire.data.list;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.RecordData;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.UnsupportedTypeException;

public class DataListFactoryImpl<T extends DataListEntry> implements DataListFactory<T>
{
    /**
     * findEntryConstructor
     * @param listEntryClass
     * @param listHeadClass
     * @return the constructor
     */
    @SuppressWarnings("unchecked")
    protected static <T extends DataListEntry> Constructor<T> findEntryConstructor(Class<?> listEntryClass, Class<? extends DataListHead> listHeadClass)
    {
        try
        {   // Alternatively use ClassUtils.findMatchingAccessibleConstructor(listEntryClass, new Class<?>[] { listHeadClass, int.class, Object[].class });
            return (Constructor<T>) listEntryClass.getDeclaredConstructor(listHeadClass, int.class, Object[].class);
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            throw new UnsupportedTypeException(listEntryClass);
        }
    }

    protected final Constructor<T> constructor;
    protected final DataListHead head;

    /**
     * Constructs a DataListHead based on an DataListEntry constructor
     * @param constructor the DataListEntry constructor
     * @param columns the list entry columns
     */
    public DataListFactoryImpl(Constructor<T> constructor, DataListHead head) 
    {
        this.constructor = constructor;
        this.head = head;
    }
    
    public DataListFactoryImpl(Class<T> listEntryClass, DataListHead head) 
    {
        this(findEntryConstructor(listEntryClass, DataListHead.class), head);
    }
    
    @Override
    public void prepareQuery()
    {
        /* Nothing */
    }

    @Override
    public List<T> newList(int capacity)
    {
        return new ArrayList<T>(capacity);
    }

    @Override
    public T newEntry(int rownum, RecordData dataRow)
    {   // check
        ColumnExpr[] columns = head.getColumns();
        if (columns.length!=dataRow.getFieldCount())
            throw new InvalidArgumentException("dataRow", dataRow);
        // copy values
        Object[] values = new Object[columns.length];
        for (int i=0; i<columns.length; i++)
            values[i] = dataRow.getValue(i);
        // create
        return newEntry(rownum, values);
    }

    protected T newEntry(int rownum, Object[] values)
    {   try
        {   // must override newEntry if no constructor is provided
            if (constructor==null)
                throw new NotSupportedException(this, "newEntry");
            // create item
            return constructor.newInstance(head, rownum, values);
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
        /* Nothing */
    }

}
