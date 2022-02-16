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
package org.apache.empire.db.expr.set;

import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdParam;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBRowSet;


/**
 * This class is used for building a set expression of a SQL update statement.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumn#to(Object)} factory method.
 * <P>
 * For the SQL fragment "set name="foo"<BR>
 * you should write: cmd.set( TABLE.NAME.to( "foo" ));
 * <P>
 * 
 *
 */
public class DBSetExpr extends DBExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    public final DBColumn column;
    public Object         value;

    /**
     * Constructs a new DBSetExpr object.
     * 
     * @param expr the column
     * @param value the value
     */
    public DBSetExpr(DBColumn expr, Object value)
    {
        this.column = expr;
        this.value = value;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return column.getDatabase();
    }

    /**
     * Returns the current Table object.
     * 
     * @return the current DBRowSet object
     */
    public DBRowSet getTable()
    {
        return column.getRowSet();
    }

    /**
     * @return the column which value should be set
     */
    public DBColumn getColumn()
    {
        return column;
    }

    /**
     * @return the value to which to set the column to
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * The value to which to set the column
     * @param value the new column value 
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        list.add(column);
    }

    /**
     * Copy Command
     * @param cmd
     */
    public DBSetExpr copy(DBCommand newCmd)
    {
        Object valueCopy = value;
        if (value instanceof DBCmdParam) 
            valueCopy = newCmd.addParam(DataType.UNKNOWN, ((DBCmdParam)value).getValue());
        return new DBSetExpr(column, valueCopy);
    }

    /**
     * Creates the SQL-Command.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    {
        if ((context & CTX_NAME) != 0)
            column.addSQL(buf, CTX_NAME);
        if ((context & CTX_NAME) != 0 && (context & CTX_VALUE) != 0)
            buf.append("=");
        if ((context & CTX_VALUE) != 0)
            buf.append(getObjectValue(column.getDataType(), value, context, "+"));
    }
}
