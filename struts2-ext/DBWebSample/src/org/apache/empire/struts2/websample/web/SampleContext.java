/*
 * ESTEAM Software GmbH, 17.11.2007
 */
package org.apache.empire.struts2.websample.web;

import java.sql.Connection;

import org.apache.empire.struts2.websample.db.SampleDB;


public interface SampleContext
{
    public SampleDB getDatabase();
    
    public SampleUser getUser();
    
    public Connection getConnection();

}
