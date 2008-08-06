/*
 * ESTEAM Software GmbH, 09.07.2007
 */
package org.apache.empire.struts2.action;

public interface Disposable
{
    /**
     * This exit-code might be used to indicate a successful action from the dispose method
     * The code will be forwarded to the WebRequest's exit method (see WebRequest.exit())  
     */
    public final int EXITCODE_SUCCESS =  0;  // The Action ended successfully

    /**
     * This Exit-code might be used to indicate an action error from the dispose method
     * The code will be forwarded to the request's exit method (see below)  
     */
    public final int EXITCODE_ERROR   = -1;  // The Action ended with an error

    /**
     * Initializes the object
     * Use this to initialize Action objects instead of the Action's constructor
     */
    void init();

    /**
     * Cleanup resources allocated by the object
     * 
     * @return an exit code which will be passed to the WebRequest's exit function (@see WebRequest.exit())
     */
    int dispose();
}
