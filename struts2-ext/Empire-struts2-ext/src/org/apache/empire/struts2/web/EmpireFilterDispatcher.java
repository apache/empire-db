package org.apache.empire.struts2.web;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterConfig;

import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.FilterDispatcher;

public class EmpireFilterDispatcher extends FilterDispatcher
{
    public EmpireFilterDispatcher()
    {
        // Default Constructor
    }

    @Override
    protected Dispatcher createDispatcher(FilterConfig filterConfig)
    {
        Map<String, String> params = new HashMap<String, String>();
        for (Enumeration e = filterConfig.getInitParameterNames(); 
             e.hasMoreElements();)
        {
            String name = (String) e.nextElement();
            String value = filterConfig.getInitParameter(name);
            params.put(name, value);
        }
        return new EmpireStrutsDispatcher(filterConfig.getServletContext(), params);
    }

}
