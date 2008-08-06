/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.commons;

import java.text.MessageFormat;

/**
 * This class holds the definition of common error types.
 * No instances of this class can be created. It's sole
 * purpose is to hold the definition of Error Types.
 * <P>
 * Define your own error types in the same way.
 * See @link {@link ErrorType} for futher information.
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class Errors
{
    // No Error
    public static final ErrorType None            = new ErrorType("error.none", "");
    public static final ErrorType Cancelled       = new ErrorType("error.cancelled", "The action has been cancelled by the user");
    // Code Errors
    public static final ErrorType Exception       = new ErrorType("error.exception", "An Exception of type {0} occurred.\n-->Message is {1}\n-->at Position {2}");
    public static final ErrorType Internal        = new ErrorType("error.internal", "Internal Error: {0}");
    public static final ErrorType InvalidArg      = new ErrorType("error.invalidarg", "Invalid Argument {0} for parameter {1}.");
    public static final ErrorType NotSupported    = new ErrorType("error.notsupported", "The function {0} is not supported");
    public static final ErrorType NotImplemented  = new ErrorType("error.notimplemented", "The function {0} is not implemented");
    public static final ErrorType ObjectNotValid  = new ErrorType("error.objectnotvalid", "The object {0} has not been initialized.");
    public static final ErrorType InvalidProperty = new ErrorType("error.invalidproperty", "The value of the property {0} is not valid.");
    // Security Errors
    public static final ErrorType NoAccess        = new ErrorType("error.noaccess", "Access denied");
    public static final ErrorType NotAuthorized   = new ErrorType("error.notauthorized", "You are not autorized for this operation.");
    public static final ErrorType InvalidPassword = new ErrorType("error.invalidpassword", "Invalid username or password.");
    // Item-Error
    public static final ErrorType OutOfRange      = new ErrorType("error.outofrange", "The value of {0} is out of range.");
    public static final ErrorType ItemNotFound    = new ErrorType("error.itemnotfound", "The element {0} was not found.");
    public static final ErrorType ItemExists      = new ErrorType("error.itemexists", "The element {0} already exists.");
    public static final ErrorType IllegalValue    = new ErrorType("error.illegalvalue", "The value {0} is invalid.");
    public static final ErrorType IllegalFormat   = new ErrorType("error.illegalformat", "The format of {0} is invalid for {1}");
    public static final ErrorType NoResult        = new ErrorType("error.noresult", "No data available for {0}");
    public static final ErrorType NoMoreData      = new ErrorType("error.nomoredata", "No more data available.");
    // File-Errors
    public static final ErrorType FileNotFound    = new ErrorType("error.filenotfound", "The file {0} was not found.");
    public static final ErrorType FileExists      = new ErrorType("error.fileexits", "The file {0} already exists");
    public static final ErrorType FileFormatError = new ErrorType("error.fileformaterror", "The format of file {0} has not been recognized.");
    public static final ErrorType FileReadError   = new ErrorType("error.filereaderror", "Error reading file {0}");
    public static final ErrorType FileWriteError  = new ErrorType("error.filewriteerror", "Error creating or writing file {0}");
    public static final ErrorType PathNotFound    = new ErrorType("error.pathnotfound", "The directory {0} does not exists.");
    public static final ErrorType PathCreateFailed= new ErrorType("error.pathcreatefailed", "Error creating the directory {0}");

    /**
     *  No instances of this class can be created.
     *  It's sole purpose is to hold the definition of Error Types
     */
    private Errors()
    {
        // see comment.
    }
    
    /**
     *  Gets an error Message from an object implementing the error info interface.
     */
    public static String getErrorMessage(ErrorInfo info)
    {
        // Check Param
        if (info==null || info.hasError()==false)
            return ""; // No Error
        // Get Error Type
        ErrorType type = info.getErrorType();
        if (type==null || type==Errors.None)
            return ""; // No Error
        // Get Message Pattern and Params
        String msgPattern = type.getMessagePattern();
        Object[] msgParams = info.getErrorParams();
        if (msgParams==null)
            return msgPattern;
        // Get Error Message
        String msg = MessageFormat.format(msgPattern, msgParams);
        return msg;
    }
}
