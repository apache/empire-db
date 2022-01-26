/*
 * ESTEAM Software GmbH, 25.01.2022
 */
package org.apache.empire.data.list;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.RecordData;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.UnsupportedTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataListHead<T extends DataListEntry> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final Logger log  = LoggerFactory.getLogger(DataListHead.class);
    
    final Constructor<T> constructor;
    final ColumnExpr[] columns;
    
    protected String columnSeparator = "\t";
    
    /**
     * findEntryConstructor
     * @param listEntryClass
     * @param listHeadClass
     * @return the constructor
     */
    @SuppressWarnings("unchecked")
    protected static <T extends DataListEntry> Constructor<T> findEntryConstructor(Class<?> listEntryClass, @SuppressWarnings("rawtypes") Class<? extends DataListHead> listHeadClass)
    {
        /*
        Constructor<?> constructor = ClassUtils.findMatchingAccessibleConstructor(listEntryClass, new Class<?>[] { listHeadClass, int.class, Object[].class });
        if (constructor==null)
            throw new UnsupportedTypeException(listEntryClass);
        return constructor;
        */
        try
        {   // Find the constructor
            return (Constructor<T>)listEntryClass.getDeclaredConstructor(listHeadClass, int.class, Object[].class);
        }
        catch (NoSuchMethodException e)
        {
            throw new UnsupportedTypeException(listEntryClass);
        }
        catch (SecurityException e)
        {
            throw new UnsupportedTypeException(listEntryClass);
        }
    }

    /**
     * Constructs a DataListHead based on an DataListEntry constructor
     * @param constructor the DataListEntry constructor
     * @param columns the list entry columns
     */
    public DataListHead(Constructor<T> constructor, ColumnExpr[] columns) 
    {
        this.constructor = constructor;
        this.columns = columns;
    }
    
    public DataListHead(Class<T> listEntryClass, ColumnExpr[] columns) 
    {
        this(findEntryConstructor(listEntryClass, DataListHead.class), columns);
    }
    
    public ColumnExpr[] getColumns()
    {
        return columns; 
    }

    public int getColumnIndex(ColumnExpr column)
    {
        for (int i=0; i<columns.length; i++)
            if (columns[i]==column)
                return i; 
        // Not found, try by name
        return getColumnIndex(column.getName());
    }
    
    public int getColumnIndex(String columnName)
    {
        for (int i=0; i<columns.length; i++)
            if (columnName.equalsIgnoreCase(columns[i].getName()))
                return i; 
        // not found
        return -1;
    }
    
    public T newEntry(int rownum, Object[] values)
    {   try
        {   // check
            if (columns.length!=values.length)
                throw new InvalidArgumentException("values", values);
            // must override newEntry if no listEntryClass is provided
            if (constructor==null)
                throw new NotSupportedException(this, "newEntry");
            // create item
            return constructor.newInstance(this, rownum, values);
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

    public final T newEntry(int rownum, RecordData data)
    {   // check
        if (columns.length!=data.getFieldCount())
            throw new InvalidArgumentException("data", data);
        // copy values
        Object[] values = new Object[columns.length];
        for (int i=0; i<columns.length; i++)
            values[i] = data.getValue(i);
        // create
        return newEntry(rownum, values);
    }
    
    public String formatValue(int idx, Object value)
    {   // check empty
        if (ObjectUtils.isEmpty(value))
            return StringUtils.EMPTY;
        // check options
        Options options = columns[idx].getOptions();
        if (options!=null && options.has(value))
        {   // lookup option
            value = options.get(value);
        }
        // Escape
        return escape(String.valueOf(value));
    }
    
    /**
     * Escapes the formatted value
     * Default is a simple HTML escape
     */
    protected String escape(String text)
    {
        if (text==null || text.length()==0)
            return StringUtils.EMPTY;
        // &amp;
        if (text.indexOf('&')>=0)
            text = StringUtils.replaceAll(text, "&", "&amp;");
        // &lt;
        if (text.indexOf('<')>=0)
            text = StringUtils.replaceAll(text, "<", "&lt;");
        // &gt;
        if (text.indexOf('>')>=0)
            text = StringUtils.replaceAll(text, ">", "&gt;");
        // done
        return text;
    }
    
}
