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
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.struts2.action.ListSortingInfo;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class TableHeadRowTag extends EmpireTagSupport
{
    public static final String HEADROWINFO_ATTRIBUTE  = "tableHeadRowInfo";

    public static class HeadRowInfo
    {
        public String currentColumnName;
        public boolean sortOrderChangeable;
        public boolean sortDescending;
        public String sortAction;
        public String sortColumnParam;
        public String sortOrderParam;
        public String currentClass;
        // Overridable column defaults
        public String columnClass;
        public String columnStyle;
        public String columnAlign;
        public String columnWrap;
    }
    
    // Item Information 
    protected ListSortingInfo sortingInfo;
    protected ColumnExpr currentColumn;
    protected Object sortOrder;         // null=none, 0=ascending, 1=descending
    protected String sortAction;
    protected String sortColumnParam;
    protected String sortOrderParam;
    protected String currentClass;
    // Overrideable column defaults
    protected String columnClass;
    protected String columnStyle;
    protected String columnAlign;
    protected String columnWrap;

    // Temporary Information 
    private Object oldHeadRowInfo;
    
    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // Item Information
        sortingInfo = null;
        currentColumn = null;
        sortOrder = null;
        sortAction = null;
        currentClass = null;
        columnClass = null;
        columnStyle = null;
        columnAlign = null;
        columnWrap = null;
        sortColumnParam = null;
        sortOrderParam = null;
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
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag thr = w.startTag(dic.TableHeadRowTag());
        addStandardAttributes(thr, null);
        thr.beginBody(true);
        // Set current Column
        HeadRowInfo hri = new HeadRowInfo();
        // Is sorting Info supplied 
        if (sortingInfo!=null)
        {   // Set from sorting Info
            hri.currentColumnName = sortingInfo.getSortColumn();      
            hri.sortOrderChangeable = true;
            hri.sortDescending = sortingInfo.isSortDescending();
            hri.sortColumnParam = getSortParamName(sortingInfo, "sortColumn");
            hri.sortOrderParam  = getSortParamName(sortingInfo, "sortDesc");
        }
        else
        {   // Set Manually
            hri.currentColumnName = ((currentColumn!=null) ? currentColumn.getName() : null);  
            hri.sortOrderChangeable = (sortOrder!=null);
            hri.sortDescending  = getBoolean(sortOrder, false);
            hri.sortColumnParam = sortColumnParam;
            hri.sortOrderParam = sortOrderParam;
        }
        hri.sortAction = sortAction;
        hri.columnClass = columnClass;
        hri.currentClass = currentClass;
        hri.columnAlign = columnAlign;
        hri.columnStyle = columnStyle;
        hri.columnWrap = columnWrap;
        oldHeadRowInfo = putPageAttribute(HEADROWINFO_ATTRIBUTE, hri);
        // return usesBody ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
        return EVAL_BODY_INCLUDE;
    }
    
    @Override
    public int doEndTag() throws JspException
    {
        // Set current Column
        removePageAttribute(HEADROWINFO_ATTRIBUTE, oldHeadRowInfo);
        oldHeadRowInfo = null;
        // Write End Tag
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag thr = w.continueTag (dic.TableHeadRowTag(), true);
        thr.endTag();
        // done
        resetParams();
        return EVAL_PAGE;
    }
    
    // ------- helpers -------
    
    private String getSortParamName(ListSortingInfo sortInfo, String sortProperty)
    {
        String listProperty = sortInfo.getListPropertyName();
        if (StringUtils.isEmpty(listProperty))
            return sortProperty;
        return listProperty + "." + sortProperty; 
    }

    // -------------------------------- Property accessors -----------------------------

    public void setCurrentClass(String currentClass)
    {
        this.currentClass = currentClass;
    }

    public void setCurrentColumn(ColumnExpr currentColumn)
    {
        this.currentColumn = currentColumn;
    }

    public void setSortAction(String sortAction)
    {
        this.sortAction = sortAction;
    }

    public void setSortOrder(Object sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public void setSortColumnParam(String sortColumnParam)
    {
        this.sortColumnParam = sortColumnParam;
    }

    public void setSortOrderParam(String sortOrderParam)
    {
        this.sortOrderParam = sortOrderParam;
    }

    public void setColumnClass(String columnClass)
    {
        this.columnClass = columnClass;
    }

    public void setColumnStyle(String columnStyle)
    {
        this.columnStyle = columnStyle;
    }

    public void setColumnAlign(String columnAlign)
    {
        this.columnAlign = columnAlign;
    }

    public void setColumnWrap(String columnWrap)
    {
        this.columnWrap = columnWrap;
    }

    public void setSortingInfo(ListSortingInfo sortingInfo)
    {
        this.sortingInfo = sortingInfo;
    }

}
