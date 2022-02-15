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

import java.sql.Connection;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.expr.column.DBFuncExpr;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBMSHandlerBase;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.NotImplementedException;

public class MockDriver extends DBMSHandlerBase {
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    int seqValue = 0;

    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        return null;
    }

    @Override
    public Object getNextSequenceValue(DBDatabase db, String SeqName, int minValue, Connection conn)
    {
        return ++seqValue;
    }

    @Override
    public DBColumnExpr getNextSequenceValueExpr(DBTableColumn column)
    {
        return new DBFuncExpr(column, "nextval()", null, column, false, DataType.INTEGER);
    }

    @Override
    public String getSQLPhrase(DBSqlPhrase phrase)
    {
        return phrase.getSqlDefault();
    }

    @Override
    public void getDDLScript(DDLActionType type, DBObject dbo, DBSQLScript script)
    {
        throw new NotImplementedException(this, "getDDLScript");
    }

    @Override
    public boolean isSupported(DBMSFeature type)
    {
        return false;
    }
    
}