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
package org.apache.empire.data;

/**
 * DataType is an enumeration of data types that are supported
 * with the empire-db component.
 * 
 */
public enum DataType
{
    /**
     * Unknown, used internally only for sql phrases
     */
    UNKNOWN, //      = 0;
    
    /**
     * Integer value (16, 32 or 64 bit)
     */
    INTEGER, //      = 1;
    
    /**
     * A numeric sequence generated value
     */
    AUTOINC, //      = 2;
    
    /**
     * Variable text (represents varchar)
     */
    VARCHAR, //      = 3;
    
    /**
     * Variable text (represents varchar)
     * 
     * @deprecated  Use VARCHAR instead
     */
    @Deprecated
    TEXT, //         = 3;
    
    /**
     * Date only value (without time)
     */
    DATE, //         = 4;
    
    /**
     * Date value including time. Also knows a timestamp
     */
    DATETIME, //     = 5;
    
    /**
     * Automatic Record timestamp (only one per table allowed)  
     */
    TIMESTAMP, //    = 6;
    
    /**
     * Fixed length character value.
     */
    CHAR, //         = 7;
    
    /**
     * floating point value
     */
    FLOAT, //        = 8;
    
    /**
     * Decimal numeric value (size indicates scale and precision)
     */
    DECIMAL, //      = 9;
    
    /**
     * Boolean field (emulated if not supported by DBMS as number or char)
     */
    BOOL, //         = 10;
    
    /**
     * Long text &gt; 2K
     */
    CLOB, //         = 11;
    
    /**
     * Binary data
     */
    BLOB, //         = 12;
    
    /**
     * Unique Identifier (non-numeric, treated like text) 
     */
    UNIQUEID; //     = 13;
    
    /**
     * Returns true if the data type is a text based data type (char, text or clob)
     * @return true if the data type is a character based data type
     */
    public boolean isText()
    {
        return (this==DataType.TEXT || this==DataType.VARCHAR || this==DataType.CHAR || this==DataType.CLOB);
    }

    /**
     * Returns true if the data type is a numeric data type (integer, decimal, float)
     * @return true if the data type is a numeric data type
     */
    public boolean isNumeric()
    {
        return (this==DataType.INTEGER || this==DataType.DECIMAL || this==DataType.FLOAT || this==DataType.AUTOINC);
    }

    /**
     * Returns true if the data type is a date based data type (date or datetime)
     * @return true if the data type is a date based data type
     */
    public boolean isDate()
    {
        return (this==DataType.DATE || this==DataType.DATETIME || this==DataType.TIMESTAMP);
    }

    /**
     * Returns true if the data type is a boolean type
     * @return true if the data type is a boolean type
     */
    public boolean isBoolean()
    {
        return (this==DataType.BOOL);
    }
}
