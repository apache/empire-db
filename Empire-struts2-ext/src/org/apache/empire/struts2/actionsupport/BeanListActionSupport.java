/*
 * ESTEAM Software GmbH, 09.07.2007
 */
package org.apache.empire.struts2.actionsupport;

import java.util.ArrayList;
import java.util.List;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;


/**
 * BeanListActionSupport
 * <p>
 * This class provides functions for handling list output from a database query through an list of JavaBeans.
 * </p> 
 * @author Rainer
 */
public class BeanListActionSupport<T> extends ListActionSupport
{
    protected ArrayList<T> list = null;
    
    protected Class<T> beanClass;

    public BeanListActionSupport(ActionBase action, Class<T> beanClass, String propertyName)
    {
        super(action, propertyName);
        // Set Bean Class
        this.beanClass = beanClass;
    }
    
    public ArrayList<T> getList()
    {
        if (list==null)
            log.warn("Bean List has not been initialized!");
        return list;
    }

    // SupplierReader
    public boolean initBeanList(DBCommand cmd)
    {
        DBReader reader = new DBReader();
        try {
            // Open Suppier Reader
            if (!reader.open(cmd, action.getConnection() ))
            {   return error(reader);
            }
            // Move to desired Position
            int first = this.getFirstItemIndex();
            if (first>0 && !reader.skipRows(first))
            {   // Page is not valid. Try again from beginning
                reader.close();
                setFirstItem(0);
                return initBeanList(cmd);
            }
            // Read List
            list = reader.getBeanList(beanClass, getPageSize());
            if (list==null)
            {   return error(reader);
            }
            // done
            return true;
            
        } finally {
            reader.close();
        }
    }

	public void setList(List<T> list)
	{
		this.list = new ArrayList<T>(list);
	}
}
