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
package org.apache.empire.struts2.html;


public class DefaultHtmlTagDictionary extends HtmlTagDictionary
{
    // ------- Misc -------

    @Override
    public String FloatClear()
    {
        return "<div style=\"clear:both;\"><!-- --></div>";
    }

    // ------- Flexible Tag -------
    
    private static final FlexDivRenderInfo flexDivDefault = new FlexDivRenderInfo("div", null, null, null);
    
    @Override
    public FlexDivRenderInfo FlexDivTag(String type, String userAgent)
    {
        return flexDivDefault;
    }
    
    // ------- Form -------

    @Override
    public String FormDefaultOnSubmitScript()
    {
        return null;
    }

    @Override
    public boolean FormDefaultRenderWrapper()
    {
        return true;
    }
    
    @Override
    public String FormPartWrapperTag()
    {
        return "table";
    }
    
    @Override
    public String FormPartWrapperClass()
    {
        return "inputForm";
    }
    
    @Override
    public String FormPartWrapperAttributes()
    {
        return "cellspacing=\"1\" cellpadding=\"0\"";
    }

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
    public String InputControlTag()
    {
        return "td";
    }
    
    @Override
    public String InputControlClass()
    {
        return "inputControl";
    }
    
    @Override
    public String InputReadOnlyClass()
    {
        return InputControlClass(); 
    }
    
    @Override
    public String InputReadOnlyDataWrapperTag()
    {
        return "div";
    }

    @Override
    public String InputLabelTag()
    {
        return "td";
    }

    @Override
    public String InputLabelClass()
    {
        return "inputLabel";
    }

    @Override
    public String InputRequiredTag()
    {
        return "span";
    }

    @Override
    public String InputRequiredClass()
    {
        return "inputRequired";
    }

    @Override
    public String InputWrapperClass()
    {
        return null;
    }

    @Override
    public String InputWrapperTag()
    {
        return "tr";
    }

    @Override
    public String InputWrapperBody()
    {
        return null; // e.g. "<div class=\"clearBoth\"><!-- ? --></div>"
    }

    @Override 
    public String InputDefaultClass(String type, boolean disabled)
    {
        return null;
    }
    
    // ------- Anchor -------
    
    @Override
    public String AnchorDisabledTag()
    {
        return "span";
    }

    @Override
    public String AnchorDisabledClass()
    {
        return null;
    }

    // ------- Button -------
    
    @Override
    public String ButtonTag()
    {
        return "div";
    }
    
    @Override
    public String ButtonClass()
    {
        return "button";
    }
    
    @Override
    public String ButtonSameActionDefaultOnClickScript()
    {
        return null;
    }

    @Override
    public String ButtonOtherActionDefaultOnClickScript()
    {
        return null;
    }
    
    // ------- Submit Button -------
    
    @Override
    public String SubmitClass()
    {
        return null;
    }

    @Override
    public String SubmitControlTag()
    {
        return "td";
    }

    @Override
    public String SubmitControlClass()
    {
        return null;
    }

    @Override
    public String SubmitControlAttributes()
    {
        return "colspan=\"2\" align=\"right\"";
    }

    // ------- Menu Tags -------
    
    @Override
    public String MenuTag()
    {
        return "ul";
    }

    @Override
    public String MenuItemTag()
    {
        return "li";
    }

    @Override
    public String MenuItemCurrentClass()
    {
        return "current";
    }

    @Override
    public String MenuItemDisabledClass()
    {
        return "disabled";
    }
    
    @Override
    public String MenuItemExpandedClass()
    {
        return "expanded";
    }

    @Override
    public String MenuItemLinkClass()
    {
        return null;
    }
    
    @Override
    public String MenuItemLinkDefaultOnClickScript()
    {
        return null;
    }
    
    // ------- TableHead -------
    
    @Override
    public String TableHeadRowTag()
    {
        return "tr";
    }
    
    @Override
    public String TableHeadColumnTag()
    {
        return "th";
    }
    
    @Override
    public String TableHeadColumnLinkEnabledClass()
    {
        return null;
    }
    
    @Override
    public String TableHeadColumnLinkDisabledClass()
    {
        return TableHeadColumnLinkEnabledClass();
    }
    
    @Override
    public String TableHeadColumnLinkCurrentClass()
    {
        return TableHeadColumnLinkEnabledClass();
    }
    
    @Override
    public String TableHeadColumnLinkCurrentAscendingClass()
    {
        return TableHeadColumnLinkCurrentClass();
    }
    
    @Override
    public String TableHeadColumnLinkCurrentDescendingClass()
    {
        return TableHeadColumnLinkCurrentClass();
    }
    
    @Override
    public String TableHeadSortAscendingIndicator()
    {
        return "&nbsp;/\\";
    }
    
    @Override
    public String TableHeadSortDescendingIndicator()
    {
        return "&nbsp;\\/";
    }
    
    @Override
    public String TableHeadSelectColumnIndicator()
    {
        return "&nbsp;[X]";
    }
    
    @Override
    public String TableHeadSelectAscendingIndicator()
    {
        return TableHeadSortAscendingIndicator();
    }
    
    @Override
    public String TableHeadSelectDescendingIndicator()
    {
        return TableHeadSortDescendingIndicator();
    }
    
    @Override
    public String TableHeadLinkDefaultOnClickScript()
    {
        return null;
    }

    // ------- Pager -------
    
    @Override
    public String PagerTag()
    {
        return "div";
    }

    @Override
    public String PagerClass()
    {
        return "pager";
    }

    @Override
    public String PagerPageTag()
    {
        return null;
    }

    @Override
    public String PagerPageClass()
    {
        return null;
    }

    @Override
    public String PagerLinkClass()
    {
        return "pagerPage";
    }

    @Override
    public String PagerLabelTag()
    {
        return "span";
    }

    @Override
    public String PagerLabelText()
    {
        return null;
    }
    
    @Override
    public String PagerLabelClass()
    {
        return "pagerLabel";
    }
    
    @Override
    public String PagerPaddingText()
    {
        return null;
    }
    
    @Override
    public String PagerLinkDefaultOnClickScript()
    {
        return null;
    }

    @Override
    public String PagerFirstPageText()
    {
        return "[|<]";
    }

    @Override
    public String PagerRewindText()
    {
        return "[<]";
    }

    @Override
    public String PagerForwardText()
    {
        return "[>]";
    }

    @Override
    public String PagerLastPageText()
    {
        return "[>|]";
    }

    @Override
    public String PagerFirstPageTextDisabled()
    {
        return "&nbsp;";
    }

    @Override
    public String PagerRewindTextDisabled()
    {
        return "&nbsp;";
    }

    @Override
    public String PagerForwardTextDisabled()
    {
        return "&nbsp;";
    }

    @Override
    public String PagerLastPageTextDisabled()
    {
        return "&nbsp;";
    }

    // ------- PageInfo -------

    @Override
    public String PageInfoTag()
    {
        return "span";
    }

    @Override
    public String PageInfoClass()
    {
        return "pageInfo";
    }

    @Override
    public String PageInfoItemTag()
    {
        return "strong";
    }

    @Override
    public String PageInfoLabel()
    {
        return null;
    }

    @Override
    public String PageInfoLabelTo()
    {
        return "-";
    }

    @Override
    public String PageInfoLabelOf()
    {
        return "/";
    }

    @Override
    public String PageInfoLabelPadding()
    {
        return "&nbsp;";
    }

    // ------- Errors -------

    @Override
    public String ErrorListTag()
    {
        return "ul";
    }

    @Override
    public String ErrorListClass()
    {
        return "errorList";
    }

    @Override
    public String ErrorEntryTag()
    {
        return "li";
    }

    @Override
    public String ErrorItemEntryClass()
    {
        return "itemError";
    }

    @Override
    public String ErrorActionEntryClass()
    {
        return "actionError";
    }

    @Override
    public String ErrorEntryWrapperTag()
    {
        return null;
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
