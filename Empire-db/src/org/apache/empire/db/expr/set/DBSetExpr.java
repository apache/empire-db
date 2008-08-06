/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db.expr.set;

import java.util.*;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;


/**
 * This class is used for building a set expression of an SQL update statement.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumn#to(Object)} factory method.
 * <P>
 * For the SQL fragment "set name="foo"<BR>
 * you should write: cmd.set( TABLE.NAME.to( "foo" ));
 * <P>
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de</A>
 */
public class DBSetExpr extends DBExpr
{
    public final DBColumn column;
    public Object         value;

    /**
     * Constructs a new DBSetExpr object. Sets the specified parameters to this object.
     */
    public DBSetExpr(DBColumn expr, Object value)
    {
        this.column = expr;
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
        return column.getDatabase();
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    public DBRowSet getTable()
    {
        return column.getRowSet();
    }

    /**
     * @return the column which value should be set
     */
    public DBColumn getColumn()
    {
        return column;
    }

    /**
     * @return the value to which to set the column to
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * The value to which to set the column
     * @param value the new column value 
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        list.add(column);
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
        if ((context & CTX_NAME) != 0)
            column.addSQL(buf, CTX_NAME);
        if ((context & CTX_NAME) != 0 && (context & CTX_VALUE) != 0)
            buf.append("=");
        if ((context & CTX_VALUE) != 0)
            buf.append(getObjectValue(column, value, CTX_NAME | CTX_VALUE, "+"));
    }
}
