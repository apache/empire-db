/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db.expr.compare;

import java.util.Set;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;


/**
 * This class is used for combining two filter constraints by and / or operator<br>
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBCompareExpr#and(DBCompareExpr)} or {@link DBCompareExpr#or(DBCompareExpr)} 
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBCompareAndOrExpr extends DBCompareExpr
{
    protected final DBCompareExpr left;
    protected final DBCompareExpr right;
    protected final boolean       or;

    /**
     * Constructs a DBCompareColExpr object set the specified parameters to this object.
     */
    public DBCompareAndOrExpr(DBCompareExpr left, DBCompareExpr right, boolean or)
    {
        this.left = left;
        this.right = right;
        this.or = or;
    }

    public DBCompareExpr getLeft()
    {
        return left;
    }

    public DBCompareExpr getRight()
    {
        return right;
    }

    public boolean isOr()
    {
        return or;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return left.getDatabase();
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    { // return all Columns
        left.addReferencedColumns(list);
        right.addReferencedColumns(list);
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
        // Name or Value Only ?
        if ((context & CTX_NAME )==0 ||
            (context & CTX_VALUE)==0)
        { // add both values separated by ","
            left.addSQL(buf, context);
            buf.append(",");
            right.addSQL(buf, context);
            return;
        }
        // Parenthesis
        boolean parenthesis = ((context & CTX_NOPARENTHESES) == 0) && or;
        boolean nested = ((left instanceof DBCompareAndOrExpr) && ((DBCompareAndOrExpr)left).or==false);
        if (parenthesis)
            buf.append("(");
        if (parenthesis && nested)
            buf.append("(");
        // the left expression
        left.addSQL(buf, context);
        // Parenthesis
        if (parenthesis && nested)
            buf.append(")");
        // Combine operator
        buf.append((or ? " OR " : " AND "));
        // Parenthesis
        nested = ((right instanceof DBCompareAndOrExpr) && ((DBCompareAndOrExpr)right).or==false);
        if (parenthesis && nested)
            buf.append("(");
        // the right expression
        right.addSQL(buf, context);
        if (parenthesis && nested)
            buf.append(")");
        // Parenthesis
        if (parenthesis)
            buf.append(")");
    }
    
    /**
     * Returns wheter the constraint should replace another one or not.
     * 
     * @return true it the constraints are mutually exclusive or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBCompareExpr other)
    {
    	if (other instanceof DBCompareAndOrExpr)
    	{
    		DBCompareAndOrExpr o = (DBCompareAndOrExpr)other;
    		if (left.equals(o.left) && right.equals(o.right))
    			return true;
    	}
    	return false;
    }
    
}