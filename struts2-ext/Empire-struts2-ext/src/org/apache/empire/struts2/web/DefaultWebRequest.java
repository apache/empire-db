package org.apache.empire.struts2.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DefaultWebRequest implements WebRequest
{    
    private HttpServletRequest  httpRequest;
    private HttpServletResponse httpResponse; 
    
    public boolean init(HttpServletRequest request, HttpServletResponse response, Object session)
    {
        this.httpRequest = request;
        this.httpResponse = response;
        return true;
    }

    /**
     * @see WebRequest#getHttpRequest()
     */
    public HttpServletRequest getHttpRequest()
    {
        return httpRequest;
    }

    /**
     * @see WebRequest#getHttpResponse()
     */
    public HttpServletResponse getHttpResponse()
    {
        return httpResponse;
    }
    
    /**
     * @see WebRequest#exit(int)
     */
    public void exit(int exitCode)
    {
        // nothing to do
    }
}
