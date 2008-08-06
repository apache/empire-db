/*
 * ESTEAM Software GmbH, 13.07.2007
 */
package org.apache.empire.struts2.interceptors;

import org.apache.empire.struts2.action.Disposable;
import org.apache.empire.struts2.action.ExceptionAware;
import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;


@SuppressWarnings("serial")
public class ActionBasicsInterceptor extends InterceptorSupport
{
    private String errorAction = null;

    public void setErrorAction(String errorAction)
    {
        this.errorAction = errorAction;
    }

    @Override
    public String intercept(ActionInvocation invocation) throws Exception
    {
        // Set the action object to the HttpRequest
        Object action = invocation.getAction();
        // Call base
        try {
            // Log Info
            if (log.isInfoEnabled())
                log.info("Processing action " + action.getClass().getName() + " for uri= " + ServletActionContext.getRequest().getRequestURI());
            // Store action on Request
            ServletActionContext.getRequest().setAttribute("action", action);
            // Check Disposible interface and call init
            if (action instanceof Disposable)         
            {
               ((Disposable)action).init(); 
            }
            // Invoke Action
            return invocation.invoke();
            
        } catch (Exception e) {
            // catch everything here and forward exception
            ActionProxy proxy = invocation.getProxy();
            log.error("An exception occurred while processing the action " + proxy.getActionName() + "!" + proxy.getMethod(), e);
            if (action instanceof ExceptionAware)
            {   // Let action handle it
                String result = ((ExceptionAware)action).handleException(e, proxy.getMethod());
                if (result!=null && result.length()>0)
                    return result;
            }
            // redirect
            if (errorAction!=null && errorAction.length()>0)
                return redirect(errorAction, true);
            // Forward the action
            throw(e); 
        }
    }

}
