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
package org.apache.empire.struts2.web;

import org.apache.empire.commons.ErrorType;

public class WebErrors
{
    // general 
    public static final ErrorType InvalidFormData = new ErrorType("error.web.recordsDontMatch", "The form submitted is invalid for the current context.");
    public static final ErrorType ColumnNotFound  = new ErrorType("error.web.columnNotFound",   "The column {0} has not been found!");
    public static final ErrorType FieldError      = new ErrorType("error.web.fieldError", "Field {0}: {1}");

    // Parsing errors
    public static final ErrorType InputInvalidValue    = new ErrorType("error.web.input.invalidValue", "Invalid Field value");
    public static final ErrorType InputNoIntegerFormat = new ErrorType("error.web.input.integerFormat", "Invalid Integer format");
    public static final ErrorType InputNoNumberFormat  = new ErrorType("error.web.input.numberFormat", "Invalid number format");
    public static final ErrorType InputNoDateFormat    = new ErrorType("error.web.input.dateFormat", "Invalid date format");
    public static final ErrorType InputTimeMissing     = new ErrorType("error.web.input.date.notime", "No time specified.");

    // Validation errors
    public static final ErrorType InputValueRequired   = new ErrorType("error.web.input.required",   "Field is required"); 
    public static final ErrorType InputValueOutOfRange = new ErrorType("error.web.input.outOfRange", "The value must be in the range of {0} and {1}.");
    public static final ErrorType InputTextTooShort    = new ErrorType("error.web.input.textTooShort",  "The value must contain at least {0} characters."); 
}
