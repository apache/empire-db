/*
 * ESTEAM Software GmbH, 14.07.2007
 */
package org.apache.empire.db;

import org.apache.empire.commons.ErrorType;

/**
 * This class contains the definition of database specific errors.
 * <P>
 * For more informations see {@link org.apache.empire.commons.Errors}
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBErrors
{
    // Database
    public static final ErrorType DatabaseNotOpen        = new ErrorType("error.db.databasenotopen",
                                                                         "the database has not been opened.");
    public static final ErrorType ConnecitonInvalid      = new ErrorType("error.db.connectioninvalid",
                                                                         "the database connection is not valid");
    public static final ErrorType SQLException           = new ErrorType("error.db.sqlexception",
                                                                         "The database operation failed. Native error is {0}.");
    public static final ErrorType ObjectNotFound         = new ErrorType("error.db.objectnotfound",
                                                                         "an object of type {0} named {1} does not exists or is inaccessible.");
    public static final ErrorType ObjectCreationFailed   = new ErrorType("error.db.objectcreationfailed",
                                                                         "error creating the {0} of name {1}.");
    public static final ErrorType NoPrimaryKey           = new ErrorType("error.db.noprimarykey",
                                                                         "No primary key is defined for {0}");
    // Query errors
    public static final ErrorType QueryFailed            = new ErrorType("error.db.queryfailed",
                                                                         "The query failed. Native error is {0}.");
    public static final ErrorType QueryNoResult          = new ErrorType("error.db.querynoresult",
                                                                         "No records found for query {0}");
    // Record errors
    public static final ErrorType RecordInvalidKey       = new ErrorType("error.db.recordinvalidkey", "Invalid record key {0}");
    public static final ErrorType RecordNotFound         = new ErrorType("error.db.recordnotfound",
                                                                         "The record {0} does not exist. It might have been deleted by another user.");
    public static final ErrorType RecordUpdateFailed     = new ErrorType("error.db.recordupatefailed",
                                                                         "Updating the record {0} failed. It might have been changed or deleted by another user.");
    public static final ErrorType RecordUpdateInvalid    = new ErrorType("error.db.recordupateinvalid",
                                                                         "Updating the record {0} failed. The given record key is ambiguous.");
    public static final ErrorType RecordDeleteFailed     = new ErrorType("error.db.recorddeletefailed",
                                                                         "Deleting the record {0} failed. The record might have been deleted already by another user.");
    // Field errors
    public static final ErrorType FieldIsReadOnly        = new ErrorType("error.db.fieldisreadonly",
                                                                         "The field {0} is read only.");
    public static final ErrorType FieldValueTooLong      = new ErrorType("error.db.fieldvaluetoolong",
                                                                         "The value supplied for field {0} is too long. The maximum number of characters is {1}.");
    public static final ErrorType FieldNotNull           = new ErrorType("error.db.fieldnotnull",
                                                                         "The value for field {0} must not be null.");
    public static final ErrorType FieldNotNumeric        = new ErrorType("error.db.fieldnotnumeric",
                                                                         "The field value supplied for field {0} is not numeric.");
    public static final ErrorType FieldInvalidDateFormat = new ErrorType("error.db.fieldinvaliddateformat",
                                                                         "The date supplied for field {0} is not valid.");
    public static final ErrorType FieldInvalidValue      = new ErrorType("error.db.fieldinvalidvalue",
                                                                         "The value for field {0} is invalid.");
}
