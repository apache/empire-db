/*
 * ESTEAM Software GmbH, 15.02.2012
 */
package org.apache.empire.db;

import java.util.Set;

import org.apache.empire.data.DataType;

public class DBCmdParam extends DBExpr
{
    private final static long serialVersionUID = 1L;
    protected DBCommand cmd;
    protected DataType  type;
    protected Object    value;
    
    protected DBCmdParam(DBCommand cmd, DataType type, Object value)
    {
        this.cmd = cmd;
        this.type = type;
        this.value = getCmdParamValue(value);
    }
    
    protected Object getCmdParamValue(Object value)
    {        
        switch (type)
        {
            case BLOB:
                if (value == null)
                    return null;
                if (value instanceof DBBlobData)
                    return value;
                if (value instanceof byte[])
                    return new DBBlobData((byte[])value);
                // create a blob data
                return new DBBlobData(value.toString());
            case CLOB:
                if (value == null)
                    return null;
                if (value instanceof DBClobData)
                    return value;
                // create a clob data
                return new DBClobData(value.toString());
            default:
                return value;
        }
    }
    
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        buf.append("?"); //$NON-NLS-1$
        // Move to current usage position
        cmd.notifyParamUsage(this);
    }
    
    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        // Nothing to add
    }
    
    @Override
    public DBDatabase getDatabase()
    {
        return cmd.getDatabase();
    }
    
    public DataType getDataType()
    {
        return type;
    }
    
    public Object getValue()
    {
        return value;
    }
    
    public void setValue(Object value)
    {
        this.value = getCmdParamValue(value);
    }
}
