/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db;

/**
 * This enum allocates the available compare types.
 *
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public enum DBCmpType
{
    NONE, //         = -1,
    EQUAL, //        =  0,
    NOTEQUAL, //     =  1,
    LESSTHAN, //     =  2,
    MOREOREQUAL, //  =  3,
    GREATERTHAN, //  =  4,
    LESSOREQUAL, //  =  5,
    LIKE, //         =  6,
    NOTLIKE, //      =  7,
    NULL, //         =  8,
    NOTNULL, //      =  9,
    BETWEEN, //      = 10,
    NOTBETWEEN, //   = 11,
    IN, //           = 12,
    NOTIN; //        = 13,
    
    public static DBCmpType getNullType(DBCmpType type)
    {
        switch(type)
        {
            case NOTEQUAL:
            case MOREOREQUAL:
            case LESSOREQUAL:
            case NOTLIKE:
            case NOTNULL:
            case NOTBETWEEN:
            case NOTIN:
            // Compare with NOT null
                 return DBCmpType.NOTNULL;
            // Compare with null
            default: return DBCmpType.NULL;
        }
    }
}
