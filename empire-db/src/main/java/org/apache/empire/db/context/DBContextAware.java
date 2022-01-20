/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db.context;

import org.apache.empire.db.DBContext;

public interface DBContextAware
{
    <T extends DBContext> T  getContext();
}
