/*
 * ESTEAM Software GmbH, 07.12.2007
 */
package org.apache.empire.struts2.websample.db;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;

/**
 * Base class definition for all database tables
 * Automatically generates a message-key for the field title
 * e.g. for the column EMPLOYEES.DATE_OF_BIRTH
 * it generates the key "!field.title.employees.dateOfBirth";
 */
public class SampleTable extends DBTable
{
    public final String MESSAGE_KEY_PREFIX = "!field.title.";
    
    public SampleTable(String name, DBDatabase db)
    {
        super(name, db);
    }

    @Override
    protected boolean addColumn(DBTableColumn column)
    {
        // Set Translation Title
        String col = column.getBeanPropertyName();  
        String tbl = getName().toLowerCase();   
        String key = MESSAGE_KEY_PREFIX + tbl + "." + col;
        column.setTitle(key);

        // Set Default Control Type
        DataType type = column.getDataType();
        column.setControlType((type==DataType.BOOL) ? "checkbox" : "text");

        // Add Column
        return super.addColumn(column);
    }
}
