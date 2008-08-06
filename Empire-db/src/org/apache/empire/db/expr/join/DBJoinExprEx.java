/*
 * ESTEAM Software GmbH, 07.03.2005
 */
package org.apache.empire.db.expr.join;

import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBJoinType;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.compare.DBCompareAndOrExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;

/**
 * This class is used for building a join expression of an SQL statement.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use @link {@link org.apache.empire.db.DBCommand#join(DBRowSet, DBCompareExpr, DBJoinType)}
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBJoinExprEx extends DBJoinExpr
{
    private final DBCompareExpr cmp;
    
    private static DBColumnExpr findFirstColumn(DBCompareExpr expr)
    {
        // DBCompareORExpr 
        while (expr instanceof DBCompareAndOrExpr)
               expr = ((DBCompareAndOrExpr)expr).getLeft();
        // Get Colum Expr
        if (expr instanceof DBCompareColExpr)
            return ((DBCompareColExpr)expr).getColumnExpr();
        // Error
        log.error("Unknown class found for building a valid JOIN Expression");
        return null;
    }
    
    /**
     * Constructor
     */
    public DBJoinExprEx(DBRowSet rset, DBCompareExpr cmp, DBJoinType joinType)
    {
        super(rset.getColumn(0), findFirstColumn(cmp), joinType);
        this.cmp = cmp;
    }
    
    public DBCompareExpr getJoinConstraint()
    {
        return cmp;
    }

    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        if ((context & CTX_NAME) != 0)
            left.getUpdateColumn().getRowSet().addSQL(buf, CTX_DEFAULT | CTX_ALIAS);
        if ((context & CTX_VALUE) != 0)
        { // Join Type
            switch(type)
            {
                case LEFT:  buf.append(" LEFT JOIN ");break;
                case INNER: buf.append(" INNER JOIN ");break;
                case RIGHT: buf.append(" RIGHT JOIN ");break;
                default:    buf.append(" JOIN "); // should not come here!
            }
            right.getUpdateColumn().getRowSet().addSQL(buf, CTX_DEFAULT | CTX_ALIAS);
            // compare equal
            buf.append(" ON ");
            cmp.addSQL(buf, CTX_DEFAULT);
            // Compare Expression
            if (compExpr != null)
            {
                buf.append(" AND ");
                compExpr.addSQL(buf, CTX_DEFAULT);
            }
        }
    }
}