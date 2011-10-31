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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleRequestFilter implements Filter
{
    private static final Logger log = LoggerFactory.getLogger(SampleRequestFilter.class);

    public void init(FilterConfig filterConfig)
    throws ServletException
    {
        SampleRequestFilter.log.info("SampleRequestFilter.init");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException
    {
        // Perform checks (should never fail)
        // Code may be removed later
    	SampleRequest check = (SampleRequest) request.getAttribute(SampleRequest.REQUEST_ATTRIBUTE_NAME);
        if (check != null)
        { // HttpRequest already has an attribute for FWSRequest
            SampleRequestFilter.log.error("HttpRequest already has a SampleRequest.");
            throw new RuntimeException("HttpRequest already has an SampleRequest.");
        }
        check = SampleRequest.get();
        if (check != null)
        { // Thread already has an attribute for AppRequest
            SampleRequestFilter.log.error("Thread already has an AppRequest.");
            throw new RuntimeException("Thread already has an AppRequest.");
        }

        // Handle request
        SampleRequest appRequest = new SampleRequest(request, response);
        try
        {
            request.setAttribute(SampleRequest.REQUEST_ATTRIBUTE_NAME, appRequest);
            if (SampleRequestFilter.log.isDebugEnabled())
            {
                SampleRequestFilter.log.debug("Serving request {}.", appRequest.getId());
            }
            // Process chain
            chain.doFilter(request, response);
        }
        catch (Throwable e)
        {
            appRequest.setFailure(e);
            // Forward exception
            throw new RuntimeException(e);
        }
        finally
        {
            // Dispose
            request.removeAttribute(SampleRequest.REQUEST_ATTRIBUTE_NAME);
            appRequest.dispose();
        }
    }

    public void destroy()
    {
        SampleRequestFilter.log.info("SampleRequestFilter.destroy");
    }
}
