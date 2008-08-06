/*
 * ESTEAM Software GmbH, 13.07.2007
 */
package org.apache.empire.struts2.interceptors;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.views.util.UrlHelper;

import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public abstract class InterceptorSupport extends AbstractInterceptor
{
    // Logger
    protected static Log log = LogFactory.getLog(InterceptorSupport.class);
    
    public static final String NONE = "none";
    
    protected String redirect(String target, boolean appendSourceUri)
        throws IOException
    {   
        HttpServletRequest req = ServletActionContext.getRequest();
        // Create rediect URL
        StringBuffer url = new StringBuffer();
        if (target.indexOf('/')<0)
        {
            url.append(req.getContextPath());
            url.append('/');
        }
        // The Target
        url.append(target);
        // Add .action
        if (target.indexOf('?')<0 && target.indexOf('.')<0)
        {
            url.append(".action");
        }
        // Apppend URI
        if (appendSourceUri)
        {   
            url.append(target.indexOf('?')<0 ? '?' : '&');
            url.append( "source-uri=" );
            url.append( UrlHelper.translateAndEncode(req.getRequestURI()) ); 
        }
        // Log Info
        if (log.isInfoEnabled())
            log.info("Redirecting request from " + req.getRequestURI() + " to " + url.toString());
        // Redirect
        HttpServletResponse response = ServletActionContext.getResponse();
        String redirectURL = response.encodeRedirectURL(url.toString());
        response.sendRedirect( redirectURL );
        return NONE; 
    }

}
