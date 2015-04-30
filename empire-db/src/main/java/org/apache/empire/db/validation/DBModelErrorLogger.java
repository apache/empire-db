package org.apache.empire.db.validation;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implemtnation of the {@link DBModelErrorHandler} interface that logs all errors
 */
public class DBModelErrorLogger implements DBModelErrorHandler
{
    private static final Logger log = LoggerFactory.getLogger(DBModelErrorLogger.class);

    /**
     * handle itemNotFound errors
     */
    public void itemNotFound(DBObject dbo)
    {
        if (dbo instanceof DBIndex)
        {
            DBIndex dbi = (DBIndex) dbo;
            DBModelErrorLogger.log.error("The primary key " + dbi.getName() + " for table " + dbi.getTable().getName()
                                         + " does not exist in the target database.");
        }
        else
        {
            DBModelErrorLogger.log.error("The object " + dbo.toString() + " does not exist in the target database.");
        }
    }

    /**
     * handle columnTypeMismatch errors
     */
    public void columnTypeMismatch(DBColumn col, DataType type)
    {
        DBModelErrorLogger.log.error("The column " + col.getFullName() + " type of " + col.getDataType().toString()
                                     + " does not match the database type of " + type.toString());
    }

    /**
     * handle columnSizeMismatch errors
     */
    public void columnSizeMismatch(DBColumn col, int size, int scale)
    {
        DBModelErrorLogger.log.error("The column " + col.getFullName() + " size of " + String.valueOf(col.getSize())
                                     + " does not match the size database size of " + String.valueOf(size));
    }

    /**
     * handle columnNullableMismatch errors
     */
    public void columnNullableMismatch(DBColumn col, boolean nullable)
    {
        if (nullable)
        {
            DBModelErrorLogger.log.error("The column " + col.getFullName() + " must not be nullable");
        }
        else
        {
            DBModelErrorLogger.log.error("The column " + col.getFullName() + " must be nullable");
        }
    }

    /**
     * handle primaryKeyColumnMissing errors
     */
    public void primaryKeyColumnMissing(DBIndex primaryKey, DBColumn column)
    {
        DBModelErrorLogger.log.error("The primary key " + primaryKey.getName() + " of table " + primaryKey.getTable().getName()
                                     + " misses the column " + column.getName());
    }

}
