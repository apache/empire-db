/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db.context;

import org.apache.empire.db.DBObject;

public interface DBRollbackHandler
{
    DBObject getObject();
    void combine(DBRollbackHandler successor);
    void rollback();
    void discard();
}
