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
package org.apache.empire.struts2.websample.web;

import org.apache.empire.struts2.html.DefaultHtmlTagDictionary;

public class SampleHtmlTagDictionary extends DefaultHtmlTagDictionary
{
    // ------- Input Control -------

    @Override
    public boolean InputReadOnlyAsData()
    {
        return false; // Show Read Only Input as Data (not as disabled input)
    }
    
    @Override
    public int InputMaxCharSize()
    {
        return 40; // Maximum horizontal size in characters
    }

    @Override
    public String InputWrapperTag()
    {
        return "tr";
    }

    @Override
    public String InputLabelTag()
    {
        return "td";
    }

    @Override
    public String InputControlTag()
    {
        return "td";
    }

    // ------- Errors -------

    @Override
    public String ErrorItemEntryClass()
    {
        return "errorMessage";
    }

    @Override
    public String ErrorActionEntryClass()
    {
        return "errorMessage";
    }

    @Override
    public String ErrorEntryWrapperTag()
    {
        return "span";
    }

    // ------- Message -------

    @Override
    public String MessageTag()
    {
        return "div";
    }

    @Override
    public String MessageClass()
    {
        return "actionMessage";
    }

}
