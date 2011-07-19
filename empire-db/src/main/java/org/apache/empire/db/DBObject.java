/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.db;

// java.sql
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.empire.EmpireException;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for all database related objects.
 * Every object is attached to a DBDatabase object.
 * 
 *
 */
public abstract class DBObject extends ErrorObject implements Serializable
{
    private static final long serialVersionUID = 1L;
    // Logger
    private static final Logger log = LoggerFactory.getLogger(DBObject.class);

    /**
     * Returns the database object to which this object belongs to.
     * For the database object itself this function will return the this pointer.
     * 
     * @return the database object
     */
    public abstract DBDatabase getDatabase();
    
    /**
     * Sets the current error from an SQL Exception.
     * 
     * @param type the error type
     * @param sqle the SQL error message
     *            
     * @return the return value is always false
     */
    protected EmpireException SQL2EmpireException(ErrorType type, SQLException sqle)
    {
        log.error("Database operation failed.", sqle);
        // converts a database error message to a human readable error message.
        DBDatabase db = getDatabase();
        if (db!=null && db.getDriver()!=null)
            return new EmpireException(type, new Object[] { db.getDriver().extractErrorMessage(sqle) }, sqle);
        // Set the error Message
        return new EmpireException(type, sqle.getMessage());
    }

    /**
     * Sets the current error from an SQL Exception.
     * 
     * @param sqle the SQL error message
     *            
     * @return the return value is always false
     */
    protected EmpireException SQL2EmpireException(SQLException sqle)
    {   // converts a database error message to a human readable error message.
        return SQL2EmpireException(DBErrors.SQLException, sqle);
    }

}