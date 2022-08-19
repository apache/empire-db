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
package org.apache.empire.db.expr.column;

import java.util.Set;

import org.apache.empire.commons.StringUtils;
// Java
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;


/**
 * This class is used for performing various SQL functions on a column or column expression. 
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use any of the following functions:<BR>
 * {@link DBColumnExpr#abs() }, {@link DBColumnExpr#coalesce(Object) }, {@link DBColumnExpr#convertTo(DataType) }, 
 * {@link DBColumnExpr#decode(java.util.Map, Object) }, {@link DBColumnExpr#lower() }, {@link DBColumnExpr#min() }, 
 * {@link DBColumnExpr#max() }, {@link DBColumnExpr#month() }, {@link DBColumnExpr#sum() }, 
 * {@link DBColumnExpr#trim() }, {@link DBColumnExpr#upper() }, {@link DBColumnExpr#year() } 
 * <P>
 *
 */
public class DBFuncExpr extends DBAbstractFuncExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    protected final DBSqlPhrase  phrase;
    protected final Object[]     params;
    protected String             template;

    /**
     * Constructs a new DBFuncExpr object set the specified parameters to this object.
     * Do not use directly - use any of the DBColumnExpr.??? factory functions instead!
     * 
     * The sql function string is built from a string template.
     * The template string is identified by the phrase param and obtained from the dbms. 
     * 
     * @param expr the DBColumnExpr object
     * @param phrase the SQL-phrase
     * @param params an array of params which will be replaced in the template
     * @param dataType indicates the data type of the function result 
     */
    public DBFuncExpr(DBColumnExpr expr, DBSqlPhrase phrase, Object[] params, DataType dataType)
    {
        super(expr, phrase.isAggregate(), dataType);
        // Set Phrase and Params
        this.phrase = phrase;
        this.params = params;
        // If database is open, set template immediately
        DBDatabase db = expr.getDatabase();
        if (db.isOpen())
        {   // Set template immediately
            this.template = db.getDbms().getSQLPhrase(phrase);
            if (StringUtils.isEmpty(template))
                throw new NotSupportedException(getDbms(), phrase.name());
        }
        // check
        if (phrase==DBSqlPhrase.SQL_FUNC_COALESCE)
            log.warn("DBFuncExpr should not be used for SQL_FUNC_COALESCE. Use DBCoalesceExpr instead.");
    }

    /**
     * Constructs a new DBFuncExpr object set the specified parameters to this object.
     * 
     * The sql function string is built from a string template.
     * The template string must contain a ? which is a placeholder for the column expression.
     * 
     * @param expr the DBColumnExpr object
     * @param template specifies a template for the expression. The template must contain a ? placeholder for the column expression
     * @param params an array of params which will be replaced in the template
     * @param updateColumn optional update column if any. This parameter may be null
     * @param isAggregate indicates whether the function is an aggregate function (sum, min, max, avg, ...)
     * @param dataType indicates the data type of the function result 
     */
    public DBFuncExpr(DBColumnExpr expr, String template, Object[] params, boolean isAggregate, DataType dataType)
    {
        super(expr, isAggregate, dataType);
        // check
        if (template==null)
            throw new InvalidArgumentException("template", template);
        // Set Phrase and Params
        this.template = template;
        this.params = params;
        this.phrase = null;
    }
    
    @Override
    protected String getFunctionName()
    {
        // Get the template
        if (phrase!=null)
        {   // from phrase
            return phrase.getFuncName();
        }
        // Get the first word
        if (template!=null)
        {   String s = template.trim();
            int i=0;
            for (; i<s.length(); i++)
                if (s.charAt(i)<'A')
                    break;
            // return name 
            if (i>0)
                return s.substring(0,i);
        }
        return "FUNC";
    }
    
    @Override
    public Class<Enum<?>> getEnumType()
    {
        // check for functions which preserve the enumType 
        if (phrase==DBSqlPhrase.SQL_FUNC_COALESCE)
            return expr.getEnumType();
        // Check SQL-Phrase
        return super.getEnumType();
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        super.addReferencedColumns(list);
        if (this.params==null)
            return;
        // Check params
        for (int i=0; i<this.params.length; i++)
        {   // add referenced columns
            if (params[i] instanceof DBExpr)
               ((DBExpr)params[i]).addReferencedColumns(list);
        }
    }

    /**
     * Creates the SQL-Command adds a function to the SQL-Command.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {   // Get Template
        if (this.template==null)
        {   this.template = getDbms().getSQLPhrase(phrase);
            if (StringUtils.isEmpty(this.template))
                throw new NotSupportedException(getDbms(), phrase.name());
        }
        // Add SQL
        super.addSQL(sql, template, params, context);
    }
}