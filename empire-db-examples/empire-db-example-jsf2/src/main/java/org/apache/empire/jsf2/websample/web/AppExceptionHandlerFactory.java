package org.apache.empire.jsf2.websample.web;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

/**
 * AppExceptionHandlerFactory
 */
public class AppExceptionHandlerFactory extends ExceptionHandlerFactory
{
    ExceptionHandlerFactory delegateFactory;

    public AppExceptionHandlerFactory(ExceptionHandlerFactory delegateFactory)
    {
        this.delegateFactory = delegateFactory;
    }

    @Override
    public ExceptionHandlerFactory getWrapped()
    {
        return delegateFactory;
    }

    @Override
    public ExceptionHandler getExceptionHandler()
    {
        return new AppExceptionHandler(getWrapped().getExceptionHandler());
    }
}
