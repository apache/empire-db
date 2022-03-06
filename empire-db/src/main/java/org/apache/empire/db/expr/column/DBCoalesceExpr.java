/*
 * ESTEAM Software GmbH, 22.02.2022
 */
package org.apache.empire.db.expr.column;

import java.util.Set;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.commons.Unwrappable;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBExpr;
import org.apache.empire.dbms.DBSqlPhrase;

public class DBCoalesceExpr extends DBAbstractFuncExpr implements Unwrappable<DBColumnExpr>
{
    private final Object nullValue;
    
    public DBCoalesceExpr(DBColumnExpr expr, Object nullValue)
    {
        super(expr, false, expr.getDataType());
        // set the null value
        this.nullValue = nullValue;
    }

    /**
     * This is a transparent wrapper
     */
    @Override
    public boolean isWrapper()
    {   // yep
        return true;
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
     * Returns true if other is equal to this expression  
     */
    @Override
    public boolean equals(Object other)
    {
        if (other==this)
            return true;
        // Check Type
        if (other instanceof DBCoalesceExpr)
        {   // Compare expressions
            DBColumnExpr otherExpr = ((DBCoalesceExpr)other).expr;
            return this.expr.equals(otherExpr);
        }
        return false;
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
