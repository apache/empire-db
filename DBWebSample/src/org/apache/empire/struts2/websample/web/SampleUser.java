package org.apache.empire.struts2.websample.web;

public class SampleUser
{
    private String userId;
    private String displayName;
    
    public SampleUser(String userId, String displayName)
    {
        this.userId = userId;
        this.displayName = displayName;
    }
    
    public String getDisplayName()
    {
        return displayName;
    }
    public String getUserId()
    {
        return userId;
    }
    
}
