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
package org.apache.empire.struts2.web;

import java.io.IOException;
import java.util.Map;

import javax.portlet.ActionResponse;
import javax.portlet.PortletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.struts2.action.WebAction;
import org.apache.struts2.portlet.PortletActionConstants;
import org.apache.struts2.portlet.context.PortletActionContext;
import org.apache.struts2.portlet.result.PortletResult;

import com.opensymphony.xwork2.ActionInvocation;

public class EmpirePortletResult extends PortletResult
{
	private static final long serialVersionUID = -6883272432993216278L;

    protected static Log log = LogFactory.getLog(EmpirePortletResult.class);
	
	private String renderMethod = "renderPortlet";
	
	public EmpirePortletResult() {
		super();
	}

	public EmpirePortletResult(String location) {
		super(location);
	}
	
	public String getRenderMethod() {
		return renderMethod;
	}

	public void setRenderMethod(String renderMethod) {
		this.renderMethod = renderMethod;
	}
	
	@Override
	protected void executeRenderResult(final String finalLocation) 
		throws PortletException, IOException 
	{
		super.executeRenderResult(finalLocation);
	}
	
	@Override
	protected void executeActionResult(String finalLocation, ActionInvocation invocation) 
		throws Exception 
	{
		// Call base
		super.executeActionResult(finalLocation, invocation);

		// Handle Deferred Rendering
		Map<String, Object> sessionMap = invocation.getInvocationContext().getSession();
        if (sessionMap.containsKey(PortletActionConstants.RENDER_DIRECT_LOCATION)) 
        {	/*
        	 *  View is rendered outside an action...uh oh...
        	 */
        	String actionName = invocation.getProxy().getActionName();
            String resultCode = invocation.getResultCode();

            // create action name
        	String namespace = invocation.getProxy().getNamespace();
            if (namespace != null && namespace.length() > 0 && !namespace.endsWith("/")) {
                namespace += "/";   
            }
            String action = namespace + actionName + "!" + renderMethod;
            if (log.isInfoEnabled())
	            log.info("Diverting Portlet render action: " + action);

            // set action name
            ActionResponse res = PortletActionContext.getActionResponse();
            res.setRenderParameter(PortletActionConstants.ACTION_PARAM, action);
            sessionMap.put(WebAction.PORTLET_ACTION_RESULT, resultCode);

            // remove RENDER_DIRECT_LOCATION 
			sessionMap.remove(RENDER_DIRECT_LOCATION);
        }   
	}
}
