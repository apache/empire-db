/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.jsf2.websample.web;

import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleExceptionHandler extends ExceptionHandlerWrapper
{

    private ExceptionHandler    wrapped;

    private static final Logger log = LoggerFactory.getLogger(SampleExceptionHandler.class);

    public SampleExceptionHandler(ExceptionHandler wrapped)
    {
        this.wrapped = wrapped;
    }

    @Override
    public ExceptionHandler getWrapped()
    {
        return this.wrapped;
    }

    @Override
    public void handle()
    throws FacesException
    {
        boolean redirectToErrorPage = false;
        Iterator<ExceptionQueuedEvent> events = getUnhandledExceptionQueuedEvents().iterator();
        Throwable rootCause = null;

        // log each error
        while (events.hasNext())
        {
            ExceptionQueuedEvent event = events.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();
            Throwable t = context.getException();
            redirectToErrorPage = true;
            log.error("SampleExceptionHandler caught exception.", t);
            rootCause = getRootCause(t);
            events.remove();
        }

        // if an error has been found
        /*
        if (redirectToErrorPage)
        {
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            try
            {
                if (rootCause != null)
                {
                    FacesUtils.getSampleSession().setError(rootCause);
                }
                response.sendRedirect("error.iface");
            }
            catch (Exception e)
            {
                SampleExceptionHandler.log.error("SampleExceptionHandler produced exception.", e);
                throw new FacesException(e);
            }
        }
        */

        // let next handler deal
        // getWrapped().handle();
    }
}
