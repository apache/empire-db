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
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class is used for performing special SQL functions. 
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBFuncExprEx extends DBColumnExpr
{
    protected final DBColumnExpr expr1;
    protected final DBColumnExpr expr2;

    protected final String       prefix;
    protected final String       middle;
    protected final String       postfix;

    /**
     * Constructs a new DBFuncExprEx object set the specified parameters to this object.
     * 
     * @param prefix the prefix of the function
     * @param expr1 the DBColumnExpr object
     * @param middle the middle of the function
     * @param expr2 the DBColumnExpr object
     * @param postfix the postfix of the function
     */
    public DBFuncExprEx(String prefix, DBColumnExpr expr1, String middle, DBColumnExpr expr2, String postfix)
    {
        this.prefix = prefix;
        this.expr1 = expr1;
        this.middle = middle;
        this.expr2 = expr2;
        this.postfix = postfix;
    }

    /**
     * Constructs a new DBFuncExprEx object Same as DBFuncExpr, 
     * but you can also set a column as the update column set the
     * specified parameters to this object.
     * 
     * @param prefix the prefix of the function
     * @param expr1 the DBColumnExpr object
     * @param postfix the postfix of the function
     */
    public DBFuncExprEx(String prefix, DBColumnExpr expr1, String postfix)
    {
        this.prefix = prefix;
        this.expr1 = expr1;
        this.middle = null;
        this.expr2 = null;
        this.postfix = postfix;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return expr1.getDatabase();
    }

    /**
     * Returns the data type of the DBColumnExpr object.
     * 
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return expr1.getDataType();
    }

    /**
     * Returns the column name.
     * 
     * @return the column name
     */
    @Override
    public String getName()
    {
        return (expr2!=null) ? expr1.getName() + "_" + expr2.getName() : expr1.getName();
    }

    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem = XMLUtil.addElement(parent, "function");
        if (prefix!=null)
            elem.setAttribute("prefix", prefix);
        if (middle!=null)
            elem.setAttribute("middle", middle);
        if (postfix!=null)
            elem.setAttribute("postfix", postfix);
        // add All Attributes
        if (attributes!=null)
            attributes.addXml(elem, flags);
        // add Expressions
        if (expr1!=null)
            expr1.addXml(elem, flags);
        if (expr2!=null)
            expr2.addXml(elem, flags);
        // add All Options
        if (options!=null)
            options.addXml(elem, flags);
        return elem;
    }

    /**
     * Returns the DBColunm object.
     * 
     * @return the DBColunm object
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    /**
     * Returns whether the function is an aggegation function<br>
     * that combines multiple rows to one result row.
     * 
     * @return true if the function is an aggregate or false otherwise
     */
    @Override
    public boolean isAggregate()
    {
        // Aggregates are not yet supported.
        return false;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        if (expr1!=null)
            expr1.addReferencedColumns(list);
        if (expr2!=null)
            expr2.addReferencedColumns(list);
    }

    /**
     * Creates the SQL-Command adds a function to the SQL-Command.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    { // Expression
        buf.append(prefix);
        expr1.addSQL(buf, context & ~CTX_ALIAS );
        if (middle != null)
        {
            buf.append(middle);
            expr2.addSQL(buf, context & ~CTX_ALIAS );
        }
        buf.append(postfix);
    }
}