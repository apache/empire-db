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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.Cookie;

public interface ResponseContext {

	Object getExternalResponse();
	
    // Action methods
	void addCookie(Cookie cookie);
    String encodeURL(String url);
    String encodeRedirectURL(String url);
    void sendRedirect(String location) throws IOException;

    // Render methods
    Locale getLocale();
    String getCharacterEncoding();
    String getContentType();
    void setContentType(String type);
    int getBufferSize();
    void setBufferSize(int size);
    PrintWriter getWriter() throws IOException;
    OutputStream getOutputStream() throws IOException;
    void flushBuffer() throws IOException;
    void resetBuffer();
    boolean isCommitted();
    void reset();
    
}
