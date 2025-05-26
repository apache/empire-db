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
    
    @Override
    public String toString()
    {
        return this.styleClassName;
    }
}
