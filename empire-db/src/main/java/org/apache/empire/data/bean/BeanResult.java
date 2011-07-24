/*
 * ESTEAM Software GmbH, 24.07.2011
 */
package org.apache.empire.data.bean;

import java.sql.Connection;
import java.util.ArrayList;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;

/**
 * BeanResult
 * This is a simple helper class that performs reading a list of beans from a query
 * Internally DBReader.getBeanList() is used.
 *  
 * @author doebele
 */
public class BeanResult<T extends Object> extends ArrayList<T>
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;

    private DBCommand cmd;
    private Class<T> type;
    
    public BeanResult(Class<T> type, DBCommand cmd)
    {
        this.type = type;
        this.cmd = cmd;
    }
    
    public DBCommand getCommand()
    {
        return cmd;
    }
    
    public int fetch(Connection conn, int maxItems)
    {
        clear();
        DBReader reader = new DBReader();
        try {
            // Open and Read
            reader.open(cmd, conn);
            reader.getBeanList(this, type, maxItems);
            return size();
            
        } finally {
            reader.close();
        }
    }

    public final int fetch(Connection conn)
    {
        return fetch(conn, -1);
    }
    
}
