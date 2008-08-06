package org.apache.empire.struts2.websample.web.actiontypes;


public abstract class DetailAction extends Action
{
    /**
     * Action mappings
     */
    public static final String INPUT  = "input";
    public static final String RETURN = "return";

    // Detail Action
    public DetailAction()
    {
        // Default constructor
    }
        
    public abstract String doCreate();
    
    public abstract String doLoad();

    public abstract String doSave();

    public abstract String doDelete();

    // Optional overridable
    public String doCancel()
    {
        return RETURN;
    }
    
}
