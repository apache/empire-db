/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db.expr.compare;

import java.util.Set;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;


/**
 * This class is used for defining filter constraints based on a column expression in SQL<br>
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBCompareExpr#not()}<BR>
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBCompareNotExpr extends DBCompareExpr
{
    public final DBCompareExpr expr;

    /**
     * constructs a DBCompareColExpr object set the specified parameters to this object
     */
    public DBCompareNotExpr(DBCompareExpr expr)
    {
        this.expr = expr;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return expr.getDatabase();
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    { 
        expr.addReferencedColumns(list);
    }

    /**
     * Creates the SQL-Command sets the specified compare value
     * (the varible boolOP) between the two DBCompareExpr objects.
     * 
     * @param buf the SQL statment
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        // Name Only ?
        if ((context & CTX_VALUE)==0)
        { // add both values separated by ","
            expr.addSQL(buf, context);
            return;
        }
        // add SQL
        buf .append(" NOT(");
        expr.addSQL(buf, context);
        buf .append(" )");
    }
    
    /**
     * Returns wheter the constraint should replace another one or not.
     * 
     * @return true it the constraints are mutually exclusive or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
    	return false;
    }
    
}