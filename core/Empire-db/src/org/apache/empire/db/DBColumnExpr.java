/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db;

// java
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBCalcExpr;
import org.apache.empire.db.expr.column.DBConcatExpr;
import org.apache.empire.db.expr.column.DBFuncExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.w3c.dom.Element;


/**
 * This class is the base class for building the SQL-Commands
 * <P>
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public abstract class DBColumnExpr extends DBExpr
    implements ColumnExpr
{
    // Predefined column expression attributes
    public static final String DBCOLATTR_TITLE     = "title";
    public static final String DBCOLATTR_TYPE      = "type";

    // Properties
    protected Attributes  attributes = null;
    protected Options     options = null;
    protected String      beanPropertyName = null;

    /**
     * Returns the data type of this column expression.
     * @see org.apache.empire.data.DataType
     *
     * @return the expressions data type
     */
    public abstract DataType getDataType();

    /**
     * Returns the column name for this column epxression.
     * The name must contain only alphanumerical charaters and the underscore.
     * For SQL functions this name may be generated. However subsequent calls to this function 
     * for the same object instance must return the same string.  
     *
     * @return the column name
     */
    public abstract String getName();
    
    /**
     * Indicates wheter this function is an aggregate (sum, min, max, avg, ...) or not
     * @return true if the column expression represents an aggregate
     */
    public abstract boolean isAggregate();

    /**
     * Returns the underlying physical column which may be used for updates.
     * For functions involving none or more than one physical column this function returns null.
     * @return the column to be used for updates if any.
     */
    public abstract DBColumn getUpdateColumn();

    /**
     * Add a description of this column with relevant metadata 
     * to the supplied parent XML Element.
     * 
     * @param parent the parent element to which to append the column description
     * @param flags currenly not used
     * @return the newly created child element
     */
    public abstract Element addXml(Element parent, long flags);

    /**
     * Returns the value of a column attribute.
     * Column attributes are used to provide metadata for a column.
     * 
     * @param name the attribute name
     * @return value of the attribute if it exists or null otherwise
     */
    public synchronized Object getAttribute(String name)
    {
        if (attributes != null && attributes.containsKey(name))
            return attributes.get(name);
        // Otherwise ask expression
        DBColumn column = getUpdateColumn();
        if (column==null || column==this)
            return null;
        return column.getAttribute(name);
    }

    /**
     * Sets the value of a column attribute.
     * 
     * @param name the attribute name
     * @param value the value of the attribute
     */
    public synchronized void setAttribute(String name, Object value)
    {
        if (attributes== null)
            attributes = new Attributes();
        attributes.set(name, value);
    }

    /**
     * Returns the list of options for this column
     * containing all possbile field values.
     * 
     * @return the list of options
     */
    public synchronized Options getOptions()
    {
        if (options != null)
            return options;
        // Otherwise ask expression
        DBColumn column = getUpdateColumn();
        if (column==null || column==this)
            return null;
        return column.getOptions();
    }

    /**
     * Sets the options for this column indicating all valid values.
     * 
     * @param options the list of options
     */
    public synchronized void setOptions(Options options)
    {
        this.options = options;
    }

    /**
     * Returns the title attribute.
     * 
     * @return the column title
     */
    public final String getTitle()
    { 
        Object title = getAttribute(DBCOLATTR_TITLE);
        return StringUtils.toString(title);
    }

    /**
     * Sets the title attribute.
     */
    public final void setTitle(String title)
    { 
        setAttribute(DBCOLATTR_TITLE, title);
    }

    /**
     * Returns the columns control type.
     * The control type is a client specific name for the type of input control 
     * that should be used to display and edit values for this column. 
     * 
     * @return the columns control type
     */
    public final String getControlType()
    { 
        Object type = getAttribute(DBCOLATTR_TYPE);
        return StringUtils.toString(type);
    }

    /**
     * Sets the title attribute.
     */
    public final void setControlType(String controlType)
    { 
        setAttribute(DBCOLATTR_TYPE, controlType);
    }

    /**
     * Returns the source column.
     * This is equivalent to the "Update Column"
     * see getUpdateColumn()
     */
    public final Column getSourceColumn()
    {
        return getUpdateColumn();
    }
    
    /**
     * Gets the Java bean property name for this column
     * i.e. EMPLOYEE_ID   = employeeId
     *      DATE_OF_BIRTH = dateOfBirth
     *      
     * @return the name of the bean property used to get and set values 
     */
    public String getBeanPropertyName()
    {
        if (beanPropertyName==null)
        {   // Compute bean property name
            String res = "";
            String name = getName().toLowerCase();
            int beg=0;
            while (beg<name.length())
            {
                int end = name.indexOf('_', beg);
                if (end<0)
                    end = name.length();
                // assemble
                if (end>beg)
                {
                    if (beg==0)
                    {   // begin with all lower cases
                        res = name.substring(beg, end);
                    }
                    else
                    {   // add word where first letter is upper case 
                        res += name.substring(beg, beg+1).toUpperCase();
                        if (end-beg>1)
                        {
                            res += name.substring(beg+1, end);
                        }
                    }
                }
                // next
                beg = end + 1;
            }
            // Result
            beanPropertyName = res;
        }
        return beanPropertyName;
    }

    /**
     * Sets the Java bean property name for this column.
     *
     * @param propertyName
     */
    public void setBeanPropertyName(String propertyName)
    {
        this.beanPropertyName = propertyName; 
    }

    /**
     * Creates a new DBConcatExpr object with the specified value.
     *
     * @param value an Object value
     * @return the new DBConcatExpr object
     */
    public DBColumnExpr append(Object value)
    {
        return new DBConcatExpr(this, value);
    }

    /**
     * creates a new DBAliasExpr which renames the current expression to the supplied name. 
     * <P>
     * @param alias the alias name
     * @return the new DBAliasExpr object
     */
    public DBColumnExpr as(String alias)
    {
        return new DBAliasExpr(this, alias);
    }

    /**
     * creates a new DBAliasExpr which renames the current expression to the name of the supplied column.
     * <P>
     * @param column the column whose name serves as an alias for the current expression
     * @return the new DBAliasExpr object
     */
    public DBColumnExpr as(DBColumn column)
    {
        return new DBAliasExpr(this, column.getName());
    }

    /**
     * Creates and returns a new comparison object for the given comparison operator and value.
     * 
     * @param op the comparison operator
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr cmp(DBCmpType op, Object value)
    {
        return new DBCompareColExpr(this, op, value);
    }

    /**
     * Creates and returns a new comparison object for the SQL "like" operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr like(Object value)
    {
        return cmp(DBCmpType.LIKE, value);
    }

    /**
     * Creates and returns a new comparison object for the SQL "like" operator.
     * By converting column value and comparision value to upper case 
     * the like is effectively case insensitive.   
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr likeUpper(String value)
    { 
        DBValueExpr expr = new DBValueExpr(getDatabase(), value, DataType.TEXT);
        return new DBCompareColExpr(this.upper(), DBCmpType.LIKE, expr.upper());
    }

    /**
     * Creates and returns a new comparison object for the SQL "like" operator.
     * By converting column value and comparision value to lower case 
     * the like is effectively case insensitive.   
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr likeLower(String value)
    { 
        DBValueExpr expr = new DBValueExpr(getDatabase(), value, DataType.TEXT);
        return new DBCompareColExpr(this.lower(), DBCmpType.LIKE, expr.lower());
    }

    /**
     * Creates and returns a new comparison object for the SQL "like" operator. 
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr like(String value, char escape)
    {
        DBValueExpr textExpr = new DBValueExpr(getDatabase(), value, DataType.TEXT);
        return cmp(DBCmpType.LIKE, textExpr.getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_ESCAPE,
                                                        new Object[] { String.valueOf(escape) }, getUpdateColumn(), false));
    }

    /**
     * Creates and returns a new comparison object for the SQL "not like" operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr notLike(Object value)
    {
        return cmp(DBCmpType.NOTLIKE, value);
    }

    /**
     * Creates and returns a new comparison object for the SQL "=" (equal) operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr is(Object value)
    {
        return cmp(DBCmpType.EQUAL, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "<>" (not equal) operator.
     * 
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr isNot(Object value)
    {
        return cmp(DBCmpType.NOTEQUAL, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "in" operator. 
     * 
     * @param value the int value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr in(Object value)
    {
        return cmp(DBCmpType.IN, listToArray(value));
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "not in" operator.
     *
     * @param value the int value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr notIn(Object value)
    {
        return cmp(DBCmpType.NOTIN, listToArray(value));
    }

    /**
     * Creates and returns a new comparison object 
     * for the SQL "between" operator. 
     *
     * @param minValue the minimum value
     * @param maxValue the maximum value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr isBetween(Object minValue, Object maxValue)
    {
        return cmp(DBCmpType.BETWEEN, new Object[] { minValue, maxValue });
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "not between" operator. 
     *
     * @param minValue the minimum value
     * @param maxValue the maximum value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr isNotBetween(Object minValue, Object maxValue)
    {
        return cmp(DBCmpType.NOTBETWEEN, new Object[] { minValue, maxValue });
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL ">" (greater than) operator. 
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr isGreaterThan(Object value)
    {
        return cmp(DBCmpType.GREATERTHAN, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL ">=" (greater or equal) operator. 
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr isMoreOrEqual(Object value)
    {
        return cmp(DBCmpType.MOREOREQUAL, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "<=" (less or equal) operator. 
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr isLessOrEqual(Object value)
    {
        return cmp(DBCmpType.LESSOREQUAL, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "<" (less than) operator. 
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr isSmallerThan(Object value)
    {
        return cmp(DBCmpType.LESSTHAN, value);
    }
    
    // ------- calculations -------
    
    /**
     * Creates and returns a new calculation object
     * for the SQL "*" (multiply) operator. 
     *
     * @param value the multiply value
     * @return the new DBCalcExpr object
     */
    public DBCalcExpr multiplyWith(Object value)
    {
        return new DBCalcExpr(this, "*", value);
    }

    /**
     * Creates and returns a new calculation object
     * for the SQL "/" (divide) operator. 
     *
     * @param value the divide value
     * @return the new DBCalcExpr object
     */
    public DBCalcExpr divideBy(Object value)
    {
        return new DBCalcExpr(this, "/", value);
    }

    /**
     * Creates and returns a new calculation object
     * for the SQL "+" (plus) operator. 
     *
     * @param value the summate value
     * @return the new DBCalcExpr object
     */
    public DBCalcExpr plus(Object value)
    {
        return new DBCalcExpr(this, "+", value);
    }

    /**
     * Creates and returns a new calculation object
     * for the SQL "-" (minus) operator. 
     *
     * @param value the subtract value
     * @return the new DBCalcExpr object
     */
    public DBCalcExpr minus(Object value)
    {
        return new DBCalcExpr(this, "-", value);
    }

    /**
     * Creates and returns a new calculation object
     * for either the SQL "+" (plus) or "-" (minus) operator
     * depending on whether the supplied integer value is positive or negative.
     *
     * @param value the subtract value
     * @return the new DBCalcExpr object
     */
    public DBCalcExpr plus(int value)
    {
        return (value >= 0) ? new DBCalcExpr(this, "+", new Integer(value)) : new DBCalcExpr(this, "-", new Integer(-value));
    }

    /**
     * Creates and returns a new calculation object
     * for either the SQL "+" (plus) or "-" (minus) operator
     * depending on whether the supplied integer value is negative or positive.
     *
     * @param value the subtract value
     * @return the new DBCalcExpr object
     */
    public DBCalcExpr minus(int value)
    {
        return (value >= 0) ? new DBCalcExpr(this, "-", new Integer(value)) : new DBCalcExpr(this, "+", new Integer(-value));
    }

    // ----------------------------------------------------------
    // --------------------- DBFuncExpr -------------------------
    // ----------------------------------------------------------
    
    /**
     * Creates a new DBFuncExpr from a given SQL-PRHASE and
     * optional additional parameters.
     *
     * @param phrase the enum-id of the phrase (see DBDatabaseDriver.SQL_...)
     * @param params the params to replace in the template
     * @param isAggregate indicates whether the Function creates an aggregate
     * @param dataType the resulting data Type
     * @return the new DBCalcExpr object
     */
    public DBColumnExpr getExprFromPhrase(int phrase, Object[] params, DBColumn updateColumn, boolean isAggregate, DataType dataType)
    {
        DBDatabaseDriver driver = getDatabase().getDriver();
        String template = driver.getSQLPhrase(phrase);
        if (params != null)
        {
            for (int i = 0; i < params.length; i++)
            { // Replace Params
                // String test  =(params[i] != null) ? params[i].toString() : "";
                String value = getObjectValue(this, params[i], DBExpr.CTX_DEFAULT, ",");
                // template = template.replaceAll("\\{" + String.valueOf(i) + "\\}", value);
                template = StringUtils.replaceAll(template, "{"+ String.valueOf(i) + "}", value);
            }
        }
        return new DBFuncExpr(this, template, updateColumn, isAggregate, dataType);
    }

    public DBColumnExpr getExprFromPhrase(int phrase, Object[] params, DBColumn updateColumn, boolean isAggregate)
    {
        return getExprFromPhrase(phrase, params, updateColumn, isAggregate, getDataType());
    }

    /**
     * Creates and returns a function object which
     * encloses the current expression in parenthesis.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr parenthesis()
    { 
        return new DBFuncExpr(this, "(?)", getUpdateColumn(), false, getDataType());
    }

    /**
     * Creates a sql-expression for the nvl() or coalesce() function.
     * 
     * @param nullValue the Object value
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr coalesce(Object nullValue)
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_COALESCE, new Object[] { nullValue }, getUpdateColumn(), false);
    }

    /**
     * Creates a sql-expression for the nvl() or coalesce() function.
     * 
     * @param nullValue the int value
     * @return the new DBFuncExpr object
     *
     * @deprecated Outdated oracle syntax - use coalesce instead
     */
    @Deprecated
    public DBColumnExpr nvl(Object nullValue)
    {
        return coalesce(nullValue);
    }

    /**
     * Creates and returns a sql-expression for the substring(...) function.
     * 
     * @param pos the position number of the string
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr substring(DBExpr pos)
    {   // Get Expression
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_SUBSTRING, new Object[] { pos }, getUpdateColumn(), false);
    }

    /**
     * Overloaded. @see substring(DBExpr pos)
     */
    public DBColumnExpr substring(int pos)
    {
        return substring(new DBValueExpr(getDatabase(), pos, DataType.INTEGER));
    }

    /**
     * Creates and returns a sql-expression for the substring(...) function.
     * 
     * @param pos the position number of the string
     * @param count the length of the substring
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr substring(DBExpr pos, DBExpr count)
    {   // Get Expression
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_SUBSTRINGEX, new Object[] { pos, count }, getUpdateColumn(), false);
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     */
    public DBColumnExpr substring(DBExpr pos, int count)
    {
        return substring(pos, new DBValueExpr(getDatabase(), count, DataType.INTEGER));
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     */
    public DBColumnExpr substring(int pos, DBExpr count)
    {
        return substring(new DBValueExpr(getDatabase(), pos, DataType.INTEGER), count);
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     */
    public DBColumnExpr substring(int pos, int count)
    {
        return substring(new DBValueExpr(getDatabase(), pos, DataType.INTEGER), 
                         new DBValueExpr(getDatabase(), count, DataType.INTEGER));
    }
    
    /**
     * Creates and returns a sql-expression for the replace(...) function.
     * 
     * @param match string to replace
     * @param replace string with replacement
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr replace(Object match, Object replace)
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_REPLACE, new Object[] { match, replace }, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression for the reverse(...) function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr reverse()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_REVERSE, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression for the trim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr trim()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_TRIM, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression for the ltrim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr trimLeft()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_LTRIM, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression for the rtrim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr trimRight()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_RTRIM, null, getUpdateColumn(), false);
    }
    
    /**
     * Creates and returns a function object which
     * converts the current expression to upper case.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr upper()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_UPPER, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a function object which
     * converts the current expression to lower case.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr lower()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_LOWER, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression that returns the string length of this expression.

     * @return the new DBFuncExpr object
     */
    public DBColumnExpr length()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_LENGTH, null, getUpdateColumn(), false, DataType.INTEGER);
    }

    /**
     * Creates and returns a sql-expression that returns the position of a string in the current column expression.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr indexOf(Object str)
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_STRINDEX, new Object[] { str }, getUpdateColumn(), false, DataType.INTEGER);
    }

    /**
     * Creates and returns a sql-expression that returns the position of a string in the current column expression.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr indexOf(Object str, DBExpr fromPos)
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_STRINDEXFROM, new Object[] { str, fromPos }, getUpdateColumn(), false, DataType.INTEGER);
    }

    /**
     * Overloaded. @see indexOf(Object str, DBExpr fromPos) 
     */
    public DBColumnExpr indexOf(Object str, int fromPos)
    {
        return indexOf(str, new DBValueExpr(getDatabase(), fromPos, DataType.INTEGER));
    }

    /**
     * Creates and returns a sql-expression for the absolute abs() function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr abs()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_ABS, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns an function object that
     * rounds a number espression with the given decimals.
     * 
     * @param decimals the number of decimal to which to truncate the current value
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr round(int decimals)
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_ROUND, new Object[] { new Integer(decimals) }, getUpdateColumn(), false);
    }

    /**
     * Creates and returns an function object that
     * truncates a number espression with the given decimals.
     * 
     * @param decimals the number of decimal to which to truncate the current value
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr trunc(int decimals)
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_TRUNC, new Object[] { new Integer(decimals) }, getUpdateColumn(), false);
    }

    /**
     * Creates and returns an function object that
     * calculates the year of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public DBColumnExpr year()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_YEAR, null, null, false);
    }

    /**
     * Creates and returns an function object that
     * calculates the month of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public DBColumnExpr month()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_MONTH, null, null, false);
    }

    /**
     * Creates and returns an function object that
     * calculates the day of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public DBColumnExpr day()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_DAY, null, null, false);
    }

    /**
     * Creates and returns an aggregation function object
     * which calculates the sum for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr sum()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_SUM, null, null, true);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the minimum value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr min()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_MIN, null, null, true);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the maximum value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr max()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_MAX, null, null, true);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the average value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr avg()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_AVG, null, null, true);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the number of rows in a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr count()
    {
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_COUNT, null, null, true);
    }

    /**
     * Creates and returns a sql-expression for the replace(...) function.
     * 
     * @param list 
     * @param otherwise
     * @return the new DBFuncExpr object
     */
    @SuppressWarnings("unchecked")
    public DBColumnExpr decode(Map list, Object otherwise)
    {
        DBDatabaseDriver driver = getDatabase().getDriver();
        StringBuilder inner = new StringBuilder();
        // Generate parts
        for (Iterator i = list.keySet().iterator(); i.hasNext();)
        {
            Object key = i.next();
            Object val = list.get(key);

            String part = driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE_PART);
            // part = part.replaceAll("\\{0\\}", getObjectValue(this, key, DBExpr.CTX_DEFAULT, ""));
            part = StringUtils.replaceAll(part, "{0}", getObjectValue(this, key, DBExpr.CTX_DEFAULT, ""));

            // part = part.replaceAll("\\{1\\}", getObjectValue(this, val, DBExpr.CTX_DEFAULT, ""));
            part = StringUtils.replaceAll(part, "{1}", getObjectValue(this, val, DBExpr.CTX_DEFAULT, ""));

            inner.append(driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE_SEP));
            inner.append(part);
        }
        // Generate other
        if (otherwise != null)
        { // Else
            String other = driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE_ELSE);

            // other = other.replaceAll("\\{0\\}", getObjectValue(this, otherwise, DBExpr.CTX_DEFAULT, ""));
            other = StringUtils.replaceAll(other, "{0}", getObjectValue(this, otherwise, DBExpr.CTX_DEFAULT, ""));

            inner.append(driver.getSQLPhrase(DBDatabaseDriver.SQL_FUNC_DECODE_SEP));
            inner.append(other);
        }
        DBValueExpr param = new DBValueExpr(getDatabase(), inner, DataType.UNKNOWN); 
        return getExprFromPhrase(DBDatabaseDriver.SQL_FUNC_DECODE, new Object[] { param }, null, false);
    }

    public final DBColumnExpr decode(Object key1, Object value1, Object otherwise)
    {
        Map<Object, Object> list = new HashMap<Object, Object>();
        list.put(key1, value1);
        return decode(list, otherwise);
    }

    public final DBColumnExpr decode(Object key1, Object value1, Object key2, Object value2, Object otherwise)
    {
        Map<Object, Object> list = new HashMap<Object, Object>();
        list.put(key1, value1);
        list.put(key2, value2);
        return decode(list, otherwise);
    }

    public final DBColumnExpr decode(Object key1, Object value1, Object key2, Object value2, Object key3, Object value3,
                               Object otherwise)
    {
        Map<Object, Object> list = new HashMap<Object, Object>();
        list.put(key1, value1);
        list.put(key2, value2);
        list.put(key3, value3);
        return decode(list, otherwise);
    }

    public final DBColumnExpr decode(Options options, Object otherwise)
    {
        int size = options.size() + (otherwise!=null ? 1 : 0);
        Map<Object, Object> list = new HashMap<Object, Object>(size);
        for (OptionEntry e : options)
            list.put(e.getValue(), e.getText());
        return decode(list, otherwise);
    }

    public final DBColumnExpr decode(Options options)
    {
        return decode(options, null);
    }

    // ----------------------------------------------------------
    // --------------------- Conversion -------------------------
    // ----------------------------------------------------------
    
    /**
     * Creates a new DBFuncExpr object (to_char SQL statement)
     * with the parameters prefix = "to_char(" and postfix = ")"
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr toChar()
    {
        return convertTo(DataType.TEXT);
    }

    /**
     * Creates a new DBFuncExpr object (to_char SQL statement)
     * with the parameters prefix = "to_char(" and postfix = ", *
     * '"+format+"')"
     * 
     * @param format the string value
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr toChar(String format)
    {
        return convertTo(DataType.TEXT, format);
    }

    /**
     * Creates a new DBFuncExpr object that will convert
     * the current column to the destination data type specivied.
     * 
     * @param dataType the destination data type
     * @param format optional destination format (usually a string)
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr convertTo(DataType dataType, Object format)
    {
        DBDatabaseDriver driver = getDatabase().getDriver();
        return new DBFuncExpr(this, driver.getConvertPhrase(dataType, getDataType(), format), getUpdateColumn(), false, dataType);
    }

    /**
     * Creates and returns a new DBFuncExpr object that will
     * convert the current column to the destination data type specivied.
     * 
     * @param dataType Data type to which to convert the current expression to.
     * @return the new DBFuncExpr object
     */
    public final DBColumnExpr convertTo(DataType dataType)
    {
        return convertTo(dataType, null);
    }


    // get object Array from List
    private Object listToArray(Object value)
    {   // Check wether value is a list
        /*
        if (value != null && value instanceof java.util.List)
        { // Convert List to array
            return ((List)value).toArray();
        }
        */
        return value;
    }
    
}