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

import java.util.HashMap;

import javax.servlet.jsp.JspException;

import org.apache.empire.data.ColumnExpr;
import org.apache.empire.struts2.html.HtmlTagDictionary;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.empire.struts2.jsp.components.AnchorComponent;


@SuppressWarnings("serial")
public class TableHeadColumnTag extends LinkTag
{
    // TableHeadColumnTag
    protected ColumnExpr column;

    // Select 
    protected ColumnExpr[] select;
    protected String selectName;
    protected String selectAction;
    
    // th
    protected String width;
    protected String height;
    protected String colspan;
    protected String rowspan;
    protected String align;
    protected String valign;
    protected String wrap;
    protected String bgcolor;
    protected String columnname;
    
    // Temporary String
    private String orderIndicator = null;
    
    @Override
    protected void resetParams()
    {
        column = null;
        select = null;
        width = null;
        height = null;
        colspan = null;
        rowspan = null;
        align = null;
        valign = null;
        wrap = null;
        bgcolor = null;
        columnname = null;
        // Reset Params
        super.resetParams();
    }

    @Override
    public String getBody()
    {
        String body = super.getBody();
        if (orderIndicator==null || orderIndicator.length()==0)
            return body;
        if (body==null || body.length()==0)
            return orderIndicator;  
        // beides zusammen
        return orderIndicator + " " + body; 
    }

    @Override
    public int doStartTag() throws JspException
    {
        TableHeadRowTag.HeadRowInfo hri = getHeadRowInfo();
        // HtmlWriter
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        orderIndicator = getSortOrderIdicator(hri, dic);
        // Start Tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag wrap = w.startTag(dic.TableHeadColumnTag());
        wrap.addAttribute("id", getId());
        wrap.addAttribute("class", getCssClass(hri));
        setStyleAndWrap(wrap, hri);
        wrap.addAttribute("width", width);
        wrap.addAttribute("height", height);
        wrap.addAttribute("colspan", colspan);
        wrap.addAttribute("rowspan", rowspan);
        wrap.addAttribute("align", str(align, hri.columnAlign));
        wrap.addAttribute("valign", valign);
        wrap.addAttribute("bgcolor", bgcolor);
        // Body
        wrap.beginBody();
        if (prepareLinkParams(hri))
        {   // The Anchor
            this.cssClass = getLinkClass(hri, dic);
            this.cssStyle = null;
            // OnClick
            if (onclick== null)
                onclick = dic.TableHeadLinkDefaultOnClickScript();
            // render column choices 
            if (select!=null)
            {
               renderColumnSelect(w);
               text = "";
            }
            // Start the tag
            super.doStartTag();
            // Add Sort Param
            AnchorComponent anchor = (AnchorComponent)component;
            anchor.addParameter(hri.sortColumnParam, getColumnName());
            if (hri.sortOrderChangeable && hri.sortOrderParam!=null)
            {
                anchor.addParameter(hri.sortOrderParam, (hri.sortDescending) ? "0" : "1");
            }
            // render Link
            super.doEndTag();
        }
        else
        {   // The value
            HtmlTag text = w.startTag("span"); // dic.AnchorDisabledTag()
            text.addAttribute("class", getLinkClass(hri, dic));
            text.beginBody(getColumnTitle());
            text.endTag(getBody());
        }
        // Don't call base class
        return EVAL_BODY_INCLUDE;
    }
    
    @Override
    public int doEndTag() throws JspException
    {
        // done
        HtmlTagDictionary dic = HtmlTagDictionary.getInstance();
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag wrap = w.continueTag(dic.TableHeadColumnTag(), true);
        wrap.endTag();
        // done
        resetParams();
        return EVAL_PAGE;
    }

    private void renderColumnSelect(HtmlWriter w)
    {
        HtmlTag selectTag = w.startTag("select");
        selectTag.addAttribute("name", selectName);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(selectName, "");
        selectTag.addAttribute("onchange", "window.location='" + getActionURL(selectAction, params) + "'+options[selectedIndex].value" );
        selectTag.beginBody();
        
        for (int i=0; i<select.length; i++)
        {
            HtmlTag option = w.startTag("option");
            option.addAttribute("value", select[i].getName());
            option.addAttribute("selected", (select[i].equals(column)));
            option.endTag(getString(select[i].getTitle()));
        }
        selectTag.endTag();
    }
    
    private TableHeadRowTag.HeadRowInfo getHeadRowInfo()
    {
        Object hri = getPageAttribute(TableHeadRowTag.HEADROWINFO_ATTRIBUTE, null);
        if (hri instanceof TableHeadRowTag.HeadRowInfo)
            return ((TableHeadRowTag.HeadRowInfo)hri);
        // No current Column Info available
        log.error("No table head row information available! <e:thr> Tag missing?" + getPageName());
        return new TableHeadRowTag.HeadRowInfo(); 
    }
    
    private void setStyleAndWrap(HtmlTag tag, TableHeadRowTag.HeadRowInfo hri)
    {
        String style = str(cssStyle, hri.columnStyle);
        String wordwrap = str(wrap, hri.columnWrap);
        // No wrap
        if ("nowrap".equalsIgnoreCase(wordwrap))
        {
            tag.addAttribute("nowrap");
        }
        // Wrap is given
        else if (wordwrap!=null && wordwrap.length()>0)
        {   // Append to style
            wordwrap = "word-wrap: " + wordwrap + ";"; 
            style = ((style!=null) ? style + " " + wordwrap : wordwrap) ;
        }
        // Set Style
        tag.addAttribute("style", style);
    }
    
    private boolean prepareLinkParams(TableHeadRowTag.HeadRowInfo hri)
    {
        if (action==null)
            action = hri.sortAction; 
        if (text== null)
            text = getColumnTitle();
        // yes, it is a link!
        return (action!=null && column!=null); 
    }
    
    private String getColumnName()
    {
        return str(columnname, column.getName());
    }
    
    private String getColumnTitle()
    {
        if (text!=null)
            return getString(text, "");
        if (column!=null)
        {
            return getTranslation( column.getTitle() ); 
        }
        // Error
        log.error("No Title given for Table Head Column on page " + getPageName());
        return "";
    }
    
    private boolean isCurrentColumn(TableHeadRowTag.HeadRowInfo hri)
    {
        if (column==null || hri.currentColumnName==null)
            return false;
        // Compare Name
        return (hri.currentColumnName.equalsIgnoreCase(column.getName()));        
    }
    
    private String getSortOrderIdicator(TableHeadRowTag.HeadRowInfo hri, HtmlTagDictionary dic)
    {
        // Is Sort order changeable
        if (hri.sortOrderChangeable==false)
            return null; // Not Changeable
        // Check if current
        if (isCurrentColumn(hri))
        {   // Yes, it is changeable
            boolean desc = hri.sortDescending; 
            if (select!=null)
            {
                return (desc) ? dic.TableHeadSelectAscendingIndicator() 
                              : dic.TableHeadSelectDescendingIndicator(); 
            }
            else
            {
                return (desc) ? dic.TableHeadSortDescendingIndicator() 
                              : dic.TableHeadSortAscendingIndicator(); 
            }
        }
        // Selection List present?
        if (select!=null)
        {
            return dic.TableHeadSelectColumnIndicator();
        }
        // Not the current column
        return null; 
    }

    private String getCssClass(TableHeadRowTag.HeadRowInfo hri)
    {
        if (this.cssClass!=null)
            return this.cssClass;
        // Check if current
        if (hri.currentClass!=null && isCurrentColumn(hri))
            return hri.currentClass; // Current column
        // Return the columnClass
        return hri.columnClass; 
    }

    private String getLinkClass(TableHeadRowTag.HeadRowInfo hri, HtmlTagDictionary dic)
    {
        // Check if current
        if (isCurrentColumn(hri))
        {   // Get Class for current Item
            if (hri.sortOrderChangeable==false)
                return dic.TableHeadColumnLinkCurrentClass();
            if (hri.sortDescending)
                return dic.TableHeadColumnLinkCurrentDescendingClass();
            else
                return dic.TableHeadColumnLinkCurrentAscendingClass();
        }
        // Check disabled
        if (action==null || item==null || getBoolean(disabled, false))
            return dic.TableHeadColumnLinkDisabledClass();
        // Return the columnClass
        return dic.TableHeadColumnLinkEnabledClass(); 
    }

    // ------- Property accessors -------
    
    public void setAlign(String align)
    {
        this.align = align;
    }

    public void setBgcolor(String bgcolor)
    {
        this.bgcolor = bgcolor;
    }

    public void setColspan(String colspan)
    {
        this.colspan = colspan;
    }

    public void setColumn(ColumnExpr column)
    {
        this.column = column;
    }

    public void setColumnname(String columnname)
    {
        this.columnname = columnname;
    }

    public void setHeight(String height)
    {
        this.height = height;
    }

    public void setWrap(String wrap)
    {
        this.wrap = wrap;
    }

    public void setRowspan(String rowspan)
    {
        this.rowspan = rowspan;
    }

    public void setValign(String valign)
    {
        this.valign = valign;
    }

    public void setWidth(String width)
    {
        this.width = width;
    }

    public void setSelect(ColumnExpr[] select)
    {
        this.select = select;
    }

    public void setSelectAction(String selectAction)
    {
        this.selectAction = selectAction;
    }

    public void setSelectName(String selectName)
    {
        this.selectName = selectName;
    }
    
}
