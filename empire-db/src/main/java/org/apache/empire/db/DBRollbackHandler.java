/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db;

public interface DBRollbackHandler
{
    DBObject getObject();
    void combine(DBRollbackHandler successor);
    void rollback();
    void discard();
}
