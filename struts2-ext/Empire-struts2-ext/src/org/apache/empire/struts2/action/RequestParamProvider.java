/*
 * ESTEAM Software GmbH, 19.07.2007
 */
package org.apache.empire.struts2.action;

import java.util.Map;

public interface RequestParamProvider
{
    Map getRequestParameters();

    boolean hasRequestParam(String param);
    
    String getRequestParam(String param);
    
    String[] getRequestArrayParam(String param);
    
}
