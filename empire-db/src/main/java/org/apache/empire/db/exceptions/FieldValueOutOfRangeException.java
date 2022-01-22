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

import org.apache.empire.commons.ErrorType;
import org.apache.empire.data.Column;

public class FieldValueOutOfRangeException extends FieldValueException
{
    private static final long serialVersionUID = 1L;
    
    public static final ErrorType outOfRangeErrorType      = new ErrorType("error.db.fieldValueOutOfRange",   "The value supplied for field {0} is out of range.");
    public static final ErrorType notInRangeErrorType      = new ErrorType("error.db.fieldValueNotInRange",   "The value supplied for field {0} must be between {1} and {2}.");
    public static final ErrorType valueTooBigErrorType     = new ErrorType("error.db.fieldValueTooBig",       "The value supplied for field {0} must not be greater than {1}.");
    public static final ErrorType valueTooSmallErrorType   = new ErrorType("error.db.fieldValueTooSmall",     "The value supplied for field {0} must not be smaller than {1}.");
    
    public FieldValueOutOfRangeException(Column col)
    {
        super(col, outOfRangeErrorType, new String[] { getColumnTitle(col) });
    }

    public FieldValueOutOfRangeException(Column col, Number min, Number max)
    {
        super(col, notInRangeErrorType, new String[] { getColumnTitle(col), String.valueOf(min), String.valueOf(max) });
    }

    public FieldValueOutOfRangeException(Column col, Number minMax, boolean isMax)
    {
        super(col, (isMax) ? valueTooBigErrorType : valueTooSmallErrorType, new String[] { getColumnTitle(col), String.valueOf(minMax) });
    }
}
