/*
 * ESTEAM Software GmbH, 09.07.2007
 */
package org.apache.empire.struts2.actionsupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBReader;


/**
 * ReaderListActionSupport
 * <p>
 * This class provides functions for handling list output from a database query through a DBReader object.
 * </p> 
 * @author Rainer
 */
public class ReaderListActionSupport extends ListActionSupport
{
    @SuppressWarnings("hiding")
    protected static Log log = LogFactory.getLog(RecordActionSupport.class);
    
    protected DBReader  reader;

    public ReaderListActionSupport(ActionBase action, String propertyName)
    {
        super(action, propertyName);
    }
    
    public DBReader getReader()
    {
        return reader;
    }
    
    public boolean initReader(DBCommandExpr cmd, boolean scrollable)
    {
        // Make sure previous reader is closed
        if (reader!=null)
            reader.close();
        // Create a new reader
        reader = new DBReader();
        if (!reader.open(cmd, scrollable, action.getConnection() ))
        {   return error(reader);
        }
        // Move to desired Position
        int first = this.getFirstItemIndex();
        if (first>0 && !reader.skipRows(first))
        {   // Page is not valid. Try again from beginning
            reader.close();
            setFirstItem(0);
            return initReader(cmd);
        }
        // done
        return true;
    }
    
    public boolean initReader(DBCommandExpr cmd)
    {
        return initReader(cmd, false);
    }

}
