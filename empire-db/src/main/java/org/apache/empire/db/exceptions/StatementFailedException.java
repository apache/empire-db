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
package org.apache.empire.db.exceptions;

import java.sql.SQLException;

import org.apache.empire.commons.ErrorType;
import org.apache.empire.dbms.DBMSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatementFailedException extends EmpireSQLException
{
    private static final long serialVersionUID = 1L;

    // Logger
    private static final Logger log = LoggerFactory.getLogger(StatementFailedException.class);
    
    public static final ErrorType errorType = new ErrorType("error.db.statementFailed",  "Error executing statement {0}.\r\nNative error is: {1}");
    
    public StatementFailedException(DBMSHandler dbms, String sqlCmd, SQLException cause)
    {
        super(StatementFailedException.errorType, new String[] { sqlCmd, messageFromSQLException(dbms, cause) }, 1, cause);
    }
    
    /**
     * log the error
     */
    @Override
    protected void log()
    {
       if ( log.isErrorEnabled() )
            log.error(getMessage());
       else
           super.log();
    }
}
