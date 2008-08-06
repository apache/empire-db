/*
 * ESTEAM Software GmbH, 24.07.2007
 */
package org.apache.empire.struts2.actionsupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.StringUtils;


public class ActionPropertySupport
{
    // Logger
    protected static Log log = LogFactory.getLog(ActionPropertySupport.class);

    private ActionBase action;
    private String propertyName;
    private boolean persist;
    
    public ActionPropertySupport(ActionBase action, String propertyName, boolean persist)
    {
        this.action = action;
        this.propertyName = propertyName;
        this.persist = persist;
    }
    
    // ------- ActionItem Property -------
    
    private String value;
    
    public String getName()
    {
        return propertyName;
    }

    public String getValue()
    {   // Get Item from request?
        if (value== null) // && (this instanceof NoParameters))
        {   value = action.getRequestParam(propertyName);
            if (value!=null)
                setValue(value);
        }
        // Check if Item is on Session
        if (value==null && persist)
            return StringUtils.toString(action.getActionObject(propertyName));
        // return Item
        return value;
    }

    public void setValue(String value)
    {   // Set Session Item
        if (value==null)
        {   log.error("Cannot set action item to null. Use clearitem() to remove the item from the session");
            return;
        }
        // Persist Item on session
        if (persist)
            action.putActionObject(propertyName, value);
        // Set item now
        this.value = value;
    }
    
    public void clear()
    {
        // Remove from session
        if (persist)
            action.removeActionObject(propertyName);
        // Clear value
        value=null;
    }
    
    public boolean hasValue(boolean lookOnSession)
    {
        if (lookOnSession==false)
            return (action.getRequestParam(propertyName)!=null);
        // Also look on Session
        return (getValue()!=null);
    }
}
