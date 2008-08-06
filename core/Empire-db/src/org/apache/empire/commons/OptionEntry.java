/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.commons;

/**
 * This class defines one possible value of a field and it's description<BR>
 * This class is used by the Options class to implement a set of options 
 * where the option value us used as the key for the set.<BR>
 * The text should only be used for display purposes e.g. to display a drop-down in a user interface.<BR>
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class OptionEntry
{
    private Object value;
    private String text;
    
    public OptionEntry(Object value, String text)
    {
        this.value = value;
        this.text = text;
    }

    public Object getValue()
    {
        return value;
    }

    public String getValueString()
    {   // Convenience Function   
        return (value!=null ? String.valueOf(value) : "");
    }
    
    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}
