/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db.oracle;

// Imports
import org.apache.empire.commons.Errors;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.expr.compare.DBCompareExpr;

/**
 * This class handles the special features of an oracle database.
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBCommandOracle extends DBCommand
{
    // Oracle Connect By / Start With
    protected DBCompareExpr connectBy  = null;
    protected DBCompareExpr startWith  = null;
    // optimizerHint
    protected String        optimizerHint  = null;

    /**
     * Constructs an oracle command object.
     * 
     * @see org.apache.empire.db.DBCommand
     * 
     * @param db the oracle database object this command belongs to
     */
    public DBCommandOracle(DBDatabase db)
    {
        super(db);
    }

    public String getOptimizerHint()
    {
        return optimizerHint;
    }

    public void setOptimizerHint(String optimizerHint)
    {
        this.optimizerHint = optimizerHint;
    }

    /**
     * @see DBCommand#clear()
     */
    @Override
    public void clear()
    {
        super.clear();
        // Clear oracle specific properties
        clearConnectBy();
        optimizerHint = null;
    }

    /**
     * Clears the connectBy Expression.
     */
    public void clearConnectBy()
    {
        connectBy = startWith = null;
    }

    public void connectByPrior(DBCompareExpr expr)
    {
        this.connectBy = expr;
    }

    public void startWith(DBCompareExpr expr)
    {
        this.startWith = expr;
    }

    /**
     * Creates the SQL statement the special characteristics of
     * the Oracle database are supported.
     * 
     * @param buf the SQL statment
     * @return true if the creation was successful
     */
    @Override
    public boolean getSelect(StringBuilder buf)
    {
        if (select == null)
            return error(Errors.ObjectNotValid, getClass().getName()); // invalid!
        // Prepares statement
        buf.append("SELECT ");
        if (optimizerHint != null)
        {
            // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            buf.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        if (selectDistinct)
            buf.append("DISTINCT ");
        // Add Select Expressions
        addListExpr(buf, select, CTX_ALL, ", ");
        // Join
        addFrom(buf);
        // Where
        addWhere(buf);
        // Connect By
        if (connectBy != null)
        {   // Add 'Connect By Prior' Expression
        	buf.append("\r\nCONNECT BY PRIOR ");
            connectBy.addSQL(buf, CTX_DEFAULT | CTX_NOPARENTHESES);
            // Start With
            if (startWith != null)
            {	// Add 'Start With' Expression
            	buf.append("\r\nSTART WITH ");
                startWith.addSQL(buf, CTX_DEFAULT);
            }
        }
        // Grouping
        addGrouping(buf);
        // Order
        if (orderBy != null)
        { // Having
            if (connectBy != null)
                buf.append("\r\nORDER SIBLINGS BY ");
            else
                buf.append("\r\nORDER BY ");
            // Add List of Order By Expressions
            addListExpr(buf, orderBy, CTX_DEFAULT, ", ");
        }
        // Done
        return success();
    }
    
    /**
     * Creates the delete SQL-Command.
     * 
     * @return the delete SQL-Command
     */
    @Override
    public String getDelete(DBTable table)
    {
        StringBuilder buf = new StringBuilder("DELETE ");
        if (optimizerHint != null)
        {   // Append an optimizer hint to the select statement e.g. SELECT /*+ RULE */
            buf.append("/*+ ").append(optimizerHint).append(" */ ");
        }
        buf.append("FROM ");
        table.addSQL(buf, CTX_FULLNAME);
        // Set Expressions
        if (where != null || having != null)
        { // add where condition
            buf.append("\r\nWHERE ");
            if (where != null)
                addListExpr(buf, where, CTX_NAME|CTX_VALUE, " AND ");
        }
        return buf.toString();
    }

}