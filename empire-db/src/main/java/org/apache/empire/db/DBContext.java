/*
 * ESTEAM Software GmbH, 19.01.2022
 */
package org.apache.empire.db;

import java.sql.Connection;

import org.apache.empire.db.context.DBRollbackHandler;

/**
 * DBContext 
 * A context is required for every database operation
 * @author rainer
 */
public interface DBContext
{
    DBDatabaseDriver getDriver();
    
    Connection getConnection();
    
    <T extends DBUtils> T getUtils();

    DBCommand createCommand(DBDatabase db); 
    
    int executeSQL(String sqlCmd, Object[] sqlParams);
    
    int executeInsert(DBCommand cmd);
    
    int executeInsertInto(DBTable table, DBCommand cmd);
    
    int executeUpdate(DBCommand cmd);

    int executeDelete(DBTable from, DBCommand cmd);
    
    void commit();

    void rollback();
    
    boolean isRollbackHandlingEnabled();
    
    void appendRollbackHandler(DBRollbackHandler handler);

    void removeRollbackHandler(DBObject object);
    
    void discard();
}