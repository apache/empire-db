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
package org.apache.empire.struts2.jsp.components;

import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.Attributes;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.struts2.html.HtmlWriter;
import org.apache.empire.struts2.html.HtmlWriter.HtmlTag;
import org.apache.empire.struts2.jsp.components.info.CalendarInfo;
import org.apache.empire.struts2.jsp.components.info.CalendarInfo.CalendarDayInfo;
import org.apache.struts2.components.UIBean;

import com.opensymphony.xwork2.util.ValueStack;


public class CalendarComponent extends UIBean
{
    // Logger
    protected static Log log                 = LogFactory.getLog(ControlComponent.class);

    private CalendarInfo calendarInfo        = null;
    private String       selectDateAction    = null;
    private String       selectWeekdayAction = null;
    private String       selectWeekAction    = null;
    private String       selectMonthAction   = null;
    private String       cellpadding         = null;
    private String       cellspacing         = null;
    private String       monthClass;
    private String       dayOfWeekClass;
    private String       weekOfYearClass;
    private String       dateEmptyClass;
    private String       dateValidClass;
    private String       dateLinkClass;
    private String       dateTodayClass;
    private String       dateSelectedClass;
    private String       paramName           = "item";

    public CalendarComponent(ValueStack stack, HttpServletRequest request, HttpServletResponse response)
    {
        super(stack, request, response);
    }

    @Override
    protected String getDefaultTemplate()
    {
        return null;
    }

    @Override
    public boolean start(Writer writer)
    {
        return super.start(writer);
    }

    @Override
    public final boolean end(Writer writer, String body)
    {
        // evaluateParams();
        try
        { // No Value

            // Render value
            HtmlWriter htmlWriter = new HtmlWriter(writer);
            HtmlTag table = htmlWriter.startTag("table");
            table.addAttribute("id", this.id);
            table.addAttribute("class", this.cssClass);
            table.addAttribute("style", this.cssStyle);
            table.addAttribute("cellpadding", this.cellpadding);
            table.addAttribute("cellspacing", this.cellspacing);
            table.beginBody();

            renderHeader(htmlWriter);
            renderBody(htmlWriter);

            table.endTag();
            return false; // do not evaluate body again!

        } catch (Exception e)
        {
            log.error("error when rendering", e);
            return false; // do not evaluate body again!
        } finally
        {
            popComponentStack();
        }
    }

    private void renderHeader(HtmlWriter writer)
    {
        HtmlTag monthHeader = writer.startTag("tr");
        monthHeader.beginBody();

        HtmlTag thMonth = writer.startTag("th");
        thMonth.addAttribute("class", this.monthClass);
        thMonth.addAttribute("colspan", 8);
        thMonth.beginBody();
        String item = calendarInfo.getLinkText();
        String text = calendarInfo.getMonthText() + " " + calendarInfo.getYearText();
        if (selectMonthAction != null)
        {
            renderLink(writer, text, selectMonthAction, paramName, item);
            text = null;
        }
        thMonth.endTag(text);
        monthHeader.endTag();

        HtmlTag weekHeader = writer.startTag("tr");
        weekHeader.beginBody();

        HtmlTag kwHead = writer.startTag("th");
        kwHead.addAttribute("class", this.dayOfWeekClass);
        kwHead.beginBody();
        kwHead.endTag("KW");

        for (int i = 0; i < 7; i++)
        {
            HtmlTag thWeekDays = writer.startTag("th");
            thWeekDays.addAttribute("class", this.dayOfWeekClass);
            thWeekDays.beginBody();
            text = calendarInfo.getDayOfWeekText(i);
            if (selectWeekdayAction != null)
            {
                renderLink(writer, text, selectWeekdayAction, paramName, item);
                text = null;
            }
            thWeekDays.endTag(text);
        }
        weekHeader.endTag();
    }

    private void renderBody(HtmlWriter writer)
    {
        // for each week in a month
        for (int i = 0; i < calendarInfo.getWeekCount(); i++)
        {
            HtmlTag weekRow = writer.startTag("tr");
            weekRow.beginBody();

            // for each day in a week
            renderKalendarWeek(calendarInfo.getWeek(i), writer);
            for (CalendarInfo.CalendarDayInfo day : calendarInfo.getWeek(i))
            {
                renderDay(day, writer);
            }
            weekRow.endTag();
        }
    }

    private void renderKalendarWeek(CalendarDayInfo[] week, HtmlWriter writer)
    {
        int kw = calendarInfo.getKalendarWeek(week);
        String bodyText = (kw!=0 ? kw + "" : "");
        String linkItem = "";

        for(int i=0;i<week.length;i++)
        {
            if(week[i]!=null)
            {
                linkItem = week[i].getLinkText();
                break;
            }
        }
        
        // Render Tag
        HtmlTag weekCell = writer.startTag("td");
        weekCell.addAttribute("class", this.weekOfYearClass);
        weekCell.beginBody();

        if(StringUtils.isValid(bodyText) && StringUtils.isValid(selectWeekAction))
            renderLink(writer, bodyText, selectWeekAction, paramName, linkItem);
        else
            weekCell.endTag("");
    }

    private void renderDay(CalendarDayInfo day, HtmlWriter writer)
    {
        String bodyText = null;
        String linkItem = null;
        String cssClass = null;
        if (day != null)
        {
            bodyText = day.getText();
            // Get Cell class
            if (day.isToday())
                cssClass = this.dateTodayClass;
            else if (day.isSelected())
                cssClass = this.dateSelectedClass;
            else
                cssClass = this.dateValidClass;
            // Add a link
            if (day.isLink() && selectDateAction != null)
            {
                linkItem = day.getLinkText();
                // if (cssClass!= null)
                // cssClass = cssClass + " " + this.dateLinkClass;
                // else
                if (!day.isSelected() && !day.isToday())
                    cssClass = this.dateLinkClass;
            }

        } else
        {
            bodyText = "&nbsp;";
            cssClass = dateEmptyClass;
        }

        // Render Tag
        HtmlTag dateCell = writer.startTag("td");
        dateCell.addAttribute("class", cssClass);
        // dayCell.addAttribute("style", "text-align:center; width:25px; height:25px; border: 1px solid white;");
        dateCell.beginBody();
        if (linkItem != null)
        {
            renderLink(writer, bodyText, selectDateAction, paramName, linkItem);
            bodyText = null;
        }
        dateCell.endTag(bodyText);
    }

    private void renderLink(HtmlWriter writer, String text, String action, String param, String value)
    {
        Map<String, Object> paramMap = null;
        if (param != null)
        {
            paramMap = new Attributes();
            paramMap.put(param, value);
        }
        // Render Link now
        HtmlTag link = writer.startTag("a");
        link.addAttribute("href", getUrl(action, paramMap));
        link.beginBody(text);
        link.endTag();
    }

    public String getUrl(String actionName, Map params)
    {
        String namespace = null;
        String method = null;
        String scheme = null;
        boolean includeContext = true;
        boolean encodeResult = true;
        boolean forceAddSchemeHostAndPort = false;
        boolean escapeAmp = true;        
        return this.determineActionURL(actionName, namespace, method, request, response, params, scheme, 
                                       includeContext, encodeResult, forceAddSchemeHostAndPort, escapeAmp);
    }

    // ------- Property setters -------

    public void setCalendarInfo(CalendarInfo calendarInfo)
    {
        this.calendarInfo = calendarInfo;
    }

    public void setSelectDateAction(String selectDateAction)
    {
        this.selectDateAction = selectDateAction;
    }

    public void setSelectWeekdayAction(String selectWeekdayAction)
    {
        this.selectWeekdayAction = selectWeekdayAction;
    }
    
    public void setSelectWeekAction(String selectWeekAction)
    {
        this.selectWeekAction = selectWeekAction;
    }

    public void setSelectMonthAction(String selectMonthAction)
    {
        this.selectMonthAction = selectMonthAction;
    }

    public void setCellpadding(String cellpadding)
    {
        this.cellpadding = cellpadding;
    }

    public void setCellspacing(String cellspacing)
    {
        this.cellspacing = cellspacing;
    }

    public void setDateEmptyClass(String dateEmptyClass)
    {
        this.dateEmptyClass = dateEmptyClass;
    }

    public void setDateLinkClass(String dateLinkClass)
    {
        this.dateLinkClass = dateLinkClass;
    }

    public void setDateSelectedClass(String dateSelectedClass)
    {
        this.dateSelectedClass = dateSelectedClass;
    }

    public void setDateTodayClass(String dateTodayClass)
    {
        this.dateTodayClass = dateTodayClass;
    }

    public void setDateValidClass(String dateValidClass)
    {
        this.dateValidClass = dateValidClass;
    }

    public void setDayOfWeekClass(String dayOfWeekClass)
    {
        this.dayOfWeekClass = dayOfWeekClass;
    }
    
    public void setWeekOfYearClass(String weekOfYearClass)
    {
        this.weekOfYearClass = weekOfYearClass;
    }

    public void setMonthClass(String monthClass)
    {
        this.monthClass = monthClass;
    }

    public void setParamName(String paramName)
    {
        if (StringUtils.isValid(paramName))
            this.paramName = paramName;
    }

}
