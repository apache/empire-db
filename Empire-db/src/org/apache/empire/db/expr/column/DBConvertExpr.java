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

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabaseDriver;

/**
 * This class is used to convert a value to a different data type.
 * This function uses the DBDatabaseDriver.getConvertPhrase function to obtain a conversion template.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#convertTo() }
 * <P>
 * @author doebele
 */
public class DBConvertExpr extends DBAbstractFuncExpr
{
    private final Object format;
    
    /**
     * Constructs a DBDecodeExpr
     * @param compExpr the condition to be evaluated
     * @param expr the expression returned if the condition is true
     * @param elseExpr the expression returned if the condition is false (may be null)
     */
    public DBConvertExpr(DBColumnExpr expr, DataType dataType, Object format)
    {
        super(expr, expr.getUpdateColumn(), false, dataType);
        // Save Info
        this.format = format;
    }

    @Override
    protected String getFunctionName()
    {
        return "convert";
    }

    @Override
    public void addSQL(StringBuilder sql, long context)
    {
        DBDatabaseDriver driver = getDatabaseDriver();
        // Set Params
        String template = driver.getConvertPhrase(dataType, expr.getDataType(), format);
        super.addSQL(sql, template, null, context);
    }

}
