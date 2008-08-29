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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class HtmlTagDictionary
{
    // Logger
    protected static Log log = LogFactory.getLog(HtmlTagDictionary.class);

    private static HtmlTagDictionary htmlDictonary; 
    
    public static HtmlTagDictionary getInstance()
    {
        if (htmlDictonary==null)
        {
            log.warn("HtmlTagDictionary has not been set. Using Default Dictionary");
            htmlDictonary = new DefaultHtmlTagDictionary();
        }    
        return htmlDictonary;
    }

    public static void set(HtmlTagDictionary htmlDictionay)
    {
        htmlDictonary = htmlDictionay;
    }
    
    // ------- Misc -------
    public abstract String FloatClear(); // Complete FloatClearTag expresseion (e.g. <div style="clear:both;"></div>)

    // ------- Flexible Tag -------   
    public static class FlexDivRenderInfo
    {
        public String tag;
        public String attributes;
        public String bodyBegin;
        public String bodyEnd;
        // Konstruktor
        public FlexDivRenderInfo(String tag, String attributes, String bodyBegin, String bodyEnd)
        {
            this.tag = tag;
            this.attributes = attributes;
            this.bodyBegin = bodyBegin;
            this.bodyEnd = bodyEnd; 
        }
    }
    public abstract FlexDivRenderInfo FlexDivTag(String type, String userAgent);
    
    // ------- Form -------
    public abstract String FormDefaultOnSubmitScript();
    public abstract boolean FormDefaultRenderWrapper();
    public abstract String FormPartWrapperTag();
    public abstract String FormPartWrapperClass();
    public abstract String FormPartWrapperAttributes();
    
    // ------- Input Control -------
    public abstract boolean InputReadOnlyAsData(); // default is false
    public abstract int     InputMaxCharSize(); // maximum number or characters in size (e.g. 40)
    public abstract String InputWrapperTag();
    public abstract String InputWrapperBody();  // e.g. "<div class=\"clearBoth\"><!-- ? --></div>" 
    public abstract String InputLabelTag();
    public abstract String InputRequiredTag();
    public abstract String InputControlTag();

    public abstract String InputWrapperClass();
    public abstract String InputLabelClass();
    public abstract String InputRequiredClass();
    public abstract String InputControlClass();
    public abstract String InputReadOnlyClass();
    public abstract String InputReadOnlyDataWrapperTag();

    // ------- Anchor -------
    public abstract String AnchorDisabledTag();
    public abstract String AnchorDisabledClass();

    // ------- Button -------
    public abstract String ButtonTag();
    public abstract String ButtonClass();
    public abstract String ButtonSameActionDefaultOnClickScript();
    public abstract String ButtonOtherActionDefaultOnClickScript();

    // ------- Submit Button -------
    public abstract String SubmitClass();
    public abstract String SubmitControlTag();  // only valid if embed=true and InputWrapperTag != null
    public abstract String SubmitControlClass();
    public abstract String SubmitControlAttributes();
    
    // ------- Menu Tags -------
    public abstract String MenuTag();
    public abstract String MenuItemTag();
    public abstract String MenuItemLinkClass();
    public abstract String MenuItemLinkDefaultOnClickScript();
    public abstract String MenuItemCurrentClass(); 
    public abstract String MenuItemDisabledClass();
    public abstract String MenuItemExpandedClass();
    
    // ------- TableHead -------
    public abstract String TableHeadRowTag();   // "tr"
    public abstract String TableHeadColumnTag(); // "th"
    public abstract String TableHeadColumnLinkEnabledClass();
    public abstract String TableHeadColumnLinkDisabledClass();
    public abstract String TableHeadColumnLinkCurrentClass();
    public abstract String TableHeadColumnLinkCurrentAscendingClass();
    public abstract String TableHeadColumnLinkCurrentDescendingClass();
    public abstract String TableHeadSortAscendingIndicator();  // "[a]"
    public abstract String TableHeadSortDescendingIndicator(); // "[d]"
    public abstract String TableHeadSelectColumnIndicator(); // [X]
    public abstract String TableHeadSelectAscendingIndicator(); // [a]
    public abstract String TableHeadSelectDescendingIndicator(); // [d]
    public abstract String TableHeadLinkDefaultOnClickScript();
    
    // ------- Pager -------
    public abstract String PagerTag();
    public abstract String PagerClass();
    public abstract String PagerPageTag();
    public abstract String PagerPageClass();
    public abstract String PagerLinkClass();
    public abstract String PagerLabelTag();
    public abstract String PagerLabelClass();
    public abstract String PagerLabelText();
    public abstract String PagerPaddingText();
    public abstract String PagerLinkDefaultOnClickScript();
    
    public abstract String PagerFirstPageText();
    public abstract String PagerRewindText();
    public abstract String PagerForwardText();
    public abstract String PagerLastPageText();
    public abstract String PagerFirstPageTextDisabled();
    public abstract String PagerRewindTextDisabled();
    public abstract String PagerForwardTextDisabled();
    public abstract String PagerLastPageTextDisabled();

    // ------- PageInfo -------

    public abstract String PageInfoTag();
    public abstract String PageInfoClass();
    public abstract String PageInfoItemTag();
    public abstract String PageInfoLabel();
    public abstract String PageInfoLabelTo();
    public abstract String PageInfoLabelOf();
    public abstract String PageInfoLabelPadding();

    // ------- Errors -------

    public abstract String ErrorListTag();
    public abstract String ErrorListClass();
    public abstract String ErrorEntryTag();
    public abstract String ErrorItemEntryClass();
    public abstract String ErrorActionEntryClass();
    public abstract String ErrorEntryWrapperTag();

    // ------- Message -------

    public abstract String MessageTag();
    public abstract String MessageClass();
    
}
