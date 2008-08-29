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
package org.apache.empire.struts2.jsp.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.struts2.action.ListPagingInfo;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.empire.struts2.jsp.components.AnchorComponent;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class ListPageBarTag extends EmpireTagSupport // AbstractRemoteCallUITag
{
    protected ListPagingInfo pagingInfo; 
    protected Object size;
    protected String action;
    protected String setPageParam;
    
    protected String label;
    protected String linkClass;
    protected String textClass;
    protected String padding;
    protected String onclick;
    
    public ListPageBarTag()
    {
        autoResetParams = false;
    }
    
    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // LinkTag
        pagingInfo    = null; 
        action  = null;
        label   = null;
        linkClass = null;
        textClass = null;
        padding = null; 
        setPageParam = null;
        onclick = null;
        // reset base params
        super.resetParams();
    }

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return new AnchorComponent(stack, req, res);
    }

    @Override
    protected void populateParams()
    {
        super.populateParams();
        
        AnchorComponent anchor = (AnchorComponent)component;
        // get Href
        anchor.setAction(action);
    }

    protected void populatePageParams(int pageIndex, boolean isCurrent, String text)
    {
        AnchorComponent anchor = (AnchorComponent)component;
        // get Href
        anchor.setAction(action);
        anchor.setText(text);
        anchor.setDisabled(isCurrent);
        anchor.addParameter(setPageParam, String.valueOf( pageIndex ));
        anchor.setOnclick(onclick);
    }
    
    @Override
    public int doStartTag() throws JspException
    {
        action = checkAction(action);
        // Don't call base class
        return EVAL_BODY_BUFFERED; // SKIP_BODY; // EVAL_BODY_INCLUDE;
    }
    
    @Override
    public int doEndTag()
        throws JspException
    {
        int current = Math.max(pagingInfo.getPage(), 0);
        int pageCnt = Math.max(pagingInfo.getPageCount(), 1);
        if (pageCnt<=1)
            return EVAL_PAGE;
        
        int ctlSize  = Math.max(getInt(size,  5), 1);
        int begIndex =(current / ctlSize) * ctlSize; 
        int endIndex = begIndex + ctlSize;
        setPageParam = getSetPageParamName();
        
        // Get Body
        String body = getBody();
        setBodyContent(null);
        
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        // Padding Text
        if (padding==null)
            padding = dic.PagerPaddingText();
        String pbeg_e = dic.PagerFirstPageText();
        String prwd_e = dic.PagerRewindText();
        String pfwd_e = dic.PagerForwardText();
        String pend_e = dic.PagerLastPageText();
        String pbeg_d = dic.PagerFirstPageTextDisabled();
        String prwd_d = dic.PagerRewindTextDisabled();
        String pfwd_d = dic.PagerForwardTextDisabled();
        String pend_d = dic.PagerLastPageTextDisabled();
        // Class and Styles
        HtmlTag div = w.startTag(dic.PagerTag());
        addStandardAttributes(div, dic.PagerClass());
        // Body
        div.beginBody(body, true);
        this.cssClass = str(linkClass, dic.PagerLinkClass());
        this.cssStyle = null;
        // onclick
        if (onclick== null)
            onclick = dic.PagerLinkDefaultOnClickScript();
        // Label?
        String pagerLabel = getString(str(label, dic.PagerLabelText()));
        if (pagerLabel!=null)
        {   // There is text
            HtmlTag label = w.startTag( dic.PagerLabelTag());
            label.addAttribute("class", dic.PagerLabelClass());
            label.endTag(pagerLabel);
        }
        // Back and fast back
        renderButton(w, pbeg_e, pbeg_d, true, pageCnt, current, 0);
        renderButton(w, prwd_e, prwd_d, true, pageCnt, current, Math.max(current - 1, 0) );
        // The Pages
        for (int pageIndex=begIndex; pageIndex<endIndex; pageIndex++)
        {
            // Padding
            if (pageIndex>0 && padding!=null)
                w.println(padding);
            // The Page selection
            HtmlTag page = w.startTag(dic.PagerPageTag());
            div.addAttribute("class", dic.PagerPageClass());
            page.beginBody();
            // The Anchors
            String pageText = String.valueOf(pageIndex+1);
            if (pageIndex>=pageCnt)
            {   // Hide page number
                pageText = "&nbsp;";
            }    
            renderButton(w, pageText, pageText, false, pageCnt, current, pageIndex);
            // end
            page.endTag(true);
        }
        // Forward and Fast Forward
        renderButton(w, pfwd_e, pfwd_d, true, pageCnt, current, Math.min(current + 1, pageCnt));
        renderButton(w, pend_e, pend_d, true, pageCnt, current, pageCnt-1);
        // end
        div.endTag();
        // done
        resetParams();
        return EVAL_PAGE;
    }
    
    private void renderButton(HtmlWriter w, String text_enabled, String text_disabled, boolean image, int count, int current, int page)
        throws JspException
    {
        boolean disabled = (page==current);
        if (page<0 || page>=count)
        {
            disabled = true;
        }    
        // The Anchors
        super.doStartTag();
        populatePageParams(page, disabled, (disabled ? text_disabled : text_enabled));
        super.doEndTag();
        // New Line
        w.println();
    }
    
    // ------- helpers --------
    
    private String getSetPageParamName()
    {
        if (pagingInfo==null)
            return "page";
        // Get Property
        String listProperty = pagingInfo.getListPropertyName();
        if (StringUtils.isEmpty(listProperty))
            return "page";
        // Property
        return listProperty + ".page";
    }

    // ------- Property Accessors -------

    public void setPagingInfo(ListPagingInfo pagingInfo)
    {
        this.pagingInfo = pagingInfo;
    }
    
    public void setAction(String action)
    {
        this.action = action;
    }

    public void setLinkClass(String linkClass)
    {
        this.linkClass = linkClass;
    }

    public void setSize(Object size)
    {
        this.size = size;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public void setOnclick(String onclick)
    {
        this.onclick = onclick;
    }

}
