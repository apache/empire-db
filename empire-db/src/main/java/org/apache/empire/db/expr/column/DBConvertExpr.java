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

/**
 * This class is used to convert a value to a different data type.
 * This function uses the DBMSHandler.getConvertPhrase function to obtain a conversion template.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBColumnExpr#convertTo(DataType) }
 * <P>
 * @author doebele
 */
public class DBConvertExpr extends DBAbstractFuncExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
  
    private final Object format;
    
    /**
     * Constructs a DBDecodeExpr
     * @param expr the expression to be converted
     * @param dataType the target data type
     * @param format optional formatting information
     */
    public DBConvertExpr(DBColumnExpr expr, DataType dataType, Object format)
    {
        super(expr, false, dataType);
        // Save Info
        this.format = format;
    }

    @Override
    protected String getFunctionName()
    {
        return dataType.name();
    }

    @Override
    public void addSQL(StringBuilder sql, long context)
    {
        // Set Params
        String template = getDbms().getConvertPhrase(dataType, expr.getDataType(), format);
        super.addSQL(sql, template, null, context);
    }

}
