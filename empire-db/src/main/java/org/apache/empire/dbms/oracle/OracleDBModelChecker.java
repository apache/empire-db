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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.db.validation.DBModelErrorHandler;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.oracle.DBMSHandlerOracle.BooleanType;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation is used to check Oracle Databases
 */
public class OracleDBModelChecker extends DBModelChecker
{
    private static final Logger log = LoggerFactory.getLogger(OracleDBModelChecker.class);
    
    private BooleanType booleanType = BooleanType.NUMBER;
    
    public OracleDBModelChecker(DBDatabase db, String schemaName)
    {
        super(db, null, schemaName);
        // Detect boolean type
        DBMSHandler dbms = db.getDbms();
        if (dbms instanceof DBMSHandlerOracle)
        {
            booleanType = ((DBMSHandlerOracle)dbms).getBooleanType();
        }
        else
        {   // Illegal DBMSHandler
            log.error("The database is not attached to a DBMSHandlerOracle");
            throw new InvalidArgumentException("db", db);
        }
    }

    /**
     * collects all column information at once
     */
    @Override
    protected int collectColumns(DatabaseMetaData dbMeta)
            throws SQLException
    {
        return super.collectColumns(dbMeta, null);
    }

    /**
     * collects all foreign keys at once
     */
    @Override
    protected int collectForeignKeys(DatabaseMetaData dbMeta)
            throws SQLException
    {
        return super.collectForeignKeys(dbMeta, null);
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
        super.checkColumnType(column, remoteColumn, handler);
    }
    
}
