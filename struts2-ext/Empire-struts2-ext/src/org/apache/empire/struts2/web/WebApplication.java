package org.apache.empire.struts2.web;

import javax.servlet.ServletContext;

public interface WebApplication
{
    public final String APPLICATION_NAME  = "webApp";

    void init(ServletContext servletContext);
}
