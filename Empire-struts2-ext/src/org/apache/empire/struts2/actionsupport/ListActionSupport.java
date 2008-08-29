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
package org.apache.empire.struts2.actionsupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.data.ColumnExpr;
import org.apache.empire.struts2.action.ListPagingInfo;
import org.apache.empire.struts2.action.ListSortingInfo;


/**
 * ListActionSupport
 * <p>
 * This action support object provides functions for dealing with list paging and sorting.<br>
 * Please used either ReaderListActionSupport or BeanListActionSupport object.<br> 
 * </p>
 * @author Rainer
 */
public abstract class ListActionSupport extends ErrorObject
    implements ListPagingInfo, ListSortingInfo
{
    protected static Log log = LogFactory.getLog(ListActionSupport.class);
    
    protected ActionBase action;
    protected String propertyName;

    public ListActionSupport(ActionBase action, String propertyName)
    {
        this.action = action;
        this.propertyName = propertyName;
    }

    public String getListPropertyName()
    {
        return propertyName;
    }
    
    // ------- ListPageInfo -------
    
    protected static final class ListPageInfo
    {
        public int firstItemIndex = 0;
        public int itemCount = 0;
    }

    private ListPageInfo lpi = null;
    protected ListPageInfo getListPageInfo()
    {
        if (lpi==null)
            lpi = (ListPageInfo)action.getActionBean(ListPageInfo.class, true, propertyName);
        return lpi;
    }
    
    // -------- Paging --------
    
    public int getPageSize()
    {
        return action.getListPageSize();
    }
    
    protected void setFirstItem(int firstItemIndex)
    {
        getListPageInfo().firstItemIndex = firstItemIndex;
    }

    public int getFirstItemIndex()
    {
        return getListPageInfo().firstItemIndex;
    }

    public int getLastItemIndex()
    {
        int first = getFirstItemIndex();
        return Math.min(first + getPageSize()-1, getItemCount()-1); 
    }
    
    public int getItemCount()
    {
        return getListPageInfo().itemCount;
    }

    public void setItemCount(int itemCount)
    {
        getListPageInfo().firstItemIndex = 0;
        getListPageInfo().itemCount = itemCount;
    }
    
    public int getPageCount()
    {
        if(getItemCount()%getPageSize()==0)
            return (getItemCount()/getPageSize());
        else
            return (getItemCount()/getPageSize()+1);
    }

    public int getPage()
    {
        return (getFirstItemIndex() / getPageSize());
    }

    public void setPage(int page)
    {
        if (page >getPageCount())
            page =getPageCount();
        // Set first Index
        setFirstItem(page * getPageSize());
    }
    
    // ------- ListSortInfo -------
    
    protected static final class ListSortInfo
    {
        public String  sortColumn; 
        public boolean sortDescending;
    }
    
    private ListSortInfo lsi = null;
    protected ListSortInfo getListSortInfo()
    {
        if (lsi== null)
            lsi = (ListSortInfo)action.getActionBean(ListSortInfo.class, true, propertyName);
        return lsi;
    }
    
    // ------- ListSortInfo -------
    
    public String getSortColumn()
    {
        return getListSortInfo().sortColumn;
    }

    public void setSortColumn(ColumnExpr column)
    {
        getListSortInfo().sortColumn = column.getName();
    }
    
    public void setSortColumn(String column)
    {
        // sort column
        @SuppressWarnings("hiding")
        ListSortInfo lsi = getListSortInfo();
        if (column!=null && column.equalsIgnoreCase(lsi.sortColumn))
        {
            lsi.sortDescending = !lsi.sortDescending;
        }
        else
        {   // Sort by a different Column
            lsi.sortColumn = column;
            lsi.sortDescending = false;
        }
    }

    public boolean isSortDescending()
    {
        return getListSortInfo().sortDescending;
    }

    public void setSortDescending(boolean sortDescending)
    {
        getListSortInfo().sortDescending = sortDescending;
    }
}
