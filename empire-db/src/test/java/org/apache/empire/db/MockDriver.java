/**
 * 
 */
package org.apache.empire.db;

import java.sql.Connection;

import org.apache.empire.data.DataType;

public class MockDriver extends DBDatabaseDriver{
    
    class MockCommand extends DBCommand{

        protected MockCommand(DBDatabase db)
        {
            super(db);
        }
        
    }

    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        return new MockCommand(db);
    }

    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        return null;
    }

    @Override
    public Object getNextSequenceValue(DBDatabase db, String SeqName, int minValue, Connection conn)
    {
        return null;
    }

    @Override
    public String getSQLPhrase(int phrase)
    {
        return null;
    }

    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        return false;
    }
    
}