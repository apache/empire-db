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

import org.apache.empire.struts2.jsp.components.CalendarComponent;
import org.apache.empire.struts2.jsp.components.info.CalendarInfo;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;


@SuppressWarnings("serial")
public class CalendarTag extends EmpireTagSupport
{
    // Properties
    protected CalendarInfo calendarInfo;
    protected String selectDateAction;
    protected String selectWeekdayAction;
    protected String selectWeekAction;
    protected String selectMonthAction;
    protected String cellpadding;
    protected String cellspacing;
    protected String monthClass;
    protected String dayOfWeekClass;
    protected String weekOfYearClass;
    protected String dateEmptyClass;
    protected String dateValidClass;
    protected String dateLinkClass;
    protected String dateTodayClass;
    protected String dateSelectedClass;
    protected String param;
    
    /*
     * InputControlTag Constructor
     */
    public CalendarTag()
    {
        // Default constructor
    }

    /*
     * Clears all params since tag is reused
     */
    @Override
    protected void resetParams()
    {
        // FormTag
        calendarInfo = null;
        selectDateAction = null;
        selectWeekdayAction = null;
        selectWeekAction = null;
        selectMonthAction = null;
        cellpadding = null;
        cellspacing = null;
        monthClass = null;
        weekOfYearClass = null;
        dayOfWeekClass = null;
        dateEmptyClass = null;
        dateValidClass = null;
        dateLinkClass = null;
        dateTodayClass = null;
        dateSelectedClass = null;
        param = null;
        // reset
        super.resetParams();
    }
    
    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res)
    {
        return new CalendarComponent(stack, req, res);
    }
    
    @Override
    protected void populateParams()
    {
        selectDateAction    = checkAction(selectDateAction);
        selectWeekdayAction = checkAction(selectWeekdayAction);
        selectWeekAction    = checkAction(selectWeekAction);
        selectMonthAction   = checkAction(selectMonthAction);
        
        super.populateParams();

        // Form Component
        CalendarComponent comp = (CalendarComponent)component;
        comp.setCalendarInfo(calendarInfo);
        comp.setSelectDateAction(selectDateAction);
        comp.setSelectMonthAction(selectMonthAction);
        comp.setSelectWeekdayAction(selectWeekdayAction);
        comp.setSelectWeekAction(selectWeekAction);
        comp.setCellpadding(cellpadding);
        comp.setCellspacing(cellspacing);
        comp.setMonthClass(monthClass);
        comp.setDayOfWeekClass(dayOfWeekClass);
        comp.setWeekOfYearClass(weekOfYearClass);
        comp.setDateEmptyClass(dateEmptyClass);
        comp.setDateValidClass(dateValidClass);
        comp.setDateLinkClass(dateLinkClass);
        comp.setDateTodayClass(dateTodayClass);
        comp.setDateSelectedClass(dateSelectedClass);
        comp.setParamName(param);
    }
    
    // ------- Setters -------

    public void setCalendarInfo(CalendarInfo calendarInfo)
    {
        this.calendarInfo = calendarInfo;
    }

    public void setParam(String param)
    {
        this.param = param;
    }

    public void setSelectDateAction(String selectDateAction)
    {
        this.selectDateAction = selectDateAction;
    }

    public void setSelectMonthAction(String selectMonthAction)
    {
        this.selectMonthAction = selectMonthAction;
    }

    public void setSelectWeekdayAction(String selectWeekdayAction)
    {
        this.selectWeekdayAction = selectWeekdayAction;
    }
    public void setSelectWeekAction(String selectWeekAction)
    {
        this.selectWeekAction = selectWeekAction;
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

    public void setDateSelectedClass(String dateSelectedClass)
    {
        this.dateSelectedClass = dateSelectedClass;
    }

}
