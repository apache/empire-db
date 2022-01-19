/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db;

public interface DBContextAware
{
    <T extends DBContext> T  getContext();
}
