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
package org.apache.empire.dbms.h2;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBTableColumn;

public class H2DDLGenerator extends DBDDLGenerator<DBMSHandlerH2>
{
    public H2DDLGenerator(DBMSHandlerH2 dbms)
    {
        super(dbms);
        // set Oracle specific data types
        initDataTypes();
    }

    /**
     * sets H2 specific data types
     */
    private void initDataTypes()
    {   // Override data types
        DATATYPE_FLOAT      = "DOUBLE";
        DATATYPE_CLOB       = "LONGTEXT";
    }

    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, StringBuilder sql)
    {
        switch (type)
        {
            case AUTOINC:
            {   // Auto increment
                super.appendColumnDataType(type, size, c, sql);
                if (dbms.isUseSequenceTable()==false)
                    sql.append(" AUTO_INCREMENT");
                break;
            }    
            default:
                // use default
                return super.appendColumnDataType(type, size, c, sql);
        }
        return true;
    }
 
}