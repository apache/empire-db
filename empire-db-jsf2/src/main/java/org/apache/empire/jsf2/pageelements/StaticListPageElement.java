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

import java.util.List;

import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.jsf2.pages.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticListPageElement<T> extends ListPageElement<T>
{
    private static final Logger log              = LoggerFactory.getLogger(BeanListPageElement.class);

    private static final long   serialVersionUID = 1L;

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
        getTableInfo().init(items.size(), 0);
    }

}
