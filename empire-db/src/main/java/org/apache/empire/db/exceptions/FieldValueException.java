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
package org.apache.empire.db.exceptions;

import org.apache.empire.commons.ErrorType;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.db.DBColumn;
import org.apache.empire.exceptions.EmpireException;

public abstract class FieldValueException extends EmpireException
{
    private static final long serialVersionUID = 1L;
    
    protected static String getColumnTitle(Column column)
    {
        String title = column.getTitle();
        if (StringUtils.isEmpty(title))
            title = "!["+((column instanceof DBColumn) ? ((DBColumn)column).getIdentifier() : column.getName()) + "]";
        return title;
    }
    
    private transient final Column column;

    protected FieldValueException(final Column column, final ErrorType errType, final String[] params, final Throwable cause)
    {
        super(errType, params, cause);
        // save type and params for custom message formatting
        this.column = column;
    }

    protected FieldValueException(final Column column, final ErrorType errType, final String[] params)
    {
        this(column, errType, params, null);
    }
    
    public Column getColumn()
    {
        return column;
    }

}
