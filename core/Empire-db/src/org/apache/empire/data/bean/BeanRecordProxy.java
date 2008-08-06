/*
 * ESTEAM Software GmbH, 01.07.2008
 */
package org.apache.empire.data.bean;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.Errors;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.Record;


/**
 * BeanRecordProxy
 * This class defines proxy that allows any POJO to behave like a record object.
 * @author Rainer
 */
public class BeanRecordProxy<T> extends ErrorObject implements Record
{
    protected static Log log = LogFactory.getLog(BeanRecordProxy.class);
    
    private List<Column> columns;
    private Column[] keyColumns;
    private boolean[] modified;

    private T data;

    public BeanRecordProxy(T data, List<Column> columns, Column[] keyColumns)
    {
        this.data = data;
        this.columns = columns;
        this.keyColumns = keyColumns;
    }

    public BeanRecordProxy(List<Column> columns, Column[] keyColumns)
    {
        this(null, columns, keyColumns);
    }

    public BeanRecordProxy(T data, BeanClass beanClass)
    {
        this(data, 
             ObjectUtils.convert(Column.class, beanClass.getProperties()), 
             beanClass.getKeyColumns());
    }

    public BeanRecordProxy(BeanClass beanClass)
    {
        this(null, 
             ObjectUtils.convert(Column.class, beanClass.getProperties()), 
             beanClass.getKeyColumns());
    }
    
    public T getBean()
    {
        return data;
    }

    public void setBean(T data)
    {
        this.data = data;
    }

    public Column getColumn(int index)
    {
        return columns.get(index);
    }

    public ColumnExpr getColumnExpr(int index)
    {
        return columns.get(index);
    }

    public Column[] getKeyColumns()
    {
        return keyColumns;
    }

    /**
     * Returns the array of primary key columns.
     * @return the array of primary key columns
     */
    public Object[] getKeyValues()
    {
        if (keyColumns==null)
            return null;
        // Get key values
        Object[] key = new Object[keyColumns.length];
        for (int i=0; i<keyColumns.length; i++)
            key[i] = this.getValue(keyColumns[i]);
        // the key
        return key;
    }

    public int getFieldCount()
    {
        return columns.size();
    }

    public int getFieldIndex(ColumnExpr column)
    {
        for (int i=0; i<columns.size(); i++)
        {
            if (columns.get(i).equals(column))
                return i;
        }
        return -1;
    }

    public int getFieldIndex(String columnName)
    {
        for (int i=0; i<columns.size(); i++)
        {
            if (columns.get(i).getName().equals(columnName))
                return i;
        }
        return -1;
    }

    public Options getFieldOptions(Column column)
    {
        return column.getOptions();
    }

    public boolean isFieldReadOnly(Column column)
    {
        return ObjectUtils.contains(keyColumns, column);
    }

    public boolean isFieldVisible(Column column)
    {
        return true;
    }

    public boolean isModified()
    {
        return (modified!=null);
    }

    public boolean isNew()
    {
        if (!isValid())
            return error(Errors.InvalidProperty, "bean");
        // Record is new until all key fields have been supplied
        clearError();
        if (keyColumns!=null)
        {   // Check all Key Columns
            for (int i=0; i<keyColumns.length; i++)
            {
                Object value = getValue(keyColumns[i]);
                if ((value instanceof Number) && ((Number)value).longValue()==0)
                    return true;
                if (ObjectUtils.isEmpty(value))
                    return true;
            }
        }
        // Not new
        return false;
    }

    public boolean isValid()
    {
        return (data!=null);
    }

    public Object getValue(ColumnExpr column)
    {
        if (!isValid())
        {   error(Errors.InvalidProperty, "bean");
            return null;
        }
        return getBeanPropertyValue(data, column);
    }

    public Object getValue(int index)
    {
        return getValue(getColumn(index));
    }

    public boolean isNull(ColumnExpr column)
    {
        return ObjectUtils.isEmpty(getValue(column));
    }

    public boolean isNull(int index)
    {
        return isNull(getColumn(index));
    }

    /**
     * sets the value of a field.
     */
    public boolean setValue(Column column, Object value)
    {
        if (!isValid())
            return error(Errors.InvalidProperty, "bean");
        // Track modification status
        if (ObjectUtils.compareEqual(getValue(column), value)==false)
        {
            if (modified== null)
                modified = new boolean[columns.size()]; 
            modified[getFieldIndex(column)] = true;
        }
        // Set Value
        return setBeanPropertyValue(data, column, value);
    }

    /**
     * sets the value of a field.
     */
    public boolean setValue(int i, Object value)
    {
        return setValue(getColumn(i), value);
    }

    /**
     * Detects whether or not a particular field has been modified.
     */
    public boolean wasModified(Column column)
    {
        int index = getFieldIndex(column);
        if (index<0)
            return error(Errors.ItemNotFound, column.getName());
        clearError();
        return (modified!=null && modified[index]);
    }

    /**
     * clears the modification status of the object and all fields.
     */
    public void clearModified()
    {
        modified = null;
    }

    // --------------- Bean support ------------------

    public boolean getBeanProperties(Object bean)
    {
        return getBeanProperties(bean, null);
    }

    public boolean getBeanProperties(Object bean, Collection<ColumnExpr> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            Column column = getColumn(i);
            if (column.isReadOnly())
                continue;
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            setBeanPropertyValue(bean, column, getValue(i));
        }
        return (count > 0);
    }

    public boolean setBeanValues(Object bean, Collection<Column> ignoreList)
    {
        // Add all Columns
        int count = 0;
        for (int i = 0; i < getFieldCount(); i++)
        { // Check Property
            Column column = getColumn(i);
            if (column.isReadOnly())
                continue;
            if (ignoreList != null && ignoreList.contains(column))
                continue; // ignore this property
            // Get Property Name
            String property = column.getBeanPropertyName();
            Object value = getBeanPropertyValue(bean, property);
            if (value==null && this.hasError())
                continue;
            if (setValue(column, value))
                count++;
        }
        return (count > 0);
    }

    public boolean setBeanValues(Object bean)
    {
        return setBeanValues(bean, null);
    }

    // --------------- private ------------------
    
    private Object getBeanPropertyValue(Object bean, ColumnExpr column)
    {
        // Check Params
        if (column==null)
        {   error(Errors.InvalidArg, "column");
            return null;
        }
        return getBeanPropertyValue(bean, column.getBeanPropertyName()); 
    }

    private Object getBeanPropertyValue(Object bean, String property)
    {
        // Check Params
        if (bean==null || property==null)
        {   error(Errors.InvalidArg, "property");
            return null;
        }
        try
        {   // Get Property Value
            clearError();
            PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
            return pub.getSimpleProperty(bean, property);

        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            error(e);
            return null;
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            error(e);
            return null;
        } catch (NoSuchMethodException e)
        {   log.warn(bean.getClass().getName() + ": no getter available for property '" + property + "'");
            error(e);
            return null;
        }
    }

    private boolean setBeanPropertyValue(Object bean, Column column, Object value)
    {
        // Check Params
        if (bean==null || column==null)
            return error(Errors.InvalidArg, "column");
        // Get Property Name
        String property = column.getBeanPropertyName(); 
        try
        {   // Get Property Value
            clearError();
            if (ObjectUtils.isEmpty(value))
                value = null;
            BeanUtils.setProperty(bean, property, value);
            // PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
            // pub.setSimpleProperty(data, property, value);
            return success();
        } catch (IllegalArgumentException e) {
            log.error(bean.getClass().getName() + ": invalid argument for property '" + property + "'");
            return error(e);
        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            return error(e);
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            return error(e);
        }    
    }
    
}
