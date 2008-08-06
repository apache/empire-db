/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db.expr.column;

// Java
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class is used for declaring constant values in SQL.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBDatabase#getValueExpr(String)} or one of it's overloads
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de</A>
 */
public class DBValueExpr extends DBColumnExpr
{
    public final DBDatabase   db;
    public final Object       value;
    public final DataType     type;
    public final DBColumnExpr column;

    /**
     * Constructs a new DBValueExpr object set the specified parameters to this object.
     */
    public DBValueExpr(DBDatabase db, Object value, DataType type)
    {
        this.db = db;
        this.value = value;
        this.type = type;
        this.column = null;
    }

    /**
     * Construct a new DBValueExpr object set the specified parameters to this object.
     */
    public DBValueExpr(DBColumnExpr col, Object value)
    {
        this.column = col;
        this.type = col.getDataType();
        this.db = col.getDatabase();
        this.value = value;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return db;
    }

    /**
     * Returns the data type of the DBColumnExpr object.
     * 
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return type;
    }

    /**
     * Returns the column name.
     * 
     * @return the column name
     */
    @Override
    public String getName()
    {
        return (column != null) ? column.getName() : null;
    }

    /** this helper function calls the DBColumnExpr.addXML(Element, long) method */
    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem;
        if (column!=null)
        {   // Update Column
            elem = column.addXml(parent, flags);
        }
        else
        {   // Add a column expression for this function
            elem = XMLUtil.addElement(parent, "column");
            String name = getName();
            if (name!=null)
                elem.setAttribute("name", getName());
            // Add Other Attributes
            if (attributes!=null)
                attributes.addXml(elem, flags);
            // add All Options
            if (options!=null)
                options.addXml(elem, flags);
        }
        // Done
        elem.setAttribute("function", "value");
        return elem;
    }

    /**
     * Returns null.
     * 
     * @return null
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return (column != null) ? column.getUpdateColumn() : null;
    }

    /**
     * Always returns false since value expressions cannot be an aggregate.
     * 
     * @return false
     */
    @Override
    public boolean isAggregate()
    {
        return false;
    }

    /**
     * Creates the SQL-Command.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        DBDatabaseDriver driver = db.getDriver();
        String text = (driver!=null) ? driver.getValueString(value, getDataType()) : String.valueOf(value); 
        buf.append(text);
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        // nothing to do!
        return;
    }

}
