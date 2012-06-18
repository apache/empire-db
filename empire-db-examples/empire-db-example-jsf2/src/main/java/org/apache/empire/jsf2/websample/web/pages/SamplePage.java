/*
 * ESTEAM Software GmbH, 18.04.2012
 */
package org.apache.empire.jsf2.websample.web.pages;

import java.sql.Connection;

import org.apache.empire.db.DBCommand;
import org.apache.empire.jsf2.pages.Page;
import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.web.SampleApplication;
import org.apache.empire.jsf2.websample.web.SampleUser;
import org.apache.empire.jsf2.websample.web.SampleUtils;

public class SamplePage extends Page
{
    private static final long serialVersionUID = 1L;


    protected SampleApplication getApplication()
    {
        return SampleUtils.getSampleApplication();
    }

    protected SampleDB getDatabase()
    {
        return SampleUtils.getDatabase();
    }

    public Connection getConnection()
    {
        Connection conn = SampleUtils.getConnection();
        return conn;
    }

    public SampleUser getUser()
    {
        return SampleUtils.getSampleUser();
    }

//    public int getLanguageIndex()
//    {
//        return SampleUtils.getSampleSession().getUser().getLanguageIndex();
//    }



    protected DBCommand createQueryCommand()
    {
        return getDatabase().createCommand();
    }
}
