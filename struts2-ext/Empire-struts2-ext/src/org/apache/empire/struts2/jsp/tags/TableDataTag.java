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

import javax.servlet.jsp.JspException;

import org.apache.empire.data.ColumnExpr;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;



@SuppressWarnings("serial")
public class TableDataTag extends DataValueTag
{
    // Default Row Info
    private static final TableRowTag.RowInfo defaultRowInfo = new TableRowTag.RowInfo();
    
    // Column Properties
    protected String width;
    protected String height;
    protected String colspan;
    protected String rowspan;
    protected String align;
    protected String valign;
    protected String wrap;
    protected String bgcolor;
    
    /*
     * InputControlTag Constructor
     */
    public TableDataTag()
    {
        // Default constructor
    }

    @Override
    protected void resetParams()
    {
        width = null;
        height = null;
        colspan = null;
        rowspan = null;
        align = null;
        valign = null;
        wrap = null;
        bgcolor = null;
        // Reset Params
        super.resetParams();
    }
    
    @Override
    public int doStartTag()
        throws JspException
    {
        // Get Row Info 
        TableRowTag.RowInfo ri = getRowInfo();
        // Start Tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag td = w.startTag("td");
        td.addAttribute("id", getId());
        td.addAttribute("class", getCssClass(ri));
        setStyleAndWrap(td, ri);
        td.addAttribute("width", width);
        td.addAttribute("height", height);
        td.addAttribute("colspan", colspan);
        td.addAttribute("rowspan", rowspan);
        td.addAttribute("align", str(align, ri.columnAlign));
        td.addAttribute("valign", valign);
        td.addAttribute("bgcolor", bgcolor);
        td.beginBody();
        // Start Value
        this.id=null;
        this.cssClass=null;
        this.cssStyle=null;
        // Render Value
        return super.doStartTag();
    }
    
    @Override
    public int doEndTag()
        throws JspException
    {
        int result = super.doEndTag();
        // Write End Tag
        HtmlWriter w = new HtmlWriter(pageContext.getOut());
        HtmlTag td = w.continueTag("td", true);
        td.endTag();
        // done
        return result;
    }
    
    private TableRowTag.RowInfo getRowInfo()
    {
        Object ri = getPageAttribute(TableRowTag.ROWINFO_ATTRIBUTE, null);
        if (ri instanceof TableRowTag.RowInfo)
            return ((TableRowTag.RowInfo)ri);
        // No current Column Info available
        log.debug("No table row information available! <e:tr> Tag missing?" + getPageName());
        return defaultRowInfo; 
    }

    private String getCssClass(TableRowTag.RowInfo ri)
    {
        if (this.cssClass!=null)
            return this.cssClass;
        // Check if current
        ColumnExpr column = getColumnExpr();
        if (ri.currentClass!=null && ri.currentColumn!=null && ri.currentColumn.equals(column))
            return ri.currentClass; // Current column
        // Return the columnClass
        return ri.columnClass; 
    }
    
    private void setStyleAndWrap(HtmlTag tag, TableRowTag.RowInfo ri)
    {
        String style = str(cssStyle, ri.columnStyle);
        String wordwrap = str(wrap, ri.columnWrap);
        // No wrap
        if ("nowrap".equalsIgnoreCase(wordwrap))
        {
            tag.addAttribute("nowrap");
        }
        // Wrap is given
        else if (wordwrap!=null && wordwrap.length()>0)
        {   // Append to style
            wordwrap = "word-wrap: " + wordwrap + ";"; 
            style = ((style!=null) ? wordwrap + style  : wordwrap) ;
        }
        // Set Style
        tag.addAttribute("style", style);
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
    
}
