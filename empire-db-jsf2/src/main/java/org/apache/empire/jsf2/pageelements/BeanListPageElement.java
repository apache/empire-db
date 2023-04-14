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
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.expr.order.DBOrderByExpr;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnspecifiedErrorException;
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
    // *Deprecated* private static final long serialVersionUID = 1L;

    private static final Logger log                  = LoggerFactory.getLogger(BeanListPageElement.class);
    
    public static final String  NO_RESULT_ATTRIBUTE  = "noQueryResult";

    private ListTableInfo       listTableInfo        = null;

    protected DBRowSet          rowset;

    protected Column            defaultSortColumn;

    protected boolean           defaultSortAscending = true;

    protected DBOrderByExpr     secondarySortOrder   = null;
    
    protected int               maxItemCount = 1000;
    
    /**
     * Extended ListTableInfo
     */
    public static class BeanListTableInfo extends ListTableInfo
    {
        // *Deprecated* private static final long serialVersionUID = 1L;

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

    /**
     * Constructor for creating a BeanListPageElement
     * @param page the page element
     * @param beanClass the bean class
     * @param rowset required Table or View
     * @param defaultSortColumn sort column that must belong to rowset
     * @param propertyName the property name which is used to get and retrieve session information
     */
    public BeanListPageElement(Page page, Class<T> beanClass, DBRowSet rowset, DBColumn defaultSortColumn, String propertyName)
    {
        super(page, beanClass, propertyName);
        // Check
        if (rowset == null)
            throw new InvalidArgumentException("rowset", rowset);
        // Default Sort Order
        if (defaultSortColumn!=null)
        {   // Date sort order is descending by default
            if (defaultSortColumn.getRowSet()!=rowset)
                throw new InvalidArgumentException("defaultSortColumn", defaultSortColumn);
            if (defaultSortColumn.getDataType().isDate())
                defaultSortAscending = false;
        }
        // Set Bean Class and more
        this.rowset = rowset;
        this.defaultSortColumn = defaultSortColumn;
    }

    /**
     * Overload that requires a default sort order to be provided
     * @param page the page element
     * @param beanClass the bean class
     * @param defaultSortColumn the default sort column
     * @param propertyName the property name which is used to get and retrieve session information
     */
    public BeanListPageElement(Page page, Class<T> beanClass, DBColumn defaultSortColumn, String propertyName)
    {
        this(page, beanClass, defaultSortColumn.getRowSet(), defaultSortColumn, propertyName);
    }

    /**
     * Overload that requires a default sort order to be provided
     * @param page the page element
     * @param beanClass the bean class
     * @param defaultSortColumn the default sort column
     */
    public BeanListPageElement(Page page, Class<T> beanClass, DBColumn defaultSortColumn)
    {
        this(page, beanClass, defaultSortColumn.getRowSet(), defaultSortColumn, getDefaultPropertyName(defaultSortColumn.getRowSet()));
    }

    /**
     * Overload that requires a default sort order to be provided
     * @param page the page element
     * @param beanClass the bean class
     * @param rowSet required Table or View
     */
    public BeanListPageElement(Page page, Class<T> beanClass, DBRowSet rowSet)
    {
        this(page, beanClass, rowSet, null, getDefaultPropertyName(rowSet));
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
            listTableInfo.setSortColumnName((defaultSortColumn!=null ? defaultSortColumn.getName() : null));
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
     * @param queryCmd the query command
     * @param countCmd the count command
     * @param pageSize the page size
     */
    public void initItems(DBCommand queryCmd, DBCommand countCmd, int pageSize)
    {
        clearItems();
        DBContext context = getDBContext(rowset);
        // Init List Table Info
        BeanListTableInfo lti = (BeanListTableInfo) getTableInfo();
        lti.setQueryCmd(queryCmd);
        if (pageSize > 0)
        { // Negative count means: loadItems should load all items.
            countCmd.clearSelect();
            countCmd.select(rowset.count());
            int count = context.getUtils().querySingleInt(countCmd, 0);
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
            handleNoResult();
        // log
        log.info("ItemList initialized for {} item count is {}.", getPropertyName(), count);
    }

    /**
     * handle the case of an empty query result
     */
    protected void handleNoResult()
    {
        FacesUtils.setRequestAttribute(FacesUtils.getContext(), NO_RESULT_ATTRIBUTE, true);
    }

    /**
     * Init list items without pagination
     * 
     * @param queryCmd the query commmand
     * @param pageSize the page size
     */
    public final void initItems(DBCommand queryCmd, int pageSize)
    {
        DBCommand countCmd = queryCmd.clone();
        initItems(queryCmd, countCmd, 0);
    }

    /**
     * Init list items without pagination
     * 
     * @param queryCmd the query commmand
     */
    public final void initItems(DBCommand queryCmd)
    {
        initItems(queryCmd, 0);
    }

    /**
     * Returns true if (and only if) items are valid but empty
     * @return true if reault is empty
     */
    public boolean isResultEmpty()
    {
        Object noQueryResult = FacesUtils.getRequestAttribute(FacesUtils.getContext(), NO_RESULT_ATTRIBUTE);
        return (noQueryResult!=null) ? ObjectUtils.getBoolean( noQueryResult ) : false;
    }

    /**
     * loads all visible list items from the database
     * @param initScrollbar flag whether to init the scrollbar
     */
    protected void loadItems(boolean initScrollbar)
    {
        // DBReader
        BeanListTableInfo lti = (BeanListTableInfo) getTableInfo();
        DBReader r = new DBReader(getDBContext(rowset));
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
            
            int position = 0;
            int skipRows = 0;
            int maxItems = maxItemCount;
            if (loadPageFromPosition)
            {   // detect position
                position = lti.getPosition();
                if (position > lti.getItemCount() - lti.getPageSize())
                { // position > count of entries is not possible, set to max
                    position = lti.getItemCount() - lti.getPageSize();
                }
                if (position < 0)
                { // position < 0 is not possible, set to 0
                    position = 0;
                }
                // maxItems
                maxItems = lti.getPageSize();
                skipRows = position;
                // constraint
                queryCmd.clearLimit();
                DBMSHandler dbms = r.getContext().getDbms(); 
                if (dbms.isSupported(DBMSFeature.QUERY_LIMIT_ROWS))
                {   // let the database limit the rows
                    if (dbms.isSupported(DBMSFeature.QUERY_SKIP_ROWS))
                    {   // let the database skip the rows
                        queryCmd.skipRows(skipRows);
                        skipRows = 0;
                    }
                    queryCmd.limitRows(skipRows+maxItems);
                }
            }

            // DBReader.open must always be surrounded with a try {} finally {} block!
            r.open(queryCmd);

            // get position from the session
            if (skipRows>0)
            {   // we are not at position 0, "skipping" entries
                r.skipRows(skipRows);
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
     * @param cmd the command
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
            if (sortColumn!=null)
                getTableInfo().setSortColumnName(sortColumn.getName());
        }
        // set Order
        setOrderBy(cmd, sortColumn, sortAscending);
    }

    protected void setOrderBy(DBCommand cmd, DBColumnExpr sortColumn, boolean sortAscending)
    {
        if (sortColumn!=null)
        {   // Sort now
            if (sortAscending)
            {
                cmd.orderBy(sortColumn);
            }
            else
            {
                cmd.orderBy(sortColumn.desc());
            }
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

    // @Override
    public Set<Object[]> getSelectedItemKeys()
    {
        if (selectedItems == null)
            return null;
        // Get the set
        Set<Object[]> items = new HashSet<Object[]>(selectedItems.size());
        for (String idParam : selectedItems)
        {
            Object[] key = getParameterMap().getKey(rowset, idParam);
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
        Column[] keyCols = rowset.getKeyColumns();
        if (keyCols == null)
            return; // No Primary Key!
        // generate all
        for (Object item : items)
        {
            if (!(item instanceof ParameterizedItem))
                continue;
            // set the idParam
            Object[] key = getItemKey(keyCols, item);
            String idparam = getParameterMap().put(rowset, key);
            ((ParameterizedItem)item).setIdParam(idparam);
        }
    }

    protected Object[] getItemKey(Column[] cols, Object item)
    {
        Object[] key = new Object[cols.length];
        for (int i = 0; i < cols.length; i++)
        {
            if (item instanceof DBRecordData)
            {
                key[i] = ((DBRecordData) item).get(cols[i]);
            }
            else
            { // Bean Property Name
                String propName = cols[i].getBeanPropertyName();
                if (propName == null || propName.length() == 0)
                    throw new UnspecifiedErrorException("Invalid Bean Property Name");
                // Get Property value
                try
                {
                    key[i] = BeanUtils.getSimpleProperty(item, propName);
                }
                catch (Exception e)
                {
                    String msg = "Error getting property '" + propName + "' from bean.";
                    log.error(msg, e);
                    throw new InternalException(e);
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
        
        Set<Object[]> items = getSelectedItemKeys();
        if (items.size()>0)
        {
            DBColumn[] pk = rowset.getKeyColumns();
            DBColumnExpr keyExpr = pk[0];
            for (int i=1; i<pk.length; i++)
            {
                keyExpr = keyExpr.append(pk[i]);
            }
            
            Object[] keys = new Object[items.size()];
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
        // clear skip and limit!
        cmd.clearLimit(); 
        return cmd;
    }

}
