/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db;

import org.apache.empire.exceptions.ObjectNotValidException;

public class DBRecordV3 extends DBRecord
{
    
    private static final long serialVersionUID = 1L;
    
    private final DBContext context;
    
    public DBRecordV3(DBContext context, DBRowSet rowset)
    {
        super(rowset);
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    public <T extends DBContext> T  getContext()
    {
        return ((T)context);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends DBRowSet> T getTable()
    {
        return (T)super.getRowSet();
    }
    
    public void create()
    {
        super.create(getRowSet());
        // remove rollback
        context.removeRollbackHandler(this);
    }
    
    public void read(Object... key)
    {
        super.read(getRowSet(), key, context.getConnection());
        // remove rollback
        context.removeRollbackHandler(this);
    }
    
    public void update()
    {
        if (!isValid())
            throw new ObjectNotValidException(this);
        if (!isModified())
            return; /* Not modified. Nothing to do! */
        // allow rollback
        context.addRollbackHandler(createRollbackHandler());
        // update
        super.update(context.getConnection());
    }
    
    public void delete()
    {
        // allow rollback
        context.addRollbackHandler(createRollbackHandler());
        // delete
        super.delete(context.getConnection());
    }
    
    protected DBRollbackHandler createRollbackHandler()
    {
        return new DBRecordRollbackHandler(this);
    }
    
}
