package org.apache.empire.struts2.interceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.action.ActionAccessValidator;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;


@SuppressWarnings("serial")
public class ActionAccessInterceptor extends InterceptorSupport
{
    // Logger
    @SuppressWarnings("hiding")
    protected static Log log = LogFactory.getLog(ActionAccessInterceptor.class);

    private String loginAction = null;
    
    private String accessDeniedAction = null;
    
    public void setLoginAction(String loginAction)
    {
        this.loginAction = loginAction;
    }

    public void setAccessDeniedAction(String accessDeniedAction)
    {
        this.accessDeniedAction = accessDeniedAction;
    }

    @Override
    public String intercept(ActionInvocation invocation) throws Exception
    {
        // Set the action object to the HttpRequest
        Object action = invocation.getAction();
        // Check Login
        if (action instanceof ActionAccessValidator)         
        {
            // Check wether login is required
            if (loginAction!=null && ((ActionAccessValidator)action).loginRequired())
            {   // Log Info
                if (log.isWarnEnabled())
                    log.warn("Access to action " + action.getClass().getName() + " requires user login. Redirecting to " + loginAction);
                // redirect to login page
                return redirect(loginAction, true); 
            }
            // Check user has access to the action and the desired method
            if (accessDeniedAction!=null)
            {
                ActionProxy proxy = invocation.getProxy();
                if (((ActionAccessValidator)action).hasAccess(proxy.getMethod())==false)
                {   // Log Info
                    if (log.isWarnEnabled())
                        log.warn("Access to action " + action.getClass().getName() + " method " + proxy.getMethod() + " has been denied. Redirecting to " + accessDeniedAction);
                    // redirect to Access Denied page
                    return redirect(accessDeniedAction, true); 
                }
            }
        }
        // Call base
        return invocation.invoke();
    }
}
