/*
 * ESTEAM Software GmbH, 25.01.2022
 */
package org.apache.empire.data.list;

import java.io.Serializable;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.ColumnExpr;

public class DataListHead implements Serializable
{
    private static final long serialVersionUID = 1L;
    // private static final Logger log  = LoggerFactory.getLogger(DataListHead.class);
    
    protected final ColumnExpr[] columns;
    
    protected String columnSeparator = "\t";

    /**
     * Constructs a DataListHead based on an DataListEntry constructor
     * @param constructor the DataListEntry constructor
     * @param columns the list entry columns
     */
    public DataListHead(ColumnExpr[] columns) 
    {
        this.columns = columns;
    }
    
    public ColumnExpr[] getColumns()
    {
        return columns; 
    }

    public int getColumnIndex(ColumnExpr column)
    {
        for (int i=0; i<columns.length; i++)
            if (columns[i]==column || columns[i].unwrap()==column)
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
     * Overwrite in order to change the behaviour
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
