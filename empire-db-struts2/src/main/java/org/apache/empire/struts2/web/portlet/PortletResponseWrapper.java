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
package org.apache.empire.struts2.web.portlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.portlet.ActionResponse;
import javax.portlet.PortletResponse;
import javax.portlet.RenderResponse;
import javax.servlet.http.Cookie;

import org.apache.empire.struts2.web.ResponseContext;

public class PortletResponseWrapper implements ResponseContext {
	
	private static final String METHOD_NOT_SUPPORTED = "Method not supported";

	private PortletResponse res;
	public PortletResponseWrapper(PortletResponse res) {
		this.res = res;
	}

	public Object getExternalResponse() {
		return res;
	}
	
    // Action methods
	public void addCookie(Cookie cookie) {
		res.addProperty(cookie.getName(), cookie.getValue());
	}
	public String encodeURL(String url) {
		return res.encodeURL(url);
	}
	public String encodeRedirectURL(String url) {
		return res.encodeURL(url);
	}
	public void sendRedirect(String location) throws IOException {
		if (res instanceof ActionResponse)
			((ActionResponse)res).sendRedirect(location);
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}

    // Render methods
	public Locale getLocale() {
		if (res instanceof RenderResponse)
			return ((RenderResponse)res).getLocale();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public String getCharacterEncoding() {
		if (res instanceof RenderResponse)
			return ((RenderResponse)res).getCharacterEncoding();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public String getContentType() {
		if (res instanceof RenderResponse)
			return ((RenderResponse)res).getContentType();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public void setContentType(String type) {
		if (res instanceof RenderResponse)
			((RenderResponse)res).setContentType(type);
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public int getBufferSize() {
		if (res instanceof RenderResponse)
			return ((RenderResponse)res).getBufferSize();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public void setBufferSize(int size) {
		if (res instanceof RenderResponse)
			((RenderResponse)res).setBufferSize(size);
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public PrintWriter getWriter() throws IOException {
		if (res instanceof RenderResponse)
			return ((RenderResponse)res).getWriter();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public OutputStream getOutputStream() throws IOException {
		if (res instanceof RenderResponse)
			return ((RenderResponse)res).getPortletOutputStream();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public void flushBuffer() throws IOException {
		if (res instanceof RenderResponse)
			((RenderResponse)res).flushBuffer();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public void resetBuffer() {
		if (res instanceof RenderResponse)
			((RenderResponse)res).resetBuffer();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public boolean isCommitted() {
		if (res instanceof RenderResponse)
			return ((RenderResponse)res).isCommitted();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
	public void reset() {
		if (res instanceof RenderResponse)
			((RenderResponse)res).reset();
		else
			throw new RuntimeException(METHOD_NOT_SUPPORTED);
	}
}
