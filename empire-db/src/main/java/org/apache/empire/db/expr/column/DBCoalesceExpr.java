/*
 * ESTEAM Software GmbH, 22.02.2022
 */
package org.apache.empire.db.expr.column;

import java.util.Set;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBExpr;
import org.apache.empire.dbms.DBSqlPhrase;

public class DBCoalesceExpr extends DBAbstractFuncExpr
{
    private final Object nullValue;
    
    public DBCoalesceExpr(DBColumnExpr expr, Object nullValue)
    {
        super(expr, expr.getUpdateColumn(), false, expr.getDataType());
        // set the null value
        this.nullValue = nullValue;
    }

    /**
     * Should we return true here?
     */
    @Override
    public boolean isWrapper()
    {   
        return false; // Should we return true here?
    }

    @Override
    public DBColumnExpr unwrap()
    {
        return expr;
    }

    @Override
    protected String getFunctionName()
    {
        return StringUtils.EMPTY;
    }
    
    /**
     * Returns the column name.
     */
    @Override
    public String getName()
    {
        return expr.getName();
    }
    
    /**
     * Returns the column enum type
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        return expr.getEnumType();
    }
    
    /**
     * Compares true if the other is also a coalesce expr on the same column
     */
    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        // Check for another Alias Expression
        if (other instanceof DBCoalesceExpr)
        {   // Compare with another alias expression
            DBColumnExpr otherExpr = ((DBCoalesceExpr)other).expr;
            return this.expr.equals(otherExpr);
        }
        return false;
    }

    /**
     * check if other function is the same and applies to the same column 
     * @param other
     * @return true if both functions are the same and on the same column or false otherwise
     */
    @Override
    public boolean isMutuallyExclusive(DBAbstractFuncExpr other)
    {
        return equals(other);
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        super.addReferencedColumns(list);
        // add referenced columns
        if (nullValue instanceof DBExpr)
            ((DBExpr)nullValue).addReferencedColumns(list);
    }

    @Override
    public void addSQL(StringBuilder sql, long context)
    {
        // Get the template
        String template = getDbms().getSQLPhrase(DBSqlPhrase.SQL_FUNC_COALESCE);
        // Add SQL
        super.addSQL(sql, template, new Object[] { nullValue }, context);
    }

}
