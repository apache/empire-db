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
import org.apache.empire.commons.ClassUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.data.DataType;
import org.apache.empire.db.expr.column.DBAliasExpr;
import org.apache.empire.db.expr.column.DBCalcExpr;
import org.apache.empire.db.expr.column.DBCaseExpr;
import org.apache.empire.db.expr.column.DBCaseWhenExpr;
import org.apache.empire.db.expr.column.DBCoalesceExpr;
import org.apache.empire.db.expr.column.DBConcatExpr;
import org.apache.empire.db.expr.column.DBConcatFuncExpr;
import org.apache.empire.db.expr.column.DBConvertExpr;
import org.apache.empire.db.expr.column.DBCountExpr;
import org.apache.empire.db.expr.column.DBDecodeExpr;
import org.apache.empire.db.expr.column.DBFuncExpr;
import org.apache.empire.db.expr.column.DBParenthesisExpr;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.expr.compare.DBCompareColExpr;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.db.expr.join.DBColumnJoinExpr;
import org.apache.empire.db.expr.order.DBOrderByExpr;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.w3c.dom.Element;

/**
 * This class is the base class for all expressions that represent a single value.
 * It provides a set of factory functions for SQL functions.
 */
public abstract class DBColumnExpr extends DBExpr
    implements ColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;

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
     * Returns the underlying rowset containing this column
     * For functions involving none or more than one physical column this function return the first one
     * @return a column used for this expression
     */
    public abstract DBRowSet getRowSet();
    
    /**
     * Returns the underlying physical column.
     * For functions involving none or more than one physical column this function returns null.
     * @return the underlying column
     */
    @Override
    public abstract DBColumn getUpdateColumn();

    /**
     * Use getUpdateColumn() instead!
     */
    @Override
    @Deprecated
    public final DBColumn getSourceColumn()
    {
        return getUpdateColumn();
    }
    
    /**
     * Indicates whether this function is an aggregate (sum, min, max, avg, ...) or not
     * @return true if the column expression represents an aggregate
     */
    public abstract boolean isAggregate();
    
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
     * returns an expression that renames the column with its alias name
     * @return the rename expression
     */
    public DBAliasExpr qualified()
    {
        return this.as(getName());
    }

    /**
     * Returns the value of a column attribute.
     * Column attributes are used to provide metadata for a column.
     * 
     * @param name the attribute name
     * @return value of the attribute if it exists or null otherwise
     */
    @Override
    public Object getAttribute(String name)
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
     * Same as Column.setAttribute but with different name to avoid name clash 
     * @param <T> the column expression type
     * @param name the attribute name
     * @param value the value of the attribute
     * @return returns self (this)
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T extends ColumnExpr> T setAttribute(String name, Object value)
    {
        if (attributes== null)
            attributes = new Attributes();
        if (value!=null)
            attributes.set(name, value);
        else
            attributes.remove(name);
        return (T)this;
    }

    /**
     * Returns the list of options for this column
     * containing all possible field values.
     * 
     * @return the list of options
     */
    @Override
    public Options getOptions()
    {
        if (options != null)
            return options;
        // Otherwise try column
        DBColumn column = getUpdateColumn();
        if (column==null || column==this)
            return null;
        return column.getOptions();
    }

    /**
     * Sets the options for this column indicating all valid values.
     * @param <T> the column expression type
     * @param options the list of options
     * @return returns self (this)
     */
    @SuppressWarnings("unchecked")
    public <T extends DBColumnExpr> T setOptions(Options options)
    {
        this.options = options;
        return (T)this;
    }

    /**
     * Returns the title attribute.
     * 
     * @return the column title
     */
    @Override
    public final String getTitle()
    { 
        Object title = getAttribute(Column.COLATTR_TITLE);
        return StringUtils.toString(title);
    }

    /**
     * Sets the title attribute.
     * @param <T> the column expression type
     * @param title the column title
     * @return returns self (this)
     */
    public <T extends DBColumnExpr> T setTitle(String title)
    { 
        return setAttribute(Column.COLATTR_TITLE, title);
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
        Object type = getAttribute(Column.COLATTR_TYPE);
        return StringUtils.toString(type);
    }

    /**
     * Sets the controlType attribute.
     * @param <T> the column expression type
     * @param controlType the column control type
     * @return returns self (this)
     */
    public final <T extends DBColumnExpr> T setControlType(String controlType)
    { 
        return setAttribute(Column.COLATTR_TYPE, controlType);
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
            beanPropertyName = StringUtils.toCamelCase(getName(), false); // Compute bean property name
        return beanPropertyName;
    }

    /**
     * Sets the Java bean property name for this column.
     * @param <T> the column expression type
     * @param propertyName the property name
     * @return returns self (this)
     */
    @SuppressWarnings("unchecked")
    public <T extends DBColumnExpr> T setBeanPropertyName(String propertyName)
    {
        this.beanPropertyName = propertyName; 
        return (T)this;
    }

    /**
     * creates a new DBAliasExpr which renames the current expression to the supplied name. 
     * <P>
     * @param alias the alias name
     * @return the new DBAliasExpr object
     */
    public DBAliasExpr as(String alias)
    {
        return new DBAliasExpr(this, alias);
    }

    /**
     * creates a new DBAliasExpr which renames the current expression to the name of the supplied column.
     * <P>
     * @param column the column whose name serves as an alias for the current expression
     * @return the new DBAliasExpr object
     */
    public final DBAliasExpr as(DBColumn column)
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
        DBValueExpr expr = getDatabase().getValueExpr(value, DataType.VARCHAR);
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
        DBValueExpr expr = getDatabase().getValueExpr(value, DataType.VARCHAR);
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
        DBValueExpr  textExpr = getDatabase().getValueExpr(value, DataType.VARCHAR);
        DBFuncExpr escapeExpr = new DBFuncExpr(textExpr, DBSqlPhrase.SQL_FUNC_ESCAPE, new Object[] { String.valueOf(escape) }, DataType.VARCHAR);
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
     * Creates and returns a new comparison object for the "contains(column, val)" comparator
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr contains(Object value)
    {
        return cmp(DBCmpType.CONTAINS, value);
    }

    /**
     * Creates and returns a new comparison object for the "not contains(column, val)" comparator
     *
     * @param value the Object value
     * @return the new DBCompareColExpr object
     */
    public final DBCompareColExpr notContains(Object value)
    {
        return cmp(DBCmpType.NOTCONTAINS, value);
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
     * @param values the values to compare this column with
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
     * @param <T> the value type
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
     * @param <T> the value type
     * @param values the values to compare this column with
     * @return a DBCompareColExpr for the "not in" operator
     */
    @SafeVarargs
    public final <T> DBCompareColExpr notIn(T... values)
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

    /**
     * Creates a new DBConcatExpr object with the specified value.
     *
     * @param value an Object value
     * @return the new DBConcatExpr object
     */
    public DBConcatExpr append(Object value)
    {
        return new DBConcatExpr(this, value);
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
     * @param dataType the resulting data Type
     * @return the new DBCalcExpr object
     */
    protected DBFuncExpr getExprFromPhrase(DBSqlPhrase phrase, Object[] params, DataType dataType)
    {
        return new DBFuncExpr(this, phrase, params, dataType);
    }

    protected final DBFuncExpr getExprFromPhrase(DBSqlPhrase phrase, Object[] params)
    {
        return getExprFromPhrase(phrase, params, getDataType());
    }

    /**
     * Creates and returns a function object which
     * encloses the current expression in parenthesis.
     *
     * @return the new DBFuncExpr object
     */
    public DBColumnExpr parenthesis()
    { 
        return new DBParenthesisExpr(this);
    }

    /**
     * Creates a sql-expression for the nvl() or coalesce() function.
     * 
     * @param nullValue the alternative value when this expression is null
     * @return the coalesce expression
     */
    public DBCoalesceExpr coalesce(Object nullValue)
    {
        return new DBCoalesceExpr(this, nullValue);
    }

    /**
     * Creates a sql-expression for the modulo or mod() function.
     * 
     * @param divisor the Object value
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr modulo(Object divisor)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_MOD, new Object[] { divisor });
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
    public final DBFuncExpr substring(DBExpr pos)
    {   // Get Expression
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_SUBSTRING, new Object[] { pos });
    }

    /**
     * Overloaded. @see substring(DBExpr pos)
     * 
     * @param pos the position number of the string
     * 
     * @return the new DBFuncExpr object
     */
    public DBFuncExpr substring(int pos)
    {
        return substring(getDatabase().getValueExpr(pos, DataType.INTEGER));
    }

    /**
     * Creates and returns a sql-expression for the substring(...) function.
     * 
     * @param pos the position number of the string
     * @param count the length of the substring
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr substring(DBExpr pos, DBExpr count)
    {   // Get Expression
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_SUBSTRINGEX, new Object[] { pos, count });
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     * 
     * @param pos the position number of the string
     * @param count the length of the substring
     * 
     * @return the new DBFuncExpr object
     */
    public DBFuncExpr substring(DBExpr pos, int count)
    {
        return substring(pos, getDatabase().getValueExpr(count, DataType.INTEGER));
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     * 
     * @param pos the position number of the string
     * @param count the length of the substring
     * 
     * @return the new DBFuncExpr object
     */
    public DBFuncExpr substring(int pos, DBExpr count)
    {
        return substring(getDatabase().getValueExpr(pos, DataType.INTEGER), count);
    }

    /**
     * Overloaded. @see substring(DBExpr pos, DBExpr count)
     * 
     * @param pos the position number of the string
     * @param count the length of the substring
     * 
     * @return the new DBFuncExpr object
     */
    public DBFuncExpr substring(int pos, int count)
    {
        return substring(getDatabase().getValueExpr(pos, DataType.INTEGER), 
                         getDatabase().getValueExpr(count, DataType.INTEGER));
    }
    
    /**
     * Creates and returns a sql-expression for the replace(...) function.
     * 
     * @param match string to replace
     * @param replace string with replacement
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr replace(Object match, Object replace)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_REPLACE, new Object[] { match, replace });
    }

    /**
     * Creates and returns a sql-expression for the reverse(...) function.
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr reverse()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_REVERSE, null);
    }

    /**
     * Creates and returns a sql-expression for the trim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr trim()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_TRIM, null);
    }

    /**
     * Creates and returns a sql-expression for the ltrim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr trimLeft()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_LTRIM, null);
    }

    /**
     * Creates and returns a sql-expression for the rtrim() function.
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr trimRight()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_RTRIM, null);
    }
    
    /**
     * Creates and returns a function object which
     * converts the current expression to upper case.
     *
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr upper()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_UPPER, null);
    }

    /**
     * Creates and returns a function object which
     * converts the current expression to lower case.
     *
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr lower()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_LOWER, null);
    }

    /**
     * Formats a column-expression using a format string
     * This function is intended for formatting numbers.
     * Formatting any other data types may not supported and be database specific
     * @param format the format string. Beware: This is passed to the database "as is" and hence may be database specific.
     * @return a string expression representing the formatted value
     */
    public final DBFuncExpr format(String format)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_FORMAT, new Object[] { format }, DataType.VARCHAR);
    }
    
    /**
     * Creates and returns a sql-expression that returns the string length of this expression.

     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr length()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_LENGTH, null, DataType.INTEGER);
    }

    /**
     * Creates and returns a sql-expression that returns the position of a string in the current column expression.
     * 
     * @param str the string to find the position of
     *
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr indexOf(Object str)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_STRINDEX, new Object[] { str }, DataType.INTEGER);
    }

    /**
     * Creates and returns a sql-expression that returns the position of a string in the current column expression.
     * 
     * @param str the string to find the position of
     * @param fromPos the start position for the search
     *
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr indexOf(Object str, DBExpr fromPos)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_STRINDEXFROM, new Object[] { str, fromPos }, DataType.INTEGER);
    }

    /**
     * Overloaded. @see indexOf(Object str, DBExpr fromPos) 
     * 
     * @param str the string to find the position of
     * @param fromPos the start position for the search
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr indexOf(Object str, int fromPos)
    {
        return indexOf(str, getDatabase().getValueExpr(fromPos, DataType.INTEGER));
    }

    /**
     * concatenates a list of expressions to the current column 
     * @param concatExprs the expressions to concat
     * @return the concat expression
     */
    public DBConcatFuncExpr concat(DBColumnExpr... concatExprs)
    {
        return new DBConcatFuncExpr(this, concatExprs);
    }
            
    /**
     * concatenates a list of expressions to the current column 
     * @param separator a string to insert between each of the expressions
     * @param concatExprs the expressions to concat
     * @return the concat expression
     */
    public DBConcatFuncExpr concat(String separator, DBColumnExpr... concatExprs)
    {
        return new DBConcatFuncExpr(this, separator, concatExprs);
    }

    /**
     * Puts a value or expression before the current expression 
     * @param value the expressions to prepend
     * @return the combined value
     */
    public DBFuncExpr prepend(Object value)
    {
        String opertor  = (getDataType()==DataType.UNKNOWN ? "" 
                        : (getDataType().isText() ? getDatabase().getDbms().getSQLPhrase(DBSqlPhrase.SQL_CONCAT_EXPR) 
                        : "+"));
        String template = StringUtils.concat("{0}",opertor,"?");
        return new DBFuncExpr(this, template, new Object[] { value }, false, getDataType());
    }
    
    /*
     * Numeric functions
     */

    /**
     * Creates and returns a sql-expression for the absolute abs() function.
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr abs()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_ABS, null);
    }

    /**
     * Create and returns an expression for the SQL-function floor()
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr floor()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_FLOOR, null);
    }

    /**
     * Create and returns an expression for the SQL-function ceil()
     * 
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr ceiling()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_CEILING, null);
    }

    /**
     * Creates and returns an function object that
     * rounds a number espression with the given decimals.
     * 
     * @param decimals the number of decimal to which to truncate the current value
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr round(int decimals)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_ROUND, new Object[] { new Integer(decimals) });
    }

    /**
     * Creates and returns an function object that
     * truncates a number espression with the given decimals.
     * 
     * @param decimals the number of decimal to which to truncate the current value
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr trunc(int decimals)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_TRUNC, new Object[] { new Integer(decimals) });
    }

    /**
     * Creates and returns an function object that
     * calculates the year of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public final DBFuncExpr year()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_YEAR, null);
    }

    /**
     * Creates and returns an function object that
     * calculates the month of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public final DBFuncExpr month()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_MONTH, null);
    }

    /**
     * Creates and returns an function object that
     * calculates the day of a date value.
     * 
     * @return the new DBColumnExpr object
     */
    public final DBFuncExpr day()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_DAY, null);
    }

    /*
     * Aggregation functions
     */
    
    /**
     * Creates and returns an aggregation function object
     * which calculates the sum for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr sum()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_SUM, null);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the minimum value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr min()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_MIN, null);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the maximum value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr max()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_MAX, null);
    }

    /**
     * Creates and returns an aggregation function object
     * which returns the average value for the current expression over a group of rows.
     *
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr avg()
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_AVG, null);
    }

    /**
     * Creates and returns string aggregation expression
     * @param separator the separator between string
     * @param orderBy the order by expression
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr stringAgg(String separator, DBOrderByExpr orderBy)
    {
        return getExprFromPhrase(DBSqlPhrase.SQL_FUNC_STRAGG, new Object[] { separator, orderBy });
    }

    /**
     * Creates and returns string aggregation expression
     * @param separator the separator between string
     * @return the new DBFuncExpr object
     */
    public final DBFuncExpr stringAgg(String separator)
    {
        return stringAgg(separator, this.asc());
    }
    
    /**
     * Creates and returns an expression for the SQL "count()" function
     * which returns the number of rows in the result set.
     *
     * @return the new DBFuncExpr object
     */
    public DBCountExpr count()
    {
        return new DBCountExpr(this, false);
    }

    /**
     * Creates and returns an expression for the SQL "count()" function
     * which returns the number of unique values in the result set.
     *
     * @return the new DBFuncExpr object
     */
    public DBCountExpr countDistinct()
    {
        return new DBCountExpr(this, true);
    }
    
    /*
     * Case functions
     */

    /**
     * Creates and returns a sql-expression that maps enum values by name or ordinal to their string representation 
     * 
     * @param enumType an enumType to decode 
     * @param otherwise the varchar value to take if no key matches the given expression
     * @return a DBDecodeExpr object
     */
    public DBDecodeExpr decodeEnum(Class<? extends Enum<?>> enumType, String otherwise)
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
    public DBDecodeExpr decodeSort(Class<? extends Enum<?>> enumType, boolean defaultToEnd)
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
    public DBDecodeExpr decode(Map<?,?> valueMap, Object otherwise)
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

    public final DBDecodeExpr decode(Object key1, Object value1, Object otherwise)
    {
        Map<Object, Object> list = new HashMap<Object, Object>();
        list.put(key1, value1);
        return decode(list, otherwise);
    }

    public final DBDecodeExpr decode(Object key1, Object value1, Object key2, Object value2, Object otherwise)
    {
        Map<Object, Object> list = new HashMap<Object, Object>();
        list.put(key1, value1);
        list.put(key2, value2);
        return decode(list, otherwise);
    }

    public final DBDecodeExpr decode(Object key1, Object value1, Object key2, Object value2, Object key3, Object value3,
                               Object otherwise)
    {
        Map<Object, Object> list = new HashMap<Object, Object>();
        list.put(key1, value1);
        list.put(key2, value2);
        list.put(key3, value3);
        return decode(list, otherwise);
    }

    public final DBDecodeExpr decode(Options options, Object otherwise)
    {
        int size = options.size() + (otherwise!=null ? 1 : 0);
        Map<Object, Object> list = new HashMap<Object, Object>(size);
        for (OptionEntry e : options)
            list.put(e.getValue(), e.getText());
        return decode(list, otherwise);
    }

    public final DBDecodeExpr decode(Options options)
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
            elseExpr = getDatabase().getValueExpr(otherwise, getDataType());
        }
        // Create DBCaseExpr
        return new DBCaseWhenExpr(compExpr, this, elseExpr);
    }

    /*
     * Type conversion functions
     */

    /**
     * Creates a new DBFuncExpr object that will convert
     * the current column to the destination data type specified.
     * 
     * @param dataType the destination data type
     * @param format optional destination format (usually a string)
     * @return the new DBFuncExpr object
     */
    public DBConvertExpr convertTo(DataType dataType, Object format)
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
    public final DBConvertExpr convertTo(DataType dataType)
    {
        return convertTo(dataType, null);
    }
    
    /**
     * Creates a new DBFuncExpr object (to_char SQL statement)
     * with the parameters prefix = "to_char(" and postfix = ")"
     * 
     * @return the new DBFuncExpr object
     */
    public final DBConvertExpr toChar()
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
    public final DBConvertExpr toChar(String format)
    {
        return convertTo(DataType.VARCHAR, format);
    }

    /*
     * DBMS Native functions
     */

    /**
     * Creates and returns a function from an sql template
     * The template may consist of the following placeholders:
     *  ? = the expression on which the function is applied (usually a column expression)
     *  {[param-index]:[DataType]} = a function parameter. The DataType name, if supplied, must match the name of a DataType enum value.
     * @param template the sql phrase template (see above)
     * @param returnType the returned DataType
     * @param params the list of function parameter values 
     * @return the function expression
     */
    public final DBColumnExpr function(String template, DataType returnType, Object... params)
    {
        return new DBFuncExpr(this, template, params, false, returnType);
    }

    /**
     * Creates and returns a function from an sql template
     * The template may consist of the following placeholders:
     *  ? = the expression on which the function is applied (usually a column expression)
     *  {[param-index]:[DataType]} = a function parameter. The DataType name, if supplied, must match the name of a DataType enum value.
     * @param template the sql phrase template (see above)
     * @param returnType the returned DataType
     * @param params additional function parameters
     * @return the aggregate expression
     */
    public final DBColumnExpr aggregate(String template, DataType returnType, Object... params)
    {
        return new DBFuncExpr(this, template, params, true, returnType);
    }
    
    /*
     *  OrderByExpr functions 
     */

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
    
    /*
     * Join
     */
    
    /**
     * create a join expression for DBCommand.join()
     * @param joinWith the column expression to join this expression with
     * @return the join expression
     */
    public DBColumnJoinExpr on(DBColumnExpr joinWith)
    {
        // Must have rowsets
        if (getRowSet()==null)
            throw new NotSupportedException(this, "join");
        if (joinWith.getRowSet()==null)
            throw new NotSupportedException(joinWith, "join");
        // create the expression
        DBColumnJoinExpr join = new DBColumnJoinExpr(this, joinWith, DBJoinType.INNER);
        return join;
    }
    
    /*
     * Other
     */
 
    /**
     * returns a corresponding Java type for this expression
     * @return the java type
     */
    public Class<?> getJavaType()
    {
        return this.getDatabase().getColumnJavaType(this);
    }
 
    /**
     * For Debugging
     */
    @Override
    public String toString()
    {
        String name = getName();
        if (StringUtils.isNotEmpty(name))
            return StringUtils.concat(getClass().getSimpleName(), "[", name, "]");
        // default
        return super.toString();
    }
    
    /**
     * Checks if a value is a literal value
     * @param value the value to check
     * @return true if the value is a simple value like String, Number, Boolean etc.
     */
    protected boolean isLiteralValue(Object value)
    {
        // Cannot use DBExpr or DBSystemDate as parameter
        if (value==null || value instanceof DBCmdParam || value instanceof DBExpr || value instanceof DBDatabase.DBSystemDate)
            return false;
        // check immutable
        return ClassUtils.isImmutableClass(value.getClass());
    }
    
}