/*
 * ESTEAM Software GmbH, 02.07.2008
 */
package org.apache.empire.struts2.actionsupport;

public enum SessionPersistence {
    /*
     * Choices for record persistence
     */
    None, /* None = nothing is stored on session */
    Key,  /* Key = the record key is stored on the session. The record is reloaded if necessary */
    Data  /* Data = the whole record is stored on the session. */

}
