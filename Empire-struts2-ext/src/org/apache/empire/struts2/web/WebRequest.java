package org.apache.empire.struts2.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface WebRequest
{    
    /**
     * This String is the attribute name of this request object on the applications request scope
     */
    public final String REQUEST_NAME  = "webRequest";
    
    /**
     * Initializes the request object
     * This method is called from the EmpiretrutsDispatcher
     *  
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @param session the sessionObject
     * 
     * @return true if the request should continue processing or false otherwise
     */
    boolean init(HttpServletRequest request, HttpServletResponse response, Object session);

    /**
     * returns the current HttpRequestObject
     * @return the httpServletRequest
     */
    public HttpServletRequest getHttpRequest();

    /**
     * returns the current HttpResponseObject
     * @return the httpServletResponse
     */
    public HttpServletResponse getHttpResponse();
    
    /**
     * This function is called from the EmpireStrutsDispatcher when a request ends
     * if an action was accociated with the request and the action implements the Disposible interface
     * then the exit code returned by Disposible.dispose() is passed with the exitCode parameter
     * 
     * You might use the exitCode e.g. to commit or rollback a transaction on the JDBC-Connection
     * 
     * @param exitCode
     */
    void exit(int exitCode);
}
