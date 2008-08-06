/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.commons;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The ErrorType class defines a type of error.
 * In order to define an error you need to provide an ErrorKey and a MessagePattern.
 * <P>
 * The ErrorKey is a unique identifier for the error that may also be used as a translation key
 * The ErrorKey should always start with the "error." prefix. 
 * <P>
 * The MessagePattern is a template containing the error message and placeholders for additional parameters.
 * The MessagePattern must be formated according to the {@link java.text.MessageFormat} rules.
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class ErrorType
{
    // Logger
    private static final Log log = LogFactory.getLog(ErrorType.class);

    private String key;
    private String msgPattern;
    private int    numParams;

    /**
     * Defines an error type.
     * 
     * @param errorKey the error key string (can be used for internationalization)
     * @param msgPattern message pattern in english language used e.g. for logging
     */
    public ErrorType(String errorKey, String msgPattern)
    {
        this.key = errorKey;
        this.msgPattern = msgPattern;
        // Count number of params
        numParams = 0;
        while(true)
        {
            String placeholder = "{" + String.valueOf(numParams) + "}";
            if (msgPattern.indexOf(placeholder)<0)
                break;
            // Param found
            numParams++;    
        }
        // Write error definition to log
        log.info("Error defined: " + key + "=" + msgPattern);
    }

    /**
     * Returns the error type key (might be used for internationalization).
     * 
     * @return the error type key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * Returns the message pattern.
     * 
     * @return the message pattern
     */
    public String getMessagePattern()
    {
        return msgPattern;
    }

    /**
     * Returns the number of parameters required for the message pattern.
     * 
     * @return the number of parameters required for the message pattern
     */
    public int getNumParams()
    {
        return numParams;
    }
}
