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
package org.apache.empire.dbms.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.validation.DBModelParser;

public class MySQLDBModelParser extends DBModelParser
{
    public MySQLDBModelParser(String catalog, String schemaPattern)
    {
        super(catalog, schemaPattern);
    }
    
    @Override
    protected double getColumnSize(DataType empireType, ResultSet rs)
            throws SQLException
    {
        switch (empireType) 
        {   
            case INTEGER: {
                // return size in byte, depending on MySQL Integer Types
                // see http://dev.mysql.com/doc/refman/5.7/en/integer-types.html
                // ignore the "real" columnsize as its just a "format hint"
                int sqlType = rs.getInt("DATA_TYPE");
                switch(sqlType) {
                    case Types.TINYINT:
                        return 1; // TINYINT, 1 byte
                    case Types.SMALLINT:
                        return 2; // SMALLINT, 2 byte
                    case Types.BIGINT:
                        return 8; // BIGINT, 8 byte
                    default: 
                        return 4; // Types.INTEGER, INT, 4 byte
                }
            }
            default:
                return super.getColumnSize(empireType, rs);
        }
    }
    
    @Override
    protected Object getColumnDefault(ResultSet rs)
        throws SQLException
    {
        String defaultValue = rs.getString("COLUMN_DEF");
        if (defaultValue != null && defaultValue.equals("CURRENT_TIMESTAMP"))
            return DBDatabase.SYSDATE;
        return defaultValue;
    }
    
    @Override
    protected boolean isIdentityColumn(ResultSet rs)
    {
        try {
            int i = rs.findColumn("IS_AUTOINCREMENT");
            return rs.getString(i).equalsIgnoreCase("YES");
        } catch(SQLException e) {
            log.warn("Missing column IS_AUTOINCREMENT. Unable to detect Identity column");
            return false;
        }
    }
    
    @Override
    protected boolean isTimestampColumn(ResultSet rs)
    {   try {
            String defaultValue = rs.getString("COLUMN_DEF");
            if (rs.getInt("DATA_TYPE") == Types.TIMESTAMP && defaultValue != null && defaultValue.equals("CURRENT_TIMESTAMP"))
            {
                return true;
            }
            return super.isTimestampColumn(rs);
        } catch(SQLException e) {
            log.warn("Missing column COLUMN_DEF. Unable to detect Timestamp column");
            return false;
        }
    }

}
