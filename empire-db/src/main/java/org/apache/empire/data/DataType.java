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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.apache.commons.beanutils.MethodUtils;

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
     * Date only value (without time)
     */
    DATE, //         = 4;
    
    /**
     * Time only (hh:mm:ss)
     * Might be emulated by a DATE column with a fix date of 1970-01-01
     */
    TIME, //         = 5;
    
    /**
     * Date including time but without nanoseconds
     */
    DATETIME, //     = 6;
    
    /**
     * Timestamp i.e. date including time and nanoseconds. 
     * Must be used for the Timestamp column used for optimistic locking  
     */
    TIMESTAMP, //    = 7;
    
    /**
     * Fixed length character value.
     */
    CHAR, //         = 8;
    
    /**
     * floating point value
     */
    FLOAT, //        = 9;
    
    /**
     * Decimal numeric value (size indicates scale and precision)
     */
    DECIMAL, //      = 10;
    
    /**
     * Boolean field (emulated if not supported by DBMS as number or char)
     */
    BOOL, //         = 11;
    
    /**
     * Long text &gt; 2K
     */
    CLOB, //         = 12;
    
    /**
     * Binary data
     */
    BLOB, //         = 13;
    
    /**
     * Unique Identifier (non-numeric, treated like text) 
     */
    UNIQUEID; //     = 14;
    
    /**
     * Returns true if the data type is a text based data type (char, text or clob)
     * @return true if the data type is a character based data type
     */
    public boolean isText()
    {
        return (this==DataType.VARCHAR || this==DataType.CHAR || this==DataType.CLOB);
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
        return (this==DataType.DATE || this==DataType.DATETIME || this==DataType.TIMESTAMP || this==DataType.TIME);
    }

    /**
     * Returns true if the data type is a boolean type
     * @return true if the data type is a boolean type
     */
    public boolean isBoolean()
    {
        return (this==DataType.BOOL);
    }

    /**
     * Returns whether or not two DataTypes are compatible
     * @param other the other one
     * @return true of types are compatible or false otherwise
     */
    public boolean isCompatible(DataType other)
    {
        if (this==other)
            return true; // the same 
        // Special case: UNKNOWN
        if (this==DataType.UNKNOWN || other==DataType.UNKNOWN)
            return true; // assume compatible
        // Check compatability
        if (isText() && other.isText())
            return true;
        if (isNumeric() && other.isNumeric())
            return true;
        if (isDate() && other.isDate())
            return true;
        // no match
        return false;
    }
    
    /**
     * Returns the DataType from a given Java Type
     * If the type is not mapped, then DataType.UNKNOWN is returned
     * @param javaType the Java Type
     * @return the corresponding DataType
     */
    public static DataType fromJavaType(Class<?> javaType)
    {
        if (javaType.isPrimitive())
            javaType = MethodUtils.getPrimitiveWrapper(javaType);
        // String
        if (javaType==String.class)
            return DataType.VARCHAR;
        if (javaType==Character.class ||
            javaType==Character[].class)
            return DataType.CHAR;
        // Check integer
        if (javaType == Integer.class || 
            javaType == Long.class ||
            javaType == Short.class)
            return DataType.INTEGER;
        if (Number.class.isAssignableFrom(javaType))
            return DataType.DECIMAL;
        // Check Dates
        if (javaType == Timestamp.class)
            return DataType.TIMESTAMP;
        if (Date.class.isAssignableFrom(javaType) ||
            javaType == LocalDateTime.class)
            return DataType.DATETIME;
        if (javaType == LocalDate.class)
            return DataType.DATE;
        // Other
        if (javaType == Boolean.class)
            return DataType.BOOL;
        if (javaType == byte[].class)
            return DataType.BLOB;
        // Unknown
        return DataType.UNKNOWN;
    }
    
}
