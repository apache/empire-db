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

import org.apache.empire.data.ColumnExpr;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;
import org.apache.struts2.views.jsp.IteratorStatus;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class TableRowTag extends EmpireTagSupport 
{
    public static final String ITERATOR_STATUS_ATTRIBUTE = "listRowStatus";
    public static final String ROWINFO_ATTRIBUTE  = "tableRowInfo";

    public static class RowInfo
    {
        public ColumnExpr currentColumn;
        public String currentClass;
        // Overridable column defaults
        public String columnClass;
        public String columnAlign;
        public String columnStyle;
        public String columnWrap;
    }
    
    // Common UI
    protected String cssOddClass;
    protected String cssOddStyle;
    // Overridable column defaults
    protected ColumnExpr currentColumn;
    protected String currentClass;
    protected String columnClass;
    protected String columnStyle;
    protected String columnAlign;
    protected String columnWrap;

    // Temporäry Information 
    private Object oldRowInfo;
    
    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // Common UI 
        cssOddClass = null;
        cssOddStyle = null;
        // Column info
        currentColumn = null;
        currentClass = null;
        columnClass = null;
        columnStyle = null;
        columnAlign = null;
        columnWrap = null;
        // reset
        super.resetParams();
    }
    
    @Override
    public Component getBean(ValueStack arg0, HttpServletRequest arg1, HttpServletResponse arg2)
    {
        return null;
    }
    
    @Override
    protected void populateParams()
    {
        log.error("Illegal Method call");
    }
    
    @Override
    public int doStartTag() throws JspException
    {
        // Tabel cell tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag trTag = w.startTag("tr");
        trTag.addAttribute("id", getId());
        trTag.addAttribute("class", (isOddStatus() && cssOddClass!=null) ? cssOddClass : cssClass);
        trTag.addAttribute("style", (isOddStatus() && cssOddStyle!=null) ? cssOddStyle : cssStyle);
        trTag.beginBody();
        // Set Row Info
        RowInfo ri = new RowInfo();
        ri.currentColumn = currentColumn;  
        ri.columnClass = columnClass;
        ri.currentClass = currentClass;
        ri.columnAlign = columnAlign;
        ri.columnStyle = columnStyle;
        ri.columnWrap = columnWrap;
        oldRowInfo = putPageAttribute(ROWINFO_ATTRIBUTE, ri);
        // return usesBody ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
        return EVAL_BODY_INCLUDE;
        
    }
    
    @Override
    public int doEndTag() throws JspException
    {
        // Set current Column
        removePageAttribute(ROWINFO_ATTRIBUTE, oldRowInfo);
        oldRowInfo = null;
        // Write End Tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag trTag = w.continueTag("tr", true);
        trTag.endTag();
        // done
        resetParams();
        return EVAL_PAGE;
    }

    // ------- Helpers -------
    
    private boolean isOddStatus()
    {
        Object statusAttr = getPageAttribute(ITERATOR_STATUS_ATTRIBUTE, null);
        if (statusAttr==null)
            return false;
        IteratorStatus status = (IteratorStatus)getStack().getContext().get(statusAttr);
        return (status!=null && status.isOdd());
    }

    // ------- Property setters -------
    
    public void setCssOddClass(String cssOddClass)
    {
        this.cssOddClass = cssOddClass;
    }

    public void setCssOddStyle(String cssOddStyle)
    {
        this.cssOddStyle = cssOddStyle;
    }

    public void setCurrentColumn(ColumnExpr currentColumn)
    {
        this.currentColumn = currentColumn;
    }

    public void setCurrentClass(String currentClass)
    {
        this.currentClass = currentClass;
    }

    public void setColumnClass(String columnClass)
    {
        this.columnClass = columnClass;
    }

    public void setColumnAlign(String columnAlign)
    {
        this.columnAlign = columnAlign;
    }

    public void setColumnStyle(String columnStyle)
    {
        this.columnStyle = columnStyle;
    }

    public void setColumnWrap(String columnWrap)
    {
        this.columnWrap = columnWrap;
    }
     
}
