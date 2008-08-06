/*
 * ESTEAM Software GmbH, 04.04.2008
 */
package org.apache.empire.db;

/**
 * This enum is used with the DBDatabaseDriver::isSupported method to query database driver capabilities.
 *
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public enum DBDriverFeature {
    // Support Flags used by DBDatabaseDriver::isSupported()
    CREATE_SCHEMA,
    SEQUENCES,    
}
