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
package org.apache.empire.dbms.oracle;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.db.validation.DBModelErrorHandler;
import org.apache.empire.dbms.oracle.DBMSHandlerOracle.BooleanType;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation is used to check Oracle Databases
 */
public class OracleDBModelChecker extends DBModelChecker
{
    private static final Logger log = LoggerFactory.getLogger(OracleDBModelChecker.class);
    
    private final BooleanType booleanType;
    
    public OracleDBModelChecker(OracleDBModelParser modelParser, BooleanType booleanType)
    {
        super(modelParser);
        // Detect boolean type
        this.booleanType = booleanType;
        // ok
        log.info("OracleDBModelChecker created for {} with booleanType {}", modelParser.getSchemaName(), booleanType);
    }
    
    @Override
    protected void checkBoolColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkColumnNullable(column, remoteColumn, handler);

        // check data type
        DataType booleanDataType = null;
        switch (booleanType)
        {
            case NUMBER:
                booleanDataType = DataType.DECIMAL;
                break;
            case CHAR:
                booleanDataType = DataType.CHAR;
                break;
            default:
                throw new InvalidPropertyException("booleanType", booleanType);
        }

        if (remoteColumn.getDataType() != booleanDataType)
        {
            handler.columnTypeMismatch(column, booleanDataType);
        }

        // size should always be 1
        if (remoteColumn.getSize() != 1)
        {
            handler.columnSizeMismatch(column, 1, 0);
        }
    }
    
    @Override
    protected void checkColumnType(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        if (column.getDataType()==DataType.INTEGER && remoteColumn.getDataType().isNumeric())
        {   // Accept all numeric types as Integer
            return;
        }
        if (column.getDataType()==DataType.BOOL)
        {   // Boolean check
            if (booleanType==BooleanType.CHAR) {
                if (!remoteColumn.getDataType().isText())
                    handler.columnTypeMismatch(column, DataType.CHAR);
            } else
                if (!remoteColumn.getDataType().isNumeric())
                    handler.columnTypeMismatch(column, DataType.DECIMAL);
            return;
        }
        super.checkColumnType(column, remoteColumn, handler);
    }
    
}
