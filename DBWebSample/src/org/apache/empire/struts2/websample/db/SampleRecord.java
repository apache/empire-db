/*
 * ESTEAM Software GmbH, 06.10.2007
 */
package org.apache.empire.struts2.websample.db;

import org.apache.empire.db.DBRecord;
import org.apache.empire.struts2.websample.web.SampleContext;


public class SampleRecord extends DBRecord
{
    // DBRecord members
    protected SampleContext context;
    
    public SampleRecord(SampleContext context)
    {
        this.context = context;
    }
}
