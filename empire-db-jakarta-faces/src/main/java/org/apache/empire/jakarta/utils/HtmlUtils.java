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
package org.apache.empire.jakarta.utils;

import org.apache.empire.commons.StringUtils;

public class HtmlUtils
{
    private static HtmlUtils htmlUtils = new HtmlUtils();

    public static HtmlUtils getInstance()
    {
        return htmlUtils;
    }

    public static void setInstance(HtmlUtils htmlUtils)
    {
        HtmlUtils.htmlUtils = htmlUtils;
    }
    
    /**
     * escapes a String for html
     * 
     * @param text the text to escape
     * @return the escaped html String
     */
    public String escapeText(String text)
    {
        if (text==null || text.length()==0)
            return text;
        // &amp;
        if (text.indexOf('&')>=0)
            text = StringUtils.replaceAll(text, "&", "&amp;");
        // &lt;
        if (text.indexOf('<')>=0)
            text = StringUtils.replaceAll(text, "<", "&lt;");
        // &gt;
        if (text.indexOf('>')>=0)
            text = StringUtils.replaceAll(text, ">", "&gt;");
        // done
        return text;
    }
    
}
