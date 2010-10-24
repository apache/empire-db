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
package org.apache.empire.struts2.web.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.empire.struts2.web.ResponseContext;

public class ServletResponseWrapper implements ResponseContext {

	private HttpServletResponse res;
	public ServletResponseWrapper(HttpServletResponse res) {
		this.res = res;
	}

	public Object getExternalResponse() {
		return res;
	}
	
    // Action methods
	public void addCookie(Cookie cookie) {
		res.addCookie(cookie);
	}
	public String encodeURL(String url) {
		return res.encodeURL(url);
	}
	public String encodeRedirectURL(String url) {
		return res.encodeRedirectURL(url);
	}
	public void sendRedirect(String location) throws IOException {
		res.sendRedirect(location);
	}

    // Render methods
	public Locale getLocale() {
		return res.getLocale();
	}
	public String getCharacterEncoding() {
		return res.getCharacterEncoding();
	}
	public String getContentType() {
		return res.getContentType();
	}
	public void setContentType(String type) {
		res.setContentType(type);
	}
	public int getBufferSize() {
		return res.getBufferSize();
	}
	public void setBufferSize(int size) {
		res.setBufferSize(size);
	}
	public PrintWriter getWriter() throws IOException {
		return res.getWriter();
	}
	public OutputStream getOutputStream() throws IOException {
		return res.getOutputStream();
	}
	public void flushBuffer() throws IOException {
		res.flushBuffer();
	}
	public void resetBuffer() {
		res.resetBuffer();
	}
	public boolean isCommitted() {
		return res.isCommitted();
	}
	public void reset() {
		res.reset();
	}
}
