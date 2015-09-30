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
package org.apache.empire.db.eclipse.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.empire.db.eclipse.CodeGenConfig;
import org.apache.empire.db.eclipse.model.ConfigFile;

public class Navigator implements INavigator
{
    private final List<Page> pages;

    private int              currentPage;

    private ConfigFile       currentConfig;

    public Navigator()
    {
        this.pages = new ArrayList<Page>();
        this.currentPage = 0;
    }

    public void addPage(Page page)
    {
        if (this.pages.size() > 0)
        {
            page.setVisible(false);
        }
        this.pages.add(page);
    }

    public boolean removePage(Page page)
    {
        if (!this.pages.contains(page))
        {
            return false;
        }
        return this.pages.remove(page);
    }

    public boolean showNextPage()
    {
        if (!hasNextPage())
        {
            return false;
        }
        this.pages.get(this.currentPage).setVisible(false);
        this.currentPage++;
        this.pages.get(this.currentPage).setVisible(true);
        return true;
    }

    public boolean showPreviousPage()
    {
        if (!hasPreviousPage())
        {
            return false;
        }
        this.pages.get(this.currentPage).setVisible(false);
        this.currentPage--;
        this.pages.get(this.currentPage).setVisible(true);
        return true;
    }

    public boolean hasNextPage()
    {
        return this.currentPage < this.pages.size() - 1;
    }

    public boolean hasPreviousPage()
    {
        return this.currentPage > 0;
    }

    public ConfigFile save()
    {
        if (this.currentConfig == null)
        {
            return null;
        }
        for (Page page : this.pages)
        {
            page.save(this.currentConfig.getCodeGenConfig());
        }
        return this.currentConfig;
    }

    public void load(ConfigFile config)
    {
        setCurrentConfig(config);
        for (Page page : this.pages)
        {
            page.load(config != null ? config.getCodeGenConfig() : new CodeGenConfig());
        }
    }

    public ConfigFile getCurrentConfig()
    {
        return this.currentConfig;
    }

    private void setCurrentConfig(ConfigFile currentConfig)
    {
        setPagesEnabled(currentConfig != null);
        this.currentConfig = currentConfig;
    }

    public void setPagesEnabled(boolean enabled)
    {
        for (Page page : this.pages)
        {
            page.setEnabled(enabled);
        }
    }

    public boolean checkRequiredFields()
    {
        for (Page page : this.pages)
        {
            if (!page.checkRequiredFields())
            {
                return false;
            }
        }
        return true;
    }
}
