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
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
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
     * Small text (represents varchar)
     */
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
     * Fixed length character value.
     */
    CHAR, //         = 6;
    
    /**
     * Double precision floating point value
     */
    DOUBLE, //       = 7;
    
    /**
     * Decimal numeric value (size indicates scale and precision)
     */
    DECIMAL, //      = 8;
    
    /**
     * Boolean field (emulated if not supported by dbms as number or char)
     */
    BOOL, //         = 9;
    
    /**
     * Long text > 2K
     */
    CLOB, //         = 10;
    
    /**
     * Binary data
     */
    BLOB, //         = 11;
}
