package org.apache.empire.jsf2.websample.web;

import java.util.Iterator;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.app.TextResolver;
import org.apache.empire.jsf2.app.WebApplication;
import org.apache.empire.jsf2.websample.web.pages.SamplePages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppExceptionHandler extends ExceptionHandlerWrapper
{

    private static final Logger    log = LoggerFactory.getLogger(AppExceptionHandler.class);

    private final ExceptionHandler wrapped;

    public AppExceptionHandler(ExceptionHandler delegate)
    {
        // super(delegate);
        this.wrapped = delegate;
    }

    @Override
    public javax.faces.context.ExceptionHandler getWrapped()
    {
        return this.wrapped;
    }

    @Override
    public void handle()
        throws FacesException
    {
        // boolean redirectToErrorPage = false;
        Throwable rootCause = null;
        Iterator<ExceptionQueuedEvent> events = getUnhandledExceptionQueuedEvents().iterator();

        // log each error
        while (events.hasNext())
        {
            ExceptionQueuedEvent event = events.next();
            // handle
            try {
            
                ExceptionQueuedEventContext source = (ExceptionQueuedEventContext) event.getSource();
                FacesContext context = source.getContext();
                Throwable t = source.getException();
                
                // check t
                if (t==null)
                {   log.error("Cannot handle exception. Exception not supplied with context!");
                    setErrorMessage(context, null);
                    continue;
                }    

                // find root
                rootCause = t.getCause();
                // second option: getRootCause
                if (rootCause == null)
                {   // get cause
                    rootCause = getRootCause(t);
                }
                // third option: use t
                if (rootCause == null)
                {
                    rootCause = t;
                }
                
                // Walk up the tree
                while (true)
                {   // if its an empire-exception: game over
                    if (rootCause instanceof EmpireException)
                        break;
                    // has root cause
                    t = rootCause.getCause();
                    if (t==null)
                        break; 
                    // yes, continue search
                    rootCause = t;
                }
                
                /*
                if (rootCause instanceof org.icefaces.application.SessionExpiredException)
                {   // expired
                    log.info("Handling SessionExpiredException. No error message is set.");
                    continue;
                }
                */

                // set message
                String msg = "Handling exception of type "+rootCause.getClass().getSimpleName();
                // log
                // msg = appendSessionInfo(context, msg);
                log.error(msg, rootCause);
                
                // set message
                if (!(rootCause instanceof EmpireException))
                {   // Wrap as internal exception
                    rootCause = new InternalException(rootCause);
                }
                setErrorMessage(context, rootCause);
                
            } catch(Throwable t2) {
                log.error("Failed to handle exception: "+t2.getMessage(), t2);
            } finally {
                events.remove();
            }
        }

        // if an error has been found
        /*
        if (redirectToErrorPage)
        {
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            try
            {
                // put error to session map (will be cleared by ErrorPage)
                if (rootCause != null)
                {
                    FacesUtils.getFin2Session().setError(rootCause);
                }
                // redirect to error page
                String errorPage = FacesUtils.getContextPath() + "/pages/error.iface";
                Fin2ExceptionHandler.log.debug("Redirecting to error page at '" + errorPage + "'...");
                FacesUtils.redirectDirectly(errorPage);
            }
            catch (Exception e)
            {
                Fin2ExceptionHandler.log.error("Error during exception handling.", e);
                throw new FacesException(e);
            }
        }
        */
        // let next handler deal
        getWrapped().handle();
    }
    
    /*
    private String appendSessionInfo(FacesContext context, String msg)
    {
        // Provide session info
        Fin2Session session = FinUtils.getFin2Session(context, false);
        Fin2User user = (session!=null ? session.getUser() : null);
        if (user!=null)
        {   // Add user information
            String viewId;
            if (context.getViewRoot()!=null)
                viewId = context.getViewRoot().getViewId();
            else {
                viewId = "[NO VIEW]";
                if (context.getExternalContext()!=null)
                {   // External Context 
                    Object request = context.getExternalContext().getRequest(); 
                    if (request instanceof HttpServletRequest) 
                    {   // The HttpServletRequest Servlet Path
                        String path = ((HttpServletRequest)request).getServletPath();
                        viewId += " ("+String.valueOf(path)+")";
                    }    
                    else if (request!=null)
                        viewId += " {"+request.getClass().getName()+"}";
                }
            }
            // Message
            msg += MessageFormat.format(" for User {0} of DUNS {1} on View {2}.", user.getUserId(), user.getDUNS(), viewId);
        }
        return msg;
    }
    */
    
    private void setErrorMessage(FacesContext fContext, Throwable rootCause)
    {
        FacesMessage message = new FacesMessage();
        message.setSeverity(FacesMessage.SEVERITY_ERROR);
     
        Map<String, Object> sm = fContext.getExternalContext().getSessionMap();
        if (sm.containsKey(SampleSession.MANAGED_BEAN_NAME))
        {
            TextResolver resolver = WebApplication.getInstance().getTextResolver(fContext);
            if (rootCause instanceof Exception)
            {
                message.setSummary(resolver.getExceptionMessage((Exception)rootCause));
            }
            else
            {
                message.setSummary(resolver.resolveKey("global_general_error"));
            }
            // Add message
            fContext.addMessage(null, message);
        }
        else
        {
            log.error("Unable to handle exception, redirecting to StartPage.", rootCause);
            FacesUtils.redirectDirectly(fContext, SamplePages.LoginPage);
        }
    }
}
