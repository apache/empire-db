/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.jsf2.websample.web;

import org.apache.empire.jsf2.app.WebDBContext;
import org.apache.empire.jsf2.websample.db.SampleDB;

/**
 * This is an example for a custom DBContext extension
 * @author rainer
 *
 */
public class SampleContext extends WebDBContext<SampleDB>
{
    private final SampleSession session;
    
    public SampleContext(SampleApplication app, SampleSession session)
    {
        super(app, app.getDatabase());
        // the session
        this.session = session;
    }

    public SampleUser getUser()
    {
        return session.getUser();
    }
}
