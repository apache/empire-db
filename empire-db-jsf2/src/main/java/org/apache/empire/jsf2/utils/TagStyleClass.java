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
package org.apache.empire.jsf2.utils;

import org.apache.empire.commons.StringUtils;

/**
 * Collection of TagStyleClasses
 */
public enum TagStyleClass
{
    VALUE("eVal"),
    VALUE_NULL("eValNull"),
    VALUE_INVALID("eInvalid"),
    LABEL("eLabel"),
    INPUT("eInput"),
    INPUT_DIS("eInpDis"),
    INPUT_REQ("eInpReq"),
    INPUT_MOD("eInpModified"),
    INPUT_HINT("eInputHint"),
    INPUT_TYPE_PREFIX("eInpType"),
    INPUT_WRAPPER("eWrapInp"),
    VALUE_WRAPPER("eWrapVal"),
    CONTROL("eControl"),
    CONTROL_LABEL("eCtlLabel"),
    CONTROL_INPUT("eCtlInput"),
    CONTROL_PLACEHOLDER("eCtlPlaceholder"),
    FORM_GRID("eFormGrid"),
    SELECT("eSelect"),
    RADIO("eCtlRadio"),
    LINK("eLink"),
    TITLE("eTitle"),
    MENU("eMenuList"),
    UNIT("eUnit"),
    TAB_VIEW("eTabView"),
    TAB_BLIND("eTabBlind"),
    TAB_BAR("eTabBar"),
    TAB_BAR_PADDING("eTabBarEmpty"),
    TAB_PANEL("eTabPanel"),
    TAB_PAGE("eTabPage"),
    TAB_LINK("eTabLink"),
    TAB_LABEL("eTabLabel"),
    TAB_ACTIVE("eTabActive"),
    TAB_DISABLED("eTabDisabled");
    
    private static final char SPACE = ' ';
    
    private String styleClassName;
    
    private TagStyleClass(String styleClassName)
    {
        this.styleClassName = styleClassName;
    }

    public String get()
    {
        return styleClassName;
    }

    public void set(String styleClassName)
    {
        this.styleClassName = styleClassName;
    }
    
    /* operations */

    private static final int find(String styleClasses, String styleClassName, int fromIdx)
    {
        if (styleClasses==null)
            return -1;
        // find
        int idx = styleClasses.indexOf(styleClassName, fromIdx);
        if (idx<0)
            return -1;
        // starts with space?
        if (idx>0 && styleClasses.charAt(idx-1)!=SPACE)
            return find(styleClasses, styleClassName, idx+1); // recurse
        // ends with space?
        int end = idx+styleClassName.length(); 
        if (end<styleClasses.length() && styleClasses.charAt(end)!=SPACE)
            return find(styleClasses, styleClassName, idx+1); // recurse
        // found
        return idx;
    }
    
    public static final boolean existsIn(String styleClasses, String styleClassName)
    {   // find
        return (find(styleClasses, styleClassName, 0)>=0);
    }

    public final boolean existsIn(String styleClasses)
    {
        return existsIn(styleClasses, this.styleClassName);
    }
    
    public static final String addTo(String styleClasses, String styleClassName)
    {
        // check 
        if (styleClasses==null || styleClasses.length()==0)
            return styleClassName;
        if (existsIn(styleClasses, styleClassName))
            return styleClasses;
        // add with space
        return StringUtils.concat(styleClasses, " ", styleClassName);
    }

    public final String addTo(String styleClasses)
    {
        return addTo(styleClasses, this.styleClassName);
    }
    
    public final String append(String... appends)
    {
        String result = this.styleClassName;
        for (int i=0; i<appends.length; i++)
        {   // append
            String append = appends[i];
            if (append!=null && append.length()>0)
                result = addTo(result, append);
        }
        return result;
    }
    
    public static final String removeFrom(String styleClasses, String styleClassName)
    {
        // Check name
        if (styleClassName==null)
            return styleClasses;
        // find
        int idx = find(styleClasses, styleClassName, 0);
        if (idx<0)
            return styleClasses; // not contained
        // remove now
        if (idx<=1)
        {   // remove from start
            idx += styleClassName.length();
            if (styleClasses.length()>idx && styleClasses.charAt(idx)==SPACE)
                idx++;
            return styleClasses.substring(idx);
        }
        if (idx+styleClassName.length()==styleClasses.length())
        {   // remove from end
            return styleClasses.substring(0, idx-1); // at the end
        }
        // in between
        int after  = idx + styleClassName.length();
        int before = idx - 1; // SPACE assumed!
        return StringUtils.concat(styleClasses.substring(0, before), styleClasses.substring(after));
    }

    public final String removeFrom(String styleClasses)
    {
        return removeFrom(styleClasses, this.styleClassName);
    }
    
    public final static String addOrRemove(String styleClasses, String styleClassName, boolean add)
    {
        // add or remove
        if (add)
            return TagStyleClass.addTo(styleClasses, styleClassName);
        else
            return TagStyleClass.removeFrom(styleClasses, styleClassName);
    }
    
    public final String addOrRemove(String styleClasses, boolean add)
    {
        return addOrRemove(styleClasses, this.styleClassName, add);
    }
    
    @Override
    public String toString()
    {
        return this.styleClassName;
    }
}
