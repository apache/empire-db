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
package org.apache.empire.jakarta.pageelements;

import java.util.List;

import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.jakarta.pages.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticListPageElement<T> extends ListPageElement<T>
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    private static final Logger log              = LoggerFactory.getLogger(StaticListPageElement.class);

    private ListTableInfo       listTableInfo    = new ListTableInfo();

    public StaticListPageElement(Page page, Class<T> beanClass, String propertyName)
    {
        super(page, beanClass, propertyName);
    }

    @Override
    protected void onInitPage()
    {
        if (items == null)
        { // loadBookings
            log.error("StaticListPageElement has not been intialized! Please initialize in doInit() before calling super.doInit()!");
            throw new ObjectNotValidException(this);
        }
    }

    @Override
    public ListTableInfo getTableInfo()
    {
        return listTableInfo;
    }

    public void setItems(List<T> items)
    {
        clearItems();
        this.items = items;
        update();
    }
    
    public int size()
    {
        return (items!=null ? items.size() : 0);
    }
    
    public boolean contains(T item)
    {
        return (items!=null ? items.contains(item) : false);
    }
    
    public boolean add(T item)
    {
        if (items==null || items.contains(item))
            return false;
        // remove
        boolean added = items.add(item);
        if (added)
            update();
        return added;
    }
    
    public boolean add(T item, int index)
    {
        if (items==null || items.contains(item))
            return false;
        // remove
        items.add(index, item);
        update();
        return true;
    }
    
    public boolean move(T item, int index)
    {
        if (items==null || !items.contains(item))
            return false;
        // remove
        items.add(index, item);
        update();
        return true;
    }
    
    public boolean remove(T item)
    {
        if (items==null)
            return false;
        // remove
        boolean removed = items.remove(item);
        if (removed)
            update();
        return removed;
    }
    
    public void update()
    {
        if (items!=null)
            getTableInfo().init(items.size(), 0);
    }

}
