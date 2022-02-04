/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBCalcExpr;
import org.apache.empire.db.expr.column.DBCaseExpr;
import org.apache.empire.db.expr.column.DBConcatExpr;
import org.apache.empire.db.expr.column.DBConcatFuncExpr;
import org.apache.empire.db.expr.column.DBConvertExpr;
import org.apache.empire.db.expr.column.DBCountExpr;
import org.apache.empire.db.expr.column.DBDecodeExpr;
import org.apache.empire.db.expr.column.DBFuncExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.order.DBOrderByExpr;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.w3c.dom.Element;


/**
 * This class is the base class for all expressions that represent a single value.
 * It provides a set of factory functions for SQL functions.
 */
public abstract class DBColumnExpr extends DBExpr
    implements ColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
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
    @Override
    public abstract DataType getDataType();

    /**
     * Returns the column name for this column expression.
     * The name must contain only alphanumeric characters and the underscore.
     * For SQL functions this name may be generated. However subsequent calls to this function 
     * for the same object instance must return the same string.  
     *
     * @return the column name
     */
    @Override
    public abstract String getName();
    
    /**
     * Indicates whether this function is an aggregate (sum, min, max, avg, ...) or not
     * @return true if the column expression represents an aggregate
     */
    public abstract boolean isAggregate();

    /**
     * Returns the underlying physical column which was used for this expression
     * For functions involving none or more than one physical column this function return the first one
     * @return a column used for this expression
     */
    @Override
    public abstract DBColumn getSourceColumn();
    
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
     * @param flags currently not used
     * @return the newly created child element
     */
    public abstract Element addXml(Element parent, long flags);

    /**
     * @link {#org.apache.empire.commons.Unwrappable#isWrapper()}
     */
    @Override
    public boolean isWrapper()
    {   // Nope
        return false;
    }

    /**
     * @link {#org.apache.empire.commons.Unwrappable#unwrap()}
     */
    @Override
    public DBColumnExpr unwrap()
    {   // Noting to unwrap
        return this;
    }

    /**
     * Returns the value of a column attribute.
     * Column attributes are used to provide metadata for a column.
     * 
     * @param name the attribute name
     * @return value of the attribute if it exists or null otherwise
     */
    @Override
    public synchronized Object getAttribute(String name)
    {
        if (attributes != null && attributes.indexOf(name)>=0)
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
     * containing all possible field values.
     * 
     * @return the list of options
     */
    @Override
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
    @Override
    public final String getTitle()
    { 
        Object title = getAttribute(DBCOLATTR_TITLE);
        return StringUtils.toString(title);
    }

    /**
     * Sets the title attribute.
     * 
     * @param title the column title
     */
    public final void setTitle(String title)
    { 
        setAttribute(DBCOLATTR_TITLE, title);
    }

    /**
     * Returns the column control type.
     * The control type is a client specific name for the type of input control 
     * that should be used to display and edit values for this column. 
     * 
     * @return the column control type
     */
    @Override
    public final String getControlType()
    { 
        Object type = getAttribute(DBCOLATTR_TYPE);
        return StringUtils.toString(type);
    }

    /**
     * Sets the controlType attribute.
     * 
     * @param controlType the column control type
     */
    public final void setControlType(String controlType)
    { 
        setAttribute(DBCOLATTR_TYPE, controlType);
    }
    
    /**
     * Gets the Java bean property name for this column
     * i.e. ID   = employeeId
     *      DATE_OF_BIRTH = dateOfBirth
     *      
     * @return the name of the bean property used to get and set values 
     */
    @Override
    public String getBeanPropertyName()
    {
        if (beanPropertyName==null)
        {   // Compute bean property name
            String name = getName();
            if (name==null)
                return null; // no name provided!
            // compute name
            name = name.toLowerCase();        
            String res = "";
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
    public final DBColumnExpr as(DBColumn column)
    {
        return as(column.getName());
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
    public final DBCompareColExpr like(Object value)
    {
        return cmp(DBCmpType.LIKE, value);
    }

    /**
     * Creates and returns a new comparison object for the SQL "like" operator.
     * By converting column value and comparison value to upper case 
     * the like is effectively case insensitive.   
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr likeUpper(String value)
    { 
        DBValueExpr expr = new DBValueExpr(getDatabase(), value, DataType.VARCHAR);
        return new DBCompareColExpr(this.upper(), DBCmpType.LIKE, expr.upper());
    }

    /**
     * Creates and returns a new comparison object for the SQL "like" operator.
     * By converting column value and comparison value to lower case 
     * the like is effectively case insensitive.   
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr likeLower(String value)
    { 
        DBValueExpr expr = new DBValueExpr(getDatabase(), value, DataType.VARCHAR);
        return new DBCompareColExpr(this.lower(), DBCmpType.LIKE, expr.lower());
    }

    /**
     * Creates and returns a new comparison object for the SQL "like" operator. 
     *
     * @param value the Object value
     * @param escape the escape character
     * @return the new DBCompareColExpr object
     */
    public DBCompareColExpr like(String value, char escape)
    {
        DBValueExpr  textExpr = new DBValueExpr(getDatabase(), value, DataType.VARCHAR);
        DBFuncExpr escapeExpr = new DBFuncExpr(textExpr, DBSqlPhrase.SQL_FUNC_ESCAPE, new Object[] { String.valueOf(escape) }, getUpdateColumn(), false, DataType.VARCHAR );
        return cmp(DBCmpType.LIKE, escapeExpr);
    }

    /**
     * Creates and returns a new comparison object for the SQL "not like" operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr notLike(Object value)
    {
        return cmp(DBCmpType.NOTLIKE, value);
    }

    /**
     * Creates and returns a new comparison object for the SQL "=" (equal) operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr is(Object value)
    {
        return cmp(DBCmpType.EQUAL, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "&lt;&gt;" (not equal) operator.
     * 
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr isNot(Object value)
    {
        return cmp(DBCmpType.NOTEQUAL, value);
    }

    /**
     * Creates and returns an expression for the SQL "in" operator. 
     * 
     * @param list the values to compare this column with
     * @return a DBCompareColExpr for the "in" operator
     */
    public final DBCompareColExpr in(Collection<?> values)
    {
        if (values==null || values.isEmpty())
            return cmp(DBCmpType.EQUAL, null);
        // create expression
        return cmp(DBCmpType.IN, values);
    }

    /**
     * Creates and returns an expression for the SQL "in" operator. 
     * 
     * @param values the values to compare this column with
     * @return a DBCompareColExpr for the "in" operator
     */
    @SafeVarargs
    public final <T> DBCompareColExpr in(T... values)
    {
        if (values==null || values.length==0)
            return cmp(DBCmpType.EQUAL, null);
        // create expression
        return cmp(DBCmpType.IN, values);
    }

    /**
     * Creates and returns an expression for the SQL "not in" operator. 
     *
     * @param expr a database expression to provide a list of values
     * @return a DBCompareColExpr for the "not in" operator
     */
    public final DBCompareColExpr in(DBExpr expr)
    {
        return cmp(DBCmpType.IN, expr);
    }

    /**
     * Creates and returns an expression for the SQL "not in" operator. 
     *
     * @param values the values to compare this column with
     * @return a DBCompareColExpr for the "not in" operator
     */
    public final DBCompareColExpr notIn(Collection<?> values)
    {
        if (values==null || values.isEmpty())
            return cmp(DBCmpType.NOTEQUAL, null);
        // create expression
        return cmp(DBCmpType.NOTIN, values);
    }

    /**
     * Creates and returns an expression for the SQL "not in" operator. 
     *
     * @param values the values to compare this column with
     * @return a DBCompareColExpr for the "not in" operator
     */
    public final DBCompareColExpr notIn(Object... values)
    {
        if (values==null || values.length==0)
            return cmp(DBCmpType.NOTEQUAL, null);
        // create expression
        return cmp(DBCmpType.NOTIN, values);
    }

    /**
     * Creates and returns an expression for the SQL "not in" operator. 
     *
     * @param expr a database expression to provide a list of values
     * @return a DBCompareColExpr for the "not in" operator
     */
    public final DBCompareColExpr notIn(DBExpr expr)
    {
        return cmp(DBCmpType.NOTIN, expr);
    }

    /**
     * Creates and returns a new comparison object 
     * for the SQL "between" operator. 
     *
     * @param minValue the minimum value
     * @param maxValue the maximum value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr isBetween(Object minValue, Object maxValue)
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
    public final DBCompareColExpr isNotBetween(Object minValue, Object maxValue)
    {
        return cmp(DBCmpType.NOTBETWEEN, new Object[] { minValue, maxValue });
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "&gt;" (greater than) operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr isGreaterThan(Object value)
    {
        return cmp(DBCmpType.GREATERTHAN, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "&gt;=" (greater or equal) operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr isMoreOrEqual(Object value)
    {
        return cmp(DBCmpType.MOREOREQUAL, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "&lt;=" (less or equal) operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr isLessOrEqual(Object value)
    {
        return cmp(DBCmpType.LESSOREQUAL, value);
    }

    /**
     * Creates and returns a new comparison object
     * for the SQL "&lt;" (less than) operator.
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr isSmallerThan(Object value)
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
     * @param phrase the id of the SQL-phrase
     * @param params the params to replace in the template
     * @param isAggregate indicates whether the Function creates an aggregate
     * @param dataType the resulting data Type
     * @return the new DBCalcExpr object
     */
    protected DBColumnExpr getExprFromPhrase(DBSqlPhrase phrase, Object[] params, DBColumn updateColumn, boolean isAggregate, DataType dataType)
    {
        return new DBFuncExpr(this, phrase, params, updateColumn, isAggregate, dataType);
    }

    protected DBColumnExpr getExprFromPhrase(DBSqlPhrase phrase, Object[] params, DBColumn updateColumn, boolean isAggregate)
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
        return new DBFuncExpr(this, "(?)", null, getUpdateColumn(), false, getDataType());
    }

    /**
     * Creates a sql-expression for the nvl() or coalesce() function.
     * 
     * @param nullValue the Object value
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr coalesce(Object nullValue)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_COALESCE, new Object[] { nullValue }, getUpdateColumn(), false);
    }

    /**
     * Creates a sql-expression for the modulo or mod() function.
     * 
     * @param divisor the Object value
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr modulo(Object divisor)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_MODULO, new Object[] { divisor }, getUpdateColumn(), false);
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
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr substring(DBExpr pos)
    {   // Get Expression
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_SUBSTRING, new Object[] { pos }, getUpdateColumn(), false);
    }

    /**
     * Overloaded. @see substring(DBExpr pos)
     * 
     * @param pos the position number of the string
     * 
     * @return the new DBFuncExpr object
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
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr substring(DBExpr pos, DBExpr count)
    {   // Get Expression
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_SUBSTRINGEX, new Object[] { pos, count }, getUpdateColumn(), false);
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     * 
     * @param pos the position number of the string
     * @param count the length of the substring
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr substring(DBExpr pos, int count)
    {
        return substring(pos, new DBValueExpr(getDatabase(), count, DataType.INTEGER));
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     * 
     * @param pos the position number of the string
     * @param count the length of the substring
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr substring(int pos, DBExpr count)
    {
        return substring(new DBValueExpr(getDatabase(), pos, DataType.INTEGER), count);
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     * 
     * @param pos the position number of the string
     * @param count the length of the substring
     * 
     * @return the new DBFuncExpr object
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
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_REPLACE, new Object[] { match, replace }, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression for the reverse(...) function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr reverse()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_REVERSE, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression for the trim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr trim()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_TRIM, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression for the ltrim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr trimLeft()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_LTRIM, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a sql-expression for the rtrim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr trimRight()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_RTRIM, null, getUpdateColumn(), false);
    }
    
    /**
     * Creates and returns a function object which
     * converts the current expression to upper case.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr upper()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_UPPER, null, getUpdateColumn(), false);
    }

    /**
     * Creates and returns a function object which
     * converts the current expression to lower case.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr lower()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_LOWER, null, getUpdateColumn(), false);
    }

    /**
     * Formats a column-expression using a format string
     * This function is intended for formatting numbers.
     * Formatting any other data types may not supported and be database specific
     * @param format the format string. Beware: This is passed to the database "as is" and hence may be database specific.
     * @return a string expression representing the formatted value
     */
    public DBColumnExpr format(String format)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_FORMAT, new Object[] { format }, getUpdateColumn(), false, DataType.VARCHAR);
    }
    
    /**
     * Creates and returns a sql-expression that returns the string length of this expression.

     * @return the new DBFuncExpr object
     */
    public DBColumnExpr length()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_LENGTH, null, getUpdateColumn(), false, DataType.INTEGER);
    }

    /**
     * Creates and returns a sql-expression that returns the position of a string in the current column expression.
     * 
     * @param str the string to find the position of
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr indexOf(Object str)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_STRINDEX, new Object[] { str }, getUpdateColumn(), false, DataType.INTEGER);
    }

    /**
     * Creates and returns a sql-expression that returns the position of a string in the current column expression.
     * 
     * @param str the string to find the position of
     * @param fromPos the start position for the search
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr indexOf(Object str, DBExpr fromPos)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_STRINDEXFROM, new Object[] { str, fromPos }, getUpdateColumn(), false, DataType.INTEGER);
    }

    /**
     * Overloaded. @see indexOf(Object str, DBExpr fromPos) 
     * 
     * @param str the string to find the position of
     * @param fromPos the start position for the search
     * 
     * @return the new DBFuncExpr object
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
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_ABS, null, getUpdateColumn(), false);
    }

    /**
     * Create and returns an expression for the SQL-function floor()
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr floor()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_FLOOR, null, getUpdateColumn(), false);
    }

    /**
     * Create and returns an expression for the SQL-function ceil()
     * 
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr ceiling()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_CEILING, null, getUpdateColumn(), false);
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
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_ROUND, new Object[] { new Integer(decimals) }, getUpdateColumn(), false);
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
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_TRUNC, new Object[] { new Integer(decimals) }, getUpdateColumn(), false);
    }

    /**
     * Creates and returns an function object that
     * calculates the year of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public DBColumnExpr year()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_YEAR, null, null, false);
    }

    /**
     * Creates and returns an function object that
     * calculates the month of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public DBColumnExpr month()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_MONTH, null, null, false);
    }

    /**
     * Creates and returns an function object that
     * calculates the day of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public DBColumnExpr day()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_DAY, null, null, false);
    }

    /**
     * Creates and returns an aggregation function object
     * which calculates the sum for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr sum()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_SUM, null, null, true);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the minimum value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr min()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_MIN, null, null, true);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the maximum value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr max()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_MAX, null, null, true);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the average value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr avg()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_AVG, null, null, true);
    }

    /**
     * Creates and returns an expression for the SQL "count()" function
     * which returns the number of rows in the result set.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr count()
    {
        return new DBCountExpr(this, false);
    }

    /**
     * Creates and returns an expression for the SQL "count()" function
     * which returns the number of unique values in the result set.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr countDistinct()
    {
        return new DBCountExpr(this, true);
    }

    /**
     * Creates and returns a sql-expression that maps enum values by name or ordinal to their string representation 
     * 
     * @param enumType an enumType to decode 
     * @param otherwise the varchar value to take if no key matches the given expression
     * @return a DBDecodeExpr object
     */
    public DBColumnExpr decodeEnum(Class<? extends Enum<?>> enumType, String otherwise)
    {
        if (enumType==null || !enumType.isEnum())
            throw new InvalidArgumentException("enumType", enumType);
        // create map
        boolean byOrdinal = getDataType().isNumeric();
        Enum<?>[] items = enumType.getEnumConstants();
        Map<Object, String> enumMap = new LinkedHashMap<Object, String>(items.length);
        for (int i=0; i<items.length; i++)
        {   // key: ordinal (for numeric columns) or name (for CHAR columns)
            Object key = ObjectUtils.getEnumValue(items[i], byOrdinal);            
            enumMap.put(key, items[i].toString());
        }
        // Create the decode function
        return new DBDecodeExpr(this, enumMap, otherwise, DataType.VARCHAR);
    }

    /**
     * Creates and returns a sql-expression that maps enum values from name to ordinal
     * This is useful for sorting.
     * e.g. cmd.orderBy(SOME_EXPR.decodeSort(MyEnum.class, true) 
     * 
     * @param enumType an enumType to decode 
     * @param defaultToEnd true if non matching values (e.g. NULL) should be assigned the highest number, otherwise they get the lowest number
     * @return a DBDecodeExpr object
     */
    public DBColumnExpr decodeSort(Class<? extends Enum<?>> enumType, boolean defaultToEnd)
    {
        if (enumType==null || !enumType.isEnum())
            throw new InvalidArgumentException("enumType", enumType);
        // create map
        Enum<?>[] items = enumType.getEnumConstants();
        Map<String, Integer> enumMap = new LinkedHashMap<String, Integer>(items.length);
        int sortOffset = (defaultToEnd ? 0 : 1);
        for (int i=0; i<items.length; i++)
        {   
            int sortValue = items[i].ordinal();
            String value = ObjectUtils.getString(items[i]);
            enumMap.put(value, sortValue + sortOffset);
        }
        // Create the decode function
        int defaultValue = (defaultToEnd ? items.length : 0);
        return new DBDecodeExpr(this, enumMap, defaultValue, DataType.INTEGER);
    }
    
    /**
     * Creates and returns a sql-expression that compares the current column expression with 
     * a list of values and returns the corresponding alternative value.<BR>
     * 
     * @param valueMap a list of key values pairs used for decoding 
     * @param otherwise the value to take if no key matches the given expression
     * @return a DBDecodeExpr object
     */
    public DBColumnExpr decode(Map<?,?> valueMap, Object otherwise)
    {
        // Detect data type
        DataType dataType = DataType.UNKNOWN;
        if (otherwise!=null)
        {
            dataType = getDatabase().detectDataType(otherwise);
        }
        if (dataType==DataType.UNKNOWN)
        {
            for (Object v : valueMap.values())
            {
                dataType = getDatabase().detectDataType(v);
                if (dataType!=DataType.UNKNOWN)
                    break;
            }
        }
        // Create the decode function
        return new DBDecodeExpr(this, valueMap, otherwise, dataType);
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
    
    /**
     * Creates and returns a sql-expression for the SQL case-phrase.<br>
     * The result will be in the form:<br>
     * "case when [compExpr] then [this] else [otherwise] end" 
     * 
     * @param compExpr the condition for which the current column expression is returned
     * @param otherwise the value that is returned if the condition is false
     * @return a DBCaseExpr representing the SQL case phrase.
     */
    public final DBCaseExpr when(DBCompareExpr compExpr, Object otherwise)
    {
        DBColumnExpr elseExpr = null;
        if (otherwise instanceof DBColumnExpr)
        {   // A column Expression
            elseExpr = (DBColumnExpr)otherwise; 
        }
        else if (otherwise != null)
        {   // A constant value   
            elseExpr = new DBValueExpr(getDatabase(), otherwise, getDataType());
        }
        // Create DBCaseExpr
        return new DBCaseExpr(compExpr, this, elseExpr);
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
        return convertTo(DataType.VARCHAR);
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
        return convertTo(DataType.VARCHAR, format);
    }

    /**
     * Creates a new DBFuncExpr object that will convert
     * the current column to the destination data type specified.
     * 
     * @param dataType the destination data type
     * @param format optional destination format (usually a string)
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr convertTo(DataType dataType, Object format)
    {
        return new DBConvertExpr(this, dataType, format);
    }

    /**
     * Creates and returns a new DBFuncExpr object that will
     * convert the current column to the destination data type specified.
     * 
     * @param dataType Data type to which to convert the current expression to.
     * @return the new DBFuncExpr object
     */
    public final DBColumnExpr convertTo(DataType dataType)
    {
        return convertTo(dataType, null);
    }

    /**
     * creates a new DBOrderByExpr for ascending order 
     * <P>
     * @return the new DBOrderByExpr object for ascending order
     */
    public DBOrderByExpr asc()
    {
        return new DBOrderByExpr(this, false);
    }

    /**
     * creates a new DBOrderByExpr for descending order 
     * <P>
     * @return the new DBOrderByExpr object for descending order
     */
    public DBOrderByExpr desc()
    {
        return new DBOrderByExpr(this, true);
    }

    /**
     * concatenates a list of expressions to the current column 
     * @param concatExprs the expressions to concat
     * @return the concat expression
     */
    public DBColumnExpr concat(DBColumnExpr... concatExprs)
    {
        return new DBConcatFuncExpr(this, concatExprs);
    }

    /**
     * concatenates a list of expressions to the current column 
     * @param separator a string to insert between each of the expressions
     * @param concatExprs the expressions to concat
     * @return the concat expression
     */
    public DBColumnExpr concat(String separator, DBColumnExpr... concatExprs)
    {
        return new DBConcatFuncExpr(this, separator, concatExprs);
    }
 
    /**
     * returns a corresponding Java type for this expression
     * @param expr
     * @return
     */
    public Class<?> getJavaType()
    {
        return this.getDatabase().getColumnJavaType(this);
    }
    
}