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
package org.apache.empire.jakarta.pages;

import java.util.Map;

import jakarta.faces.context.FacesContext;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.InvalidArgumentException;


public class PageElement<P extends Page> // *Deprecated* implements Serializable
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    protected final P page;

    private final String propertyName;

    protected PageElement(P page, String name)
    {
        if (page==null)
            throw new InvalidArgumentException("page", page);
        if (StringUtils.isEmpty(name))
            throw new InvalidArgumentException("name", name);
        // set params
        this.page = page;
        this.propertyName = name;
        // register with page
        page.registerPageElement(this);
    }

    public String getPropertyName()
    {
        return propertyName;
    }
    
    public P getPage()
    {
        return page;
    }

    protected void onInitPage()
    {
        // Chance to init the page
    }

    protected void onRefreshPage()
    {
        // Chance to init the page
    }

    /* Session Object handling */

    @SuppressWarnings("unchecked")
    protected <T> T getSessionObject(Class<T> type)
    {
        String beanName = StringUtils.concat(page.getPageName(), ".", propertyName, ".", type.getSimpleName());
        Map<String, Object> map = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        return (T) map.get(beanName);
    }

    protected <T> void setSessionObject(Class<T> type, T object)
    {
        String beanName = StringUtils.concat(page.getPageName(), ".", propertyName, ".", type.getSimpleName());
        Map<String, Object> map = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        if (object != null)
            map.put(beanName, object);
        else
            map.remove(beanName);
    }

    protected final <T> void removeSessionObject(Class<T> type)
    {
        setSessionObject(type, null);
    }

    /*
     * Removed with EMPIREDB-421:
     * 
    public DBContext getDBContext(DBDatabase db)
    {
        return page.getDBContext(db);
    }

    public DBContext getDBContext(DBObject dbo)
    {
        if (dbo==null)
            throw new InvalidArgumentException("dbo", dbo);
        return getDBContext(dbo.getDatabase());
    }
    */
    
    /**
     * generates a default property name for the bean list
     * @param rowset the rowset
     * @return a propertyName
     */
    protected static String getDefaultPropertyName(DBRowSet rowset)
    {
        String name = rowset.getName();
        if (name==null)
            return "unknown"; // no name provided!
        // compute name
        name = name.toLowerCase();        
        String res = "";
        int beg=0;
        while (beg<name.length())
        {
            int end = name.indexOf('_', beg);
            if (end<0)
                end = name.length();
            // assemble
            if (end>beg)
            {
                if (beg==0)
                {   // begin with all lower cases
                    res = name.substring(beg, end);
                }
                else
                {   // add word where first letter is upper case 
                    res += name.substring(beg, beg+1).toUpperCase();
                    if (end-beg>1)
                    {
                        res += name.substring(beg+1, end);
                    }
                }
            }
            // next
            beg = end + 1;
        }
        // Result
        return res;
    }
    
}
