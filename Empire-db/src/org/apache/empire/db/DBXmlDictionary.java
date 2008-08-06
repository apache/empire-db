/*
 * ESTEAM Software GmbH, 22.12.2007
 */
package org.apache.empire.db;

/**
 * This class is used to configure XML generation as performed by the 
 * getXmlDocument Document function on DBReader and DBRecord.<BR>
 */
public class DBXmlDictionary
{
    private static DBXmlDictionary dbXmlDictonary; 
    
    public static DBXmlDictionary getInstance()
    {
        if (dbXmlDictonary==null)
        {
            // dbXmlTagDictionary has not been set. Using Default Dictionary");
            dbXmlDictonary = new DBXmlDictionary();
        }    
        return dbXmlDictonary;
    }

    public static void set(DBXmlDictionary xmlDictonary)
    {
        dbXmlDictonary = xmlDictonary;
    }

    // ------- XML Element and Attribute Names -------
    
    public String getRowSetElementName() {
        return "rowset"; 
    }
    
    public String getRowElementName() {
        return "row"; 
    }
    
    public String getRowIdColumnAttribute() {
        return "id";
    }
    
}
