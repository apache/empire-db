/*
 * ESTEAM Software GmbH, 13.07.2007
 */
package org.apache.empire.struts2.action;

public interface ExceptionAware
{
    /**
     * informs the action that an uncaught exception has occurred
     *
     * @param exception the exception that occurred
     * 
     * @return the target mapping which to execute
     */
    String handleException(java.lang.Throwable exception, String method);
}
