/*
 * ESTEAM Software GmbH, 22.07.2007
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
