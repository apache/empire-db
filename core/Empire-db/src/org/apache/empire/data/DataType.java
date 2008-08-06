/*
 * ESTEAM Software GmbH
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
