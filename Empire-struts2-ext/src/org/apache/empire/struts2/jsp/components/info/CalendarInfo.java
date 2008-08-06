/*
 * ESTEAM Software GmbH, 13.08.2007
 */
package org.apache.empire.struts2.jsp.components.info;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.DateUtils;
import org.apache.empire.struts2.jsp.components.ControlComponent;


public class CalendarInfo
{
    // Logger
    protected static Log log = LogFactory.getLog(ControlComponent.class);

    protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    
    public static String formatDate(Date date)
    {
        return dateFormat.format(date);
    }
    
    public static Date parseDate(String date)
    {
        if (date==null || date.length()!=dateFormat.toPattern().length())
        {   // Error: Invalid Date supplied. Using Today
            log.error("Invalid date format: " + String.valueOf(date));
            return DateUtils.getDateNow();
        }
        try
        {   // See, if a date is supplied on the request
            return dateFormat.parse(date);
            
        } catch (ParseException e)
        {   // Unable to parse Date
            log.error("Invalid date format: " + date, e);
            return DateUtils.getDateNow();
        }
    }
    
    public static class CalendarDayInfo
    {
        private Date date;
        private boolean link;
        private boolean selected;
        private boolean today;
        private String text;
        
        public CalendarDayInfo(Date date, String text, boolean isToday)
        {
            this.date = date;
            this.text = text;
            this.link = false;
            this.today = isToday;
        }
        public Date getDate()
        {
            return date;
        }
        public boolean isToday()
        {
            return today;
        }
        public String getText()
        {
            return text;
        }
        public String getLinkText()
        {
            return CalendarInfo.formatDate(date);
        }
        public boolean isLink()
        {
            return link;
        }
        public void setLink(boolean link)
        {
            this.link = link;
        }
        public boolean isSelected()
        {
            return selected;
        }
        public void setSelected(boolean selected)
        {
            this.selected = selected;
        }
    }
    
    private Locale locale;
    private CalendarDayInfo[][] days = new CalendarDayInfo[6][7];
    private int year;
    private int month;
    private Date date = null;

    private String[] months;
    private String[] weekdays;
    
    public CalendarInfo(Locale locale)
    {
        this.locale = locale;

        SimpleDateFormat sdf = new SimpleDateFormat("", locale);
        this.months = sdf.getDateFormatSymbols().getMonths();
        this.weekdays = sdf.getDateFormatSymbols().getShortWeekdays();
        
    }
    
    public void init(int year, int month)
    {
        this.year = year;
        this.month = month;
        Calendar calendar = Calendar.getInstance(locale);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        Date today = DateUtils.getDateNow();
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        this.date = calendar.getTime(); 

        // Init all calendar cells
        for (int w = 0; w< days.length; w++)
        {
            int dayOfWeek = getDayOfWeek(calendar);
            for (int d=dayOfWeek; d<days[w].length; d++)
            {
                Date date = calendar.getTime();
                String sDay = "&nbsp;" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)) + "&nbsp;";         
                
                days[w][d] = new CalendarDayInfo(date, sDay, date.equals(today));
                // Next Day
                calendar.add(Calendar.DAY_OF_MONTH, 1);

                if(month!=calendar.get(Calendar.MONTH))
                    // finish month
                    return;
            }
        }
    }
    
    // ------- Properties -------

    public int getWeekCount()
    {
        return days.length;
    }
    
    public CalendarDayInfo[] getWeek(int i)
    {
        return days[i]; 
    }
    
    public CalendarDayInfo getDayInfo(Date date)
    {
        if (date==null)
            return null;
        // Init Calendar
        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        date = cal.getTime();
        // Search for Day
        for (int w = 0; w< days.length; w++)
        {
            for (int d=0; d<days[w].length; d++)
            {
                CalendarDayInfo cdi = days[w][d]; 
                if (cdi!=null && cdi.date.equals(date))
                {
                   return cdi;
                }
            }
        }
        // Date not found
        return null;
    }
    
    public boolean getLink(Date date)
    {
        CalendarDayInfo cdi = getDayInfo(date);
        return (cdi!=null ? cdi.link : null);
    }
    
    public void setLink(Date date, boolean link)
    {
        CalendarDayInfo cdi = getDayInfo(date);
        if (cdi!=null)
            cdi.setLink(link);
    }
    
    public void setSelected(boolean selected)
    {
        for (int w = 0; w< days.length; w++)
        {
            for (int d=0; d<days[w].length; d++)
            {
                if(days[w][d]!=null)
                   days[w][d].setSelected(selected);
            }
        }
    }
    
    public void setSelected(Date date, boolean selected)
    {
        CalendarDayInfo cdi = getDayInfo(date);
        if (cdi!=null)
            cdi.setSelected(selected);
    }
    
    public void setSelected(int kalendarWeek ,boolean selected)
    {
        int kw = 0;
        for (int w = 0; w< days.length; w++)
        {
            kw = getKalendarWeek(days[w]);
            if(kw != 0 && kalendarWeek == kw)
                setSelected(days[w],selected);
        }
    }  

    public int getMonth()
    {
        return month;
    }
    
    public int getYear()
    {
        return year;
    }
    
    public String getLinkText()
    {
        return formatDate(date);
    }
    
    public String getMonthText()
    {
        return ((month>=0 && month<months.length) ? months[month] : "");
    }
    
    public String getYearText()
    {
        return String.valueOf(year);
    }
    
    public String getDayOfWeekText(int day)
    {
        if(day==6)
            return weekdays[1];
        else
            return weekdays[day+2];
    }
    
    public int getKalendarWeek(CalendarDayInfo[] week)
    {
        CalendarDayInfo validDay = null;
        int retVal = 0;
        for (int i = 0;week!=null &&  i<week.length; i++)
        {
            validDay = week[i];
            if (validDay != null)
            {
                retVal = DateUtils.getWeekOfYear(validDay.getDate(),locale);
                break;
            }
        }
        return retVal;
    }
    
    // ------- Helpers -------
    
    private int getDayOfWeek(Calendar c)
    {
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        if( dayOfWeek == Calendar.SUNDAY)
            dayOfWeek=6;
        else
            dayOfWeek-=2;
        
        return dayOfWeek;
    }
    
    private void setSelected(CalendarDayInfo[] week, boolean selected)
    {
        for (int d=0; d<week.length; d++)
        {
            if(week[d]!=null)
                week[d].setSelected(selected);
        }
    }
}
