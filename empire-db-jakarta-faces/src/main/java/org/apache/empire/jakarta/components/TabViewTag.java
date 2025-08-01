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
package org.apache.empire.jakarta.components;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidOperationException;
import org.apache.empire.jakarta.app.FacesUtils;
import org.apache.empire.jakarta.controls.InputControl;
import org.apache.empire.jakarta.controls.InputControlManager;
import org.apache.empire.jakarta.utils.StyleClass;
import org.apache.empire.jakarta.utils.TagEncodingHelper;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory;
import org.apache.empire.jakarta.utils.TagEncodingHelperFactory.TagEncodingHolder;
import org.apache.empire.jakarta.utils.TagStyleClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.html.HtmlCommandLink;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.ActionListener;

public class TabViewTag extends UIOutput implements TagEncodingHolder
{
    // Logger
    private static final Logger       log                    = LoggerFactory.getLogger(TabViewTag.class);

    protected static String           TAB_PAGE_ID            = "pageId";
    
    protected static String           TAB_ACTIVE_PAGE        = "activePage";
    
    protected static String           TAB_ACTIVE_INDEX       = "activeIndex";

    protected static String           TABLINK_ID_PREFIX      = "tabLink";

    protected static String           TAB_RENDERED_ATTRIBUTE = "visible";

    protected final TagEncodingHelper helper                 = TagEncodingHelperFactory.create(this, TagStyleClass.TAB_VIEW.get());

    /**
     * TabPageActionListener
     */
    public static class TabPageActionListener implements ActionListener // , StateHolder
    {
        public TabPageActionListener()
        {
            // constructor for state-saving 
        }

        public TabPageActionListener(TabViewTag tabViewTag)
        {
            // internal constructor from TabViewTag
        }

        @Override
        public void processAction(ActionEvent actionEvent)
            throws AbortProcessingException
        {
            // tabView must be the parent of the component!
            UIComponent tabView = actionEvent.getComponent().getParent();
            if (!(tabView instanceof TabViewTag))
            {
                throw new InvalidArgumentException("ActionEvent.component", (tabView!=null ? tabView.getClass().getName() : null));
            }
            // Invoke
            TabViewTag tvt = (TabViewTag) tabView;
            tvt.setActiveTab(actionEvent);
        }
    }

    /*
     * TabViewMode
     */
    public enum TabViewMode 
    {
        TABLE (InputControl.HTML_TAG_TABLE, InputControl.HTML_TAG_TR,  InputControl.HTML_TAG_TD,  InputControl.HTML_TAG_TD
             , InputControl.HTML_TAG_TABLE, InputControl.HTML_TAG_TR,  InputControl.HTML_TAG_TD),
        GRID  (InputControl.HTML_TAG_DIV,   null,                      InputControl.HTML_TAG_DIV, InputControl.HTML_TAG_DIV
             , InputControl.HTML_TAG_DIV,   null,                      InputControl.HTML_TAG_DIV);
        
        public final String BAR_TAG;        // InputControl.HTML_TAG_TABLE
        public final String BAR_ROW_TAG;    // InputControl.HTML_TAG_TR
        public final String BAR_ITEM_TAG;   // InputControl.HTML_TAG_TD
        public final String BAR_PAD_TAG;    // InputControl.HTML_TAG_TD
        public final String PANEL_TAG;      // InputControl.HTML_TAG_TABLE
        public final String PAGE_WRAP_TAG;  // InputControl.HTML_TAG_TR
        public final String PAGE_TAG;       // InputControl.HTML_TAG_TD
        
        private TabViewMode(String barTag, String barRowTag, String barItemTag, String barPadTag, String panelTag, String pageWrapTag, String pageTag)
        {
            this.BAR_TAG = barTag;          // InputControl.HTML_TAG_TABLE
            this.BAR_ROW_TAG = barRowTag;   // InputControl.HTML_TAG_TR
            this.BAR_ITEM_TAG = barItemTag; // InputControl.HTML_TAG_TD
            this.BAR_PAD_TAG = barPadTag;   // InputControl.HTML_TAG_TD
            this.PANEL_TAG = panelTag;      // InputControl.HTML_TAG_TABLE
            this.PAGE_WRAP_TAG = pageWrapTag;   // InputControl.HTML_TAG_TR
            this.PAGE_TAG = pageTag;            // InputControl.HTML_TAG_TD
        }
        
        public static TabViewMode detect(String mode)
        {
            if (mode==null || mode.length()==0)
                return TABLE;
            // find
            TabViewMode[] values = values();
            for (int i=0; i<values.length; i++)
            {
                if (values[i].name().equalsIgnoreCase(mode))
                    return values[i]; 
            }
            // not found
            log.warn("TabViewMode \"{}\" not found. Using default!", mode);
            return TABLE;
        }
        
        @Override
        public String toString()
        {
            return name();
        }
    }
    
    private TabViewMode mode;
    
    public TabViewTag()
    {
        log.trace("TabViewTag created");
    }

    @Override
    public String getFamily()
    {
        return TagEncodingHelper.COMPONENT_FAMILY;
    }

    @Override
    public TagEncodingHelper getEncodingHelper()
    {
        return helper;
    }

    public TabViewMode getViewMode()
    {
        if (this.mode==null)
            this.mode = TabViewMode.detect(helper.getTagAttributeString("mode", TabViewMode.TABLE.name())); 
        return mode;
    }

    public int getActivePageIndex()
    {
        Object active = this.helper.getTagAttributeValue(TAB_ACTIVE_INDEX);
        return ObjectUtils.getInteger(active);
    }

    public void setActivePageIndex(int activeIndex)
    {
        ValueExpression ve = this.getValueExpression(TAB_ACTIVE_INDEX);
        if (ve != null)
        { // set active index
            FacesContext fc = FacesUtils.getContext();
            ve.setValue(fc.getELContext(), activeIndex);
        }
        else
        { // save activeIndex
            getAttributes().put(TAB_ACTIVE_INDEX, activeIndex);
        }
    }

    public String getActivePageId()
    {
        Object active = this.helper.getTagAttributeValue(TAB_ACTIVE_PAGE);
        return ObjectUtils.getString(active);
    }

    public void setActivePageId(String pageId)
    {
        ValueExpression ve = this.getValueExpression(TAB_ACTIVE_PAGE);
        if (ve != null)
        {   // set active index
            FacesContext fc = FacesUtils.getContext();
            ve.setValue(fc.getELContext(), pageId);
        }
    }

    public TabPageTag getActiveTabPage()
    {
        Iterator<UIComponent> ci = getFacetsAndChildren();
        if (ci.hasNext() == false)
        {   log.warn("Invalid TabPage definition!");
            return null;
        }
        UIComponent panel = ci.next();
        int index = 0;
        int activeIndex = getActivePageIndex();
        String activePageId = getActivePageId();
        for (UIComponent c : panel.getChildren())
        {   // Find Tab pages
            if (!(c instanceof TabPageTag))
            {
                continue;
            }
            // check visibility
            TabPageTag page = (TabPageTag) c;
            if (!isPageVisible(page))
                continue; // don't count hidden pages
            // found a page
            boolean active = (activePageId!=null ? activePageId.equals(page.getId()) : (index == activeIndex));
            if (active)
                return page;
            // next
            index++;
        }
        // not found
        if (activePageId!=null)
            log.warn("No visible tab-page with pageId {} has been found!", activePageId);
        else
            log.warn("No visible tab-page with index {} has been found!", activeIndex);
        return null;
    }

    public void setActiveTab(ActionEvent event)
    {
        log.debug("setActiveTab");
        // done
        UIComponent comp = event.getComponent();
        String tabNo = comp.getId().substring(TABLINK_ID_PREFIX.length());
        int pageIndex = ObjectUtils.getInteger(tabNo);
        if (pageIndex == getActivePageIndex())
        {   // already set
            log.warn("setActiveTab is called for active page!");
            return;
        }

        // set active index
        setActivePageIndex(pageIndex);
        // set active pageId
        String pageId = (String)comp.getAttributes().get(TAB_PAGE_ID);
        setActivePageId(pageId);

        // TabChangeListener
        Object tcl = getAttributes().get("tabChangedListener");
        if (tcl != null)
        {   // check Method expression
            if (!(tcl instanceof MethodExpression))
                throw new InvalidOperationException("tabChangedListener is not a valid method expression!");
            // invoke
            FacesContext fc = FacesUtils.getContext();
            MethodExpression methodExpression = (MethodExpression)tcl;
            methodExpression.invoke(fc.getELContext(), new Object[] { pageIndex });
        }
    }

    @Override
    public void encodeBegin(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeBegin(context);

        // registerTabViewBean
        // context.getExternalContext().getRequestMap().put("tabView", this);
        getViewMode();

        // render components
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(InputControl.HTML_TAG_DIV, this);
        helper.writeComponentId(writer, true); /* must always render id! */
        helper.writeStyleClass(writer);
        // style
        String style = this.helper.getTagAttributeString("style");
        if (StringUtils.isNotEmpty(style))
            this.helper.writeAttribute(writer, InputControl.HTML_ATTR_STYLE, style);

        // The Blind
        String showTabBlindJs = null;
        if (ObjectUtils.getBoolean(helper.getTagAttributeValue("showBlind")))
        {   // hide bar
            String tabBlindClass = TagStyleClass.TAB_BLIND.get();
            writer.startElement(InputControl.HTML_TAG_DIV, this);
            writer.writeAttribute(InputControl.HTML_ATTR_CLASS, tabBlindClass, null);
            writer.writeAttribute(InputControl.HTML_ATTR_STYLE, "display:none", null);
            writer.endElement(InputControl.HTML_TAG_DIV);
            // showTabBlindJs
            String tabViewId = this.getClientId();
            showTabBlindJs = StringUtils.concat("$(document.getElementById('", tabViewId, "')).find('.", tabBlindClass,"').show();");
        }
        
        // The Tabs
        if (ObjectUtils.getBoolean(this.helper.getTagAttributeValue("hideTabBar")))
        {   // hide bar
            encodeTabs(context, null, null);
        }
        else
        {   // show bar
            writer.startElement(mode.BAR_TAG, this);
            writer.writeAttribute(InputControl.HTML_ATTR_CLASS, TagStyleClass.TAB_BAR.get(), null);
            if (mode.BAR_ROW_TAG!=null)
                writer.startElement(mode.BAR_ROW_TAG, this);
            // encode Tabs
            encodeTabs(context, writer, showTabBlindJs);
            // Bar padding item
            if (mode.BAR_PAD_TAG!=null)
            {   // Bar padding item
                writer.startElement(mode.BAR_PAD_TAG, this);
                writer.writeAttribute(InputControl.HTML_ATTR_CLASS, TagStyleClass.TAB_BAR_PADDING.get(), null);
                encodeTabPadding(context, writer);
                writer.endElement(mode.BAR_PAD_TAG);
            }
            else
                encodeTabPadding(context, writer);
            // finish
            if (mode.BAR_ROW_TAG!=null)
                writer.endElement(mode.BAR_ROW_TAG);
            writer.endElement(mode.BAR_TAG);
        }
        
        // The Pages
        writer.startElement(mode.PANEL_TAG, this);
        writer.writeAttribute(InputControl.HTML_ATTR_CLASS, TagStyleClass.TAB_PANEL.get(), null);
        String minHeight = this.helper.getTagAttributeString("minHeight");
        if (StringUtils.isNotEmpty(minHeight))
        {
            writer.writeAttribute(InputControl.HTML_ATTR_STYLE, "min-height:" + minHeight, null);
        }
    }

    @Override
    public boolean getRendersChildren()
    {
        return super.getRendersChildren();
    }

    @Override
    public void encodeChildren(FacesContext context)
        throws IOException
    {
        super.encodeChildren(context);
    }

    @Override
    public void encodeEnd(FacesContext context)
        throws IOException
    {
        // call base
        super.encodeEnd(context);
        // close
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement(mode.PANEL_TAG);
        writer.endElement(InputControl.HTML_TAG_DIV);
    }

    @Override
    public void decode(FacesContext context)
    {
        for (UIComponent c : getChildren())
        {
            c.decode(context);
        }
        super.decode(context);
    }

    @Override
    public void processDecodes(FacesContext context)
    {
        setRenderTabs(context);
        super.processDecodes(context);
    }

    @Override
    public void processValidators(FacesContext context)
    {
        setRenderTabs(context);
        super.processValidators(context);
    }

    @Override
    public void processUpdates(FacesContext context)
    {
        setRenderTabs(context);
        super.processUpdates(context);
    }
    
    /*
     * internal
     */

    protected void encodeTabs(FacesContext context, ResponseWriter writer, String showTabBlindJs)
        throws IOException
    {
        Iterator<UIComponent> ci = getFacetsAndChildren();
        if (ci.hasNext() == false)
        {
            log.warn("Invalid TabPage definition!");
            return;
        }
        UIComponent panel = ci.next();
        int index = 0;
        int activeIndex = getActivePageIndex();
        String activePageId = getActivePageId();
        // Patch for MOJARRA: Remove HtmlCommandLinks
        List<UIComponent> chk = panel.getChildren();
        for (int i = chk.size() - 1; i >= 0; i--)
        {
            if ((chk.get(i) instanceof HtmlCommandLink))
                chk.remove(i);
        }
        // Create Page Links
        for (UIComponent c : panel.getChildren())
        { // Find Tab pages
            if (!(c instanceof TabPageTag))
            {
                continue;
            }
            // found
            TabPageTag page = (TabPageTag) c;
            // render tab-page? default is true
            boolean visible = isPageVisible(page);
            if (!visible)
            {   // don't render content
                page.setRendered(false);
                continue;
            }
            // check active
            boolean active = (activePageId!=null ? activePageId.equals(page.getId()) : (index == activeIndex));
            if (active && (index!=activeIndex))
            {   // set index
                setActivePageIndex(index);
            }
            // write tabs
            if (writer!=null)
            {   // encode Tab
                boolean disabled = isPageDisabled(page);
                writer.startElement(mode.BAR_ITEM_TAG, this);
                // tab label
                StyleClass styleClass = new StyleClass(TagStyleClass.TAB_LABEL);
                if (active)
                {
                    styleClass.add(TagStyleClass.TAB_ACTIVE);
                }
                else if (disabled)
                {
                    styleClass.add(TagStyleClass.TAB_DISABLED);
                }
                writer.writeAttribute(InputControl.HTML_ATTR_CLASS, styleClass, null);
                // encode Link
                encodeTabLink(context, writer, index, page, (active || disabled), showTabBlindJs);
                writer.endElement(mode.BAR_ITEM_TAG);
            }
            // set rendered
            page.setRendered(active);
            // next
            index++;
        }
    }
    
    protected void encodeTabLink(FacesContext context, ResponseWriter writer, int index, TabPageTag pageTag, boolean disabled, String showTabBlindJs)
        throws IOException
    {
        // Add component
        HtmlCommandLink link = null;
        List<UIComponent> tabLinks = getChildren();
        if (tabLinks.size() > index)
        {
            UIComponent c = tabLinks.get(index);
            if (c instanceof HtmlCommandLink)
            {
                link = (HtmlCommandLink) c;
            }
            else
            { // Something's wrong here?
                log.error("INFO: Unexpected child node for {}! Child item type is {}.", getClass().getName(), c.getClass().getName());
                // encode anyway
                c.setRendered(true);
                c.encodeAll(context);
                c.setRendered(false); // Don't render twice!
                return;
            }
        }
        if (link == null)
        { // create the tab-Link   
            String linkId = TABLINK_ID_PREFIX + String.valueOf(index);
            link = createTabCommandLink(context, pageTag, linkId);
            tabLinks.add(index, link);
            // Set StyleClass
            String styleClass = StringUtils.coalesce(link.getStyleClass(), TagStyleClass.TAB_LINK.get());
            String extraClass = pageTag.helper.getTagAttributeString("labelClass");
            if (StringUtils.isNotEmpty(extraClass))
                styleClass = StringUtils.concat(styleClass, " ", extraClass);
            link.setStyleClass(styleClass);
            // showTabBlindJs
            if (showTabBlindJs!=null)
            {   // append showTabBlindJs
                String onClick = link.getOnclick();
                onClick = StringUtils.concat(onClick, showTabBlindJs);
                link.setOnclick(onClick);
            }
            // Set TabPageActionListener
            TabPageActionListener tpal = new TabPageActionListener(this);
            link.addActionListener(tpal);
        }
        // init linkComponent
        link.getAttributes().put(TAB_PAGE_ID, pageTag.getId());
        link.setValue(pageTag.getTabLabel());
        link.setTitle(pageTag.getTabTitle());
        link.setDisabled(disabled);
            
        // encode link
        link.setRendered(true);
        encodeTabCommandLink(context, pageTag, link);
        link.setRendered(false); // Don't render twice!
    }

    protected HtmlCommandLink createTabCommandLink(FacesContext context, TabPageTag pageTag, String linkId)
    {
        // CommandLink link 
        HtmlCommandLink link = InputControlManager.createComponent(context, HtmlCommandLink.class);
        link.setId(linkId);
        return link;
    }
    
    protected void encodeTabCommandLink(FacesContext context, TabPageTag pageTag, HtmlCommandLink link)
            throws IOException
    {
        UIComponent labelFacet = pageTag.getFacet(TabPageTag.LABEL_FACET_NAME);
        if (labelFacet!=null)
        {   // custom rendering
            link.encodeBegin(context);
            labelFacet.encodeAll(context);
            link.encodeEnd(context);
        }
        else
        {   // default
            link.encodeAll(context);
        }
    }
    
    protected void encodeTabPadding(FacesContext context, ResponseWriter writer) throws IOException
    {
        UIComponent paddingFacet = getFacet("tabPadding");
        if (paddingFacet!=null)
        {   // custom rendering
            paddingFacet.encodeAll(context);
        }
    }

    protected void setRenderTabs(FacesContext context)
    {
        if (getFacetCount()<1)
            return; // No TabPages yet 
        // the tab panel
        UIComponent panel = getFacets().values().iterator().next();
        int tabIndex=0;
        int activeIndex = getActivePageIndex();
        for (UIComponent page : panel.getChildren())
        {
            if (!(page instanceof TabPageTag) || !isPageVisible((TabPageTag)page))
            {
                continue;
            }
            // check active
            boolean rendered = (tabIndex==activeIndex);
            if (page.isRendered()!=rendered)
            {
                // log.debug("Tab-Page {} rendered has changed to {}", tabIndex, rendered);
                page.setRendered(rendered);
            }
            tabIndex++;
        }
    }
    
    protected boolean isPageVisible(TabPageTag page)
    {
        return ObjectUtils.getBoolean(page.getAttributes().get(TAB_RENDERED_ATTRIBUTE), true);
    }
    
    protected boolean isPageDisabled(TabPageTag page)
    {
        return ObjectUtils.getBoolean(page.getAttributes().get("disabled"), false);
    }
    
}
