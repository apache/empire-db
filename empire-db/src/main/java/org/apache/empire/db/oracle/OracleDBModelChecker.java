package org.apache.empire.db.oracle;

import java.sql.Connection;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.oracle.DBDatabaseDriverOracle.BooleanType;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.db.validation.DBModelErrorHandler;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation is used to check Oracle Databases
 */
public class OracleDBModelChecker extends DBModelChecker
{
    private static final Logger log = LoggerFactory.getLogger(OracleDBModelChecker.class);
    
    private BooleanType booleanType = BooleanType.NUMBER; 

    @Override
    public void checkModel(DBDatabase db, Connection conn, String dbSchema, DBModelErrorHandler handler)
    {
        // Get boolean type from driver
        DBDatabaseDriver driver = db.getDriver();
        if (driver instanceof DBDatabaseDriverOracle)
        {
            booleanType = ((DBDatabaseDriverOracle)driver).getBooleanType();
        }
        else
            log.warn("Provided driver is not of type DBDatabaseDriverOracle");

        // check now
        super.checkModel(db, conn, dbSchema, handler);
        
    }

    @Override
    protected void checkBoolColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkColumnNullable(column, remoteColumn, handler);

        // check data type
        DataType booleanDataType = null;
        switch (booleanType)
        {
            case NUMBER:
                booleanDataType = DataType.DECIMAL;
                break;
            case CHAR:
                booleanDataType = DataType.CHAR;
                break;
            default:
                throw new InvalidPropertyException("booleanType", booleanType);
        }

        if (remoteColumn.getDataType() != booleanDataType)
        {
            handler.columnTypeMismatch(column, booleanDataType);
        }

        // size should always be 1
        if (remoteColumn.getSize() != 1)
        {
            handler.columnSizeMismatch(column, 1, 0);
        }
    }
}
