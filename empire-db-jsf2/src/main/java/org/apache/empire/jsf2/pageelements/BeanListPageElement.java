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
package org.apache.empire.jsf2.pageelements;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.event.ValueChangeEvent;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.order.DBOrderByExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.pages.Page;
import org.apache.empire.jsf2.utils.ListColumnFinder;
import org.apache.empire.jsf2.utils.ListItemSelection;
import org.apache.empire.jsf2.utils.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeanListPageElement<T> extends ListPageElement<T> implements ListItemSelection
{
    private static final long   serialVersionUID     = 1L;

    private static final Logger log                  = LoggerFactory.getLogger(BeanListPageElement.class);

    public static final String  IDPARAM_PROPERTY     = "idParam";
    
    public static final String  NO_RESULT_ATTRIBUTE  = "noQueryResult";

    private ListTableInfo       listTableInfo        = null;

    protected DBRowSet          rowset;

    protected Column            defaultSortColumn;

    protected boolean           defaultSortAscending = true;

    protected DBOrderByExpr     secondarySortOrder   = null;
    
    /**
     * Extended ListTableInfo
     */
    public static class BeanListTableInfo extends ListTableInfo
    {
        private static final long serialVersionUID = 1L;

        private DBCommand         queryCmd         = null;

        public DBCommand getQueryCmd()
        {
            return queryCmd;
        }

        public void setQueryCmd(DBCommand queryCmd)
        {
            this.queryCmd = queryCmd;
        }
    }

    public BeanListPageElement(Page page, Class<T> beanClass, DBColumn defaultSortColumn, String propertyName, boolean test)
    {
        super(page, beanClass, propertyName);
        // Check
        if (defaultSortColumn == null)
            throw new InvalidArgumentException("defaultSortColumn", defaultSortColumn);
        // Set Bean Class and more
        this.rowset = defaultSortColumn.getRowSet();
        this.defaultSortColumn = defaultSortColumn;
        // Default Sort Order
        if (defaultSortColumn.getDataType() == DataType.DATE || defaultSortColumn.getDataType() == DataType.DATETIME)
        { // Date sort order is descending by default
            defaultSortAscending = false;
        }
    }

    @Override
    protected void onInitPage()
    {
        ListTableInfo lti = getTableInfo();
        if (lti.isValid() && items == null)
        { // loadBookings
            loadItems(true);
        }
    }

    @Override
    protected void onRefreshPage()
    {
        ListTableInfo lti = getTableInfo();
        if (lti.isValid() && lti.isModified())
        { // load
            loadItems(false);
        }
        else
        { // hide the loading indicator on client (JavaScript call)
            if (lti.isAllowPagination())
                updateScrollbar();
        }
    }

    @Override
    public int getItemCount()
    {
        return getTableInfo().getItemCount();
    }

    public Column getDefaultSortColumn()
    {
        return defaultSortColumn;
    }

    public boolean isDefaultSortAscending()
    {
        return defaultSortAscending;
    }

    public void setDefaultSortAscending(boolean defaultSortAscending)
    {
        this.defaultSortAscending = defaultSortAscending;
    }

    public DBOrderByExpr getSecondarySortOrder()
    {
        return secondarySortOrder;
    }

    public void setSecondarySortOrder(DBOrderByExpr secondarySortOrder)
    {
        this.secondarySortOrder = secondarySortOrder;
    }

    /** session scoped properties **/
    @Override
    public ListTableInfo getTableInfo()
    {
        // Lazy initialization
        if ((listTableInfo == null) && (listTableInfo = getSessionObject(ListTableInfo.class)) == null)
        { // Create and put on session
            listTableInfo = new BeanListTableInfo();
            listTableInfo.setSortColumnName(defaultSortColumn.getName());
            listTableInfo.setSortAscending(defaultSortAscending);
            setSessionObject(ListTableInfo.class, listTableInfo);
        }
        return listTableInfo;
    }

    @Override
    public void clearItems()
    { // clear Items
        super.clearItems();
        setSessionObject(ListTableInfo.class, null);
        listTableInfo = null;
        // Clear parameters
        getParameterMap().clear(rowset);
    }
    
    protected ParameterMap getParameterMap()
    {
    	return FacesUtils.getParameterMap(FacesUtils.getContext());
    }

    /**
     * Init list items with pagination
     * 
     * @param queryCmd
     * @param pageSize
     */
    public void initItems(DBCommand queryCmd, DBCommand countCmd, int pageSize)
    {
        clearItems();
        // Init List Table Info
        BeanListTableInfo lti = (BeanListTableInfo) getTableInfo();
        lti.setQueryCmd(queryCmd);
        if (pageSize > 0)
        { // Negative count means: loadItems should load all items.
            countCmd.clearSelect();
            countCmd.select(rowset.count());
            int count = rowset.getDatabase().querySingleInt(countCmd.getSelect(), countCmd.getParamValues(), 0, getConnection(rowset));
            lti.init(count, pageSize);
        }
        else
        { // 0 or more items available
            lti.init(-1, 0);
        }
        // Init List table Info
        lti.setSortOrderChanged(true);
        loadItems(true);
        // log
        int count = getTableInfo().getItemCount();
        if (count==0)
            FacesUtils.setRequestAttribute(FacesUtils.getContext(), NO_RESULT_ATTRIBUTE, true);
        // log
        log.info("ItemList initialized for {} item count is {}.", getPropertyName(), count);
    }

    /**
     * Init list items without pagination
     * 
     * @param queryCmd
     */
    public final void initItems(DBCommand queryCmd, int pageSize)
    {
        DBCommand countCmd = queryCmd.clone();
        initItems(queryCmd, countCmd, 0);
    }

    /**
     * Init list items without pagination
     * 
     * @param queryCmd
     */
    public final void initItems(DBCommand queryCmd)
    {
        initItems(queryCmd, 0);
    }

    /**
     * Returns true if (and only if) items are valid but empty
     * @return
     */
    public boolean isResultEmpty()
    {
        Object noQueryResult = FacesUtils.getRequestAttribute(FacesUtils.getContext(), NO_RESULT_ATTRIBUTE);
        return (noQueryResult!=null) ? ObjectUtils.getBoolean( noQueryResult ) : false;
    }

    private void loadItems(boolean initScrollbar)
    {
        // DBReader
        BeanListTableInfo lti = (BeanListTableInfo) getTableInfo();
        DBReader r = new DBReader();
        try
        { // Check command
            DBCommand queryCmd = lti.getQueryCmd();
            if (queryCmd == null)
                throw new ObjectNotValidException(this);

            boolean loadPageFromPosition = lti.isValid() && lti.isAllowPagination();
            lti.setValid(false);

            if (lti.isSortOrderChanged())
            { // Set Sort order
                setOrderBy(queryCmd);
                lti.setSortOrderChanged(false);
            }

            // DBReader.open immer nur innerhalb eines try {} finally {} blocks!
            r.open(queryCmd, getConnection(queryCmd));

            // get position from the session
            int position = 0;
            int maxItems = 1000;
            if (loadPageFromPosition)
            {
                position = lti.getPosition();
                if (position < 0)
                { // position < 0 is not possible, set to 0
                    position = 0;
                }
                if (position > lti.getItemCount() - lti.getPageSize())
                { // position > count of entries is not possible, set to max
                    position = lti.getItemCount() - lti.getPageSize();
                }
                if (position > 0)
                { // we are not at position 0, "skipping" entries
                    r.skipRows(position);
                }
                else
                    position = 0;
                // maxItems
                maxItems = lti.getPageSize();
            }

            // Read all Items
            items = r.getBeanList(beanClass, maxItems);
            if (items == null)
                throw new UnexpectedReturnValueException(items, "DBReader.getBeanList");
            generateIdParams(rowset, items);
            assignSelectionMap(items);

            // set position at session object
            if (loadPageFromPosition)
            { // set valid
                if (position + items.size() > lti.getItemCount())
                { // Oops: More items than expected.
                    log.warn("Item count of {} has changed. Adjusting item count.", getPropertyName());
                    lti.init(position + items.size(), lti.getPageSize());
                }
                lti.setPosition(position);
                lti.setModified(false);
                lti.setValid(true);
            }
            else
            { // Init the list
                lti.init(items.size(), 0);
            }
        }
        catch (RuntimeException e)
        {
            log.error("Error loading bean list " + e.getMessage(), e);
            throw e;
        }
        finally
        {
            r.close();
            // Pagination
            if (lti.isAllowPagination())
            { // Scrollbar   
                if (initScrollbar)
                    initScrollbar();
                else
                    updateScrollbar();
            }
        }
    }

    /**
     * set order by for db queries
     * 
     * @param cmd
     */
    protected void setOrderBy(DBCommand cmd)
    {
        cmd.clearOrderBy();
        String sortColumnName = getTableInfo().getSortColumnName();
        boolean sortAscending = getTableInfo().getSortAscending();

        // Find Column
        DBColumnExpr sortColumn = rowset.getColumn(sortColumnName);
        if (sortColumn == null && (getPage() instanceof ListColumnFinder))
        {   // Ask Page
            sortColumn = ((ListColumnFinder)getPage()).findColumn(sortColumnName);
        }
        if (sortColumn == null)
        {
            log.error("Invalid Sort Column {}. Using Default!", sortColumnName);
            sortColumn = (DBColumn) getDefaultSortColumn();
            getTableInfo().setSortColumnName(sortColumn.getName());
        }
        // set Order
        setOrderBy(cmd, sortColumn, sortAscending);
    }

    protected void setOrderBy(DBCommand cmd, DBColumnExpr sortColumn, boolean sortAscending)
    {
        // Sort now
        if (sortAscending)
        {
            cmd.orderBy(sortColumn);
        }
        else
        {
            cmd.orderBy(sortColumn.desc());
        }
        // Secondary sort
        if (this.secondarySortOrder != null && !secondarySortOrder.getColumn().equals(sortColumn))
        {
            cmd.orderBy(secondarySortOrder);
        }
    }
    
    /* Scrollbar relacted functions */

    /**
     * addJavascriptCall to initScrollbar
     */
    public void initScrollbar()
    {
        ListTableInfo lti = getTableInfo();
        // init Scrollbar on page
        int max = lti.getItemCount() - lti.getPageSize();
        if (max < 0)
            max = 0;
        int pos = max;
        if (max > lti.getPosition())
            pos = max - lti.getPosition();
        // init now
        StringBuilder b = new StringBuilder();
        b.append("initScrollbar('");
        b.append(String.valueOf(getPropertyName()));
        b.append("',");
        b.append(String.valueOf(max));
        b.append(",");
        b.append(String.valueOf(pos));
        b.append(",");
        b.append(String.valueOf(lti.getPageSize()));
        b.append(");");
        getPage().addJavascriptCall(b.toString());
    }

    /**
     * addJavascriptCall to updateScrollbar
     */
    public void updateScrollbar()
    {
        int pos = getScrollbarPosition();
        StringBuilder b = new StringBuilder();
        b.append("updateScrollbar('");
        b.append(String.valueOf(getPropertyName()));
        b.append("',");
        b.append(String.valueOf(pos));
        b.append(");");
        getPage().addJavascriptCall(b.toString());
    }

    public int getScrollbarPosition()
    {
        ListTableInfo lti = getTableInfo();
        if (!lti.isValid())
            return 0;
        if (lti.getItemCount() <= lti.getPageSize())
            return 0;
        int pos = lti.getItemCount() - lti.getPageSize() - lti.getPosition();
        return (pos >= 0 ? pos : 0);
    }

    public void setScrollbarPosition(int value)
    {
        // should never come here!
    }

    public void positionValueChanged(ValueChangeEvent ve)
    {
        ListTableInfo lti = getTableInfo();
        int val = ObjectUtils.getInteger(ve.getNewValue());
        int pos = lti.getItemCount() - lti.getPageSize() - val;
        lti.setPosition((pos > 0 ? pos : 0));
    }

    @Override
    public Set<Object[]> getSelectedItems()
    {
        if (selectedItems == null)
            return null;
        // Get the set
        Set<Object[]> items = new HashSet<Object[]>(selectedItems.size());
        for (String idParam : selectedItems)
        {
            Object[] key = getParameterMap().get(rowset, idParam);
            if (key == null)
            {
                log.warn("Object does not exist in ParameterMap!");
                continue;
            }
            items.add(key);
        }
        return items;
    }

    public void setSelectedItems(Set<Object[]> items)
    {
        if (selectedItems == null)
            throw new NotSupportedException(this, "setSelectedItems");
        // Get the set
        selectedItems = new SelectionSet(items.size());
        for (Object[] key : items)
        {
            if (key == null || key.length == 0)
            {
                log.warn("Cannot select Null-Object.");
                continue;
            }
            String idParam = getParameterMap().put(rowset, key);
            selectedItems.add(idParam);
        }
    }

    protected void generateIdParams(DBRowSet rowset, List<?> items)
    {
        DBColumn[] keyCols = rowset.getKeyColumns();
        if (keyCols == null)
            throw new RuntimeException("No Primary Key");
        // generate all
        for (Object item : items)
        {
            Object[] key = getItemKey(keyCols, item);
            String idparam = getParameterMap().put(rowset, key);
            try
            {
                BeanUtils.setProperty(item, IDPARAM_PROPERTY, idparam);
            }
            catch (Exception e)
            {
                String msg = "Error setting property idparam on bean.";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
    }

    protected Object[] getItemKey(DBColumn[] cols, Object item)
    {
        Object[] key = new Object[cols.length];
        for (int i = 0; i < cols.length; i++)
        {
            if (item instanceof DBRecordData)
            {
                key[i] = ((DBRecordData) item).getValue(cols[i]);
            }
            else
            { // Bean Property Name
                String propName = cols[i].getBeanPropertyName();
                if (propName == null || propName.length() == 0)
                    throw new RuntimeException("Invalid Bean Property Name");
                // Get Property value
                try
                {
                    key[i] = BeanUtils.getSimpleProperty(item, propName);
                }
                catch (Exception e)
                {
                    String msg = "Error getting property '" + propName + "' from bean.";
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
        }
        return key;
    }

    @Override
    public DBCommand getItemQueryCmd()
    {
        BeanListTableInfo lti = (BeanListTableInfo) getTableInfo();
        DBCommand cmd = lti.getQueryCmd().clone();
       
      
        
        Set<Object[]> items = getSelectedItems();
        
        if (items.size()>0)
        {
            DBColumn[] pk = rowset.getKeyColumns();
            DBColumnExpr keyExpr = pk[0];
           
            for (int i=1; i<pk.length; i++)
            {
                keyExpr = keyExpr.append(pk[i]);
            }
            
            String[] keys = new String[items.size()];
            int i = 0;

            for (Object[] item : items)
            {
                keys[i++] = StringUtils.arrayToString(item, "");
            }
            if (isInvertSelection())
                cmd.where(keyExpr.notIn(keys));
            else
                cmd.where(keyExpr.in(keys));
        }
        // clear previous settings without the where causes
        cmd.clearSelect();
        cmd.clearGroupBy();
        cmd.clearOrderBy();
        return cmd;
    }

}
