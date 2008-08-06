package org.apache.empire.struts2.web;

import javax.servlet.http.HttpSession;

public interface WebSession
{
    public final String SESSION_NAME  = "webSession";

    void init(HttpSession httpSession, Object application);
}
