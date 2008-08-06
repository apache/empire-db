/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db;

/**
 * 
 * DBJoinType contains the possibilities to join two database tables.
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public enum DBJoinType
{
    LEFT,   //   =-1,
    INNER,  //   = 0,
    RIGHT;  //   = 1
    
    public static DBJoinType reversed(DBJoinType type)
    {
        switch(type)
        {
            case LEFT:  return RIGHT;
            case RIGHT: return LEFT;
            default:    return type; // no change
        }
    }
}
