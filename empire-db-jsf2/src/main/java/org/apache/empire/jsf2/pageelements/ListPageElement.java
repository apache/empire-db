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

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.jsf2.pages.Page;
import org.apache.empire.jsf2.pages.PageElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ListPageElement<T> extends PageElement
{
    private static final long   serialVersionUID = 1L;

    private static final Logger log              = LoggerFactory.getLogger(ListPageElement.class);

    protected Class<T>          beanClass;

    protected List<T>           items            = null;

    protected SelectionSet      selectedItems    = null;
    
    public static class SelectionSet extends HashSet<String>
    {
        private static final long serialVersionUID = 1L;
        
        private boolean invertSelection = false;
        
        public SelectionSet()
        {
            super();
        }
        
        public SelectionSet(int size)
        {
            super(size);
        }
    
        public boolean isInvertSelection()
        {
            return invertSelection;
        }

        public void setInvertSelection(boolean invertSelection)
        {
            if (this.invertSelection==invertSelection)
                return; // no change
            // invert 
            this.invertSelection = invertSelection;
            this.clear();
        }
        
        @Override
        public boolean contains(Object item)
        {
            boolean exists = super.contains(item);
            return (invertSelection ? !exists : exists);
        }
    }

    public static abstract class SelectableItem implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private SelectionSet      selectSet        = null;

        protected void setSelectMap(SelectionSet selectSet)
        {
            this.selectSet = selectSet;
        }

        public boolean isSelected()
        {
            String id = getIdParam();
            return this.selectSet.contains(id);
        }

        public void setSelected(boolean selected)
        {
            String id = getIdParam();
            if (selected != this.selectSet.isInvertSelection())
            {
                this.selectSet.add(id);
            }
            else
            {
                this.selectSet.remove(id);
            }
        }

        public abstract String getIdParam();

    }

    public static class ListTableInfo implements Serializable
    {
        private static final long serialVersionUID = 1L;

        /** Pagination **/
        /**
         * position, starts with 0
         */
        private int               itemCount        = -1;
        private boolean           valid            = false;
        private boolean           modified         = false;

        /** Sorting **/
        private String            sortColumnName;
        private boolean           sortAscending    = true;
        private boolean           sortOrderChanged = false;

        /** Pagination **/
        private int               position         = 0;
        private int               pageSize         = 0;

        public void init(int itemCount, int pageSize)
        {
            if (pageSize < 0)
            {   // pageSize must not be negative!
                throw new ObjectNotValidException(this);
            }
            this.itemCount = itemCount;
            this.pageSize = pageSize;
            this.position = 0;
            this.valid = (itemCount >= 0);
            this.modified = false;
        }

        public boolean isValid()
        {
            return this.valid;
        }

        public void setValid(boolean valid)
        {
            if (valid && (this.itemCount < 0))
            { // itemCount and position must not be negative!
                throw new ObjectNotValidException(this);
            }
            this.valid = valid;
        }

        public boolean isModified()
        {
            return (this.valid && this.modified);
        }

        public void setModified(boolean modified)
        {
            this.modified = modified;
        }

        public int getItemCount()
        {
            return this.itemCount;
        }

        public int getPageSize()
        {
            return this.pageSize;
        }

        /*** sorting ***/

        public String getSortColumnName()
        {
            return this.sortColumnName;
        }

        public void setSortColumnName(String column)
        {
            if (ObjectUtils.compareEqual(column, this.sortColumnName))
            {
                return;
            }
            // change value
            this.sortColumnName = column;
            this.position = 0;
            this.modified = true;
            this.sortOrderChanged = true;
        }

        public boolean getSortAscending()
        {
            return this.sortAscending;
        }

        public void setSortAscending(boolean sortAscending)
        {
            if (this.sortAscending == sortAscending)
            {
                return;
            }
            // change value
            this.sortAscending = sortAscending;
            this.modified = true;
            this.sortOrderChanged = true;
        }

        public boolean isSortOrderChanged()
        {
            return this.sortOrderChanged;
        }

        public void setSortOrderChanged(boolean sortOrderChanged)
        {
            this.sortOrderChanged = sortOrderChanged;
        }

        /*** pagination ***/

        public int getPosition()
        {
            return this.position;
        }

        public void setPosition(int position)
        {
            if (this.pageSize == 0)
            {
                throw new NotSupportedException(this, "setPosition");
            }
            if (position < 0)
            {
                position = 0;
            }
            if (position >= this.itemCount)
            {
                position = this.itemCount - 1;
            }
            if (this.position == position)
            {
                return;
            }
            // change value
            this.position = position;
            this.modified = true;
        }

        public boolean isAllowPagination()
        {
            return (this.pageSize > 0);
        }

        public boolean isHasNextPage()
        {
            if (isValid() && isAllowPagination())
            {
                return ((this.itemCount - (this.position + this.pageSize)) > 0);
            }
            return false;
        }

        public boolean isHasPrevPage()
        {
            return (isValid() && isAllowPagination() && this.position > 0);
        }

        /**
         * set the next x entries
         */
        public void nextPage(ActionEvent e)
        {
            if (!isAllowPagination())
            {
                throw new NotSupportedException(this, "nextPage");
            }
            // Check
            if ((this.position + this.pageSize) > this.itemCount)
            {
                return; // Already on last page
            }
            // One page forward
            this.position += this.pageSize;
            this.modified = true;
        }

        /**
         * set the prev x entries
         */
        public void prevPage(ActionEvent e)
        {
            if (!isAllowPagination())
            {
                throw new NotSupportedException(this, "prevPage");
            }
            // Check
            if (this.position == 0)
            {
                return; // Already on first page
            }
            // one page back
            if (this.position > this.pageSize)
            {
                this.position -= this.pageSize;
            }
            else
            {
                this.position = 0;
            }
            // modified
            this.modified = true;
        }

        public int getPageNumber()
        {
            if (!isAllowPagination())
            {
                return 1; // always first page
            }
            // Calc Page num
            int pageNumber = new Double(Math.ceil(((double) this.position / this.pageSize))).intValue() + 1;
            return pageNumber;
        }

        public int getPageCount()
        {
            if (!isAllowPagination())
            {
                return 1; // only one page
            }
            // Calc Page count
            int pageCount = new Double(Math.ceil(((double) this.itemCount / this.pageSize))).intValue();
            return pageCount;
        }

    }

    public ListPageElement(Page page, Class<T> beanClass, String propertyName)
    {
        super(page, propertyName);
        // set bean Class
        this.beanClass = beanClass;
        // selectable
        if (ListPageElement.isSelectableItem(beanClass))
        {
            this.selectedItems = new SelectionSet();
        }
    }

    private static <T> boolean isSelectableItem(Class<T> beanClass)
    {
        Class<?> superClass = beanClass.getSuperclass();
        while (superClass != null)
        { // Check Superclass
            if (superClass.equals(SelectableItem.class))
            {
                return true;
            }
            superClass = superClass.getSuperclass();
        }
        return false;
    }

    /** session scoped properties **/
    public abstract ListTableInfo getTableInfo();
    
    public List<T> getItems()
    {
        if (this.items == null)
        {
            ListPageElement.log.warn("Bean List has not been initialized!");
        }
        return this.items;
    }

    // DS only valid if there are items in the list
    public final boolean isValid()
    {
        return (this.items == null) ? false : this.items.size() > 0;
    }

    public int getItemCount()
    {
        return getTableInfo().getItemCount();
    }

    public void clearItems()
    {
        clearSelection();
        this.items = null;
    }

    /*** Selection ***/

    public void clearSelection()
    {
        if (this.selectedItems != null)
        {
            this.selectedItems.clear();
        }
    }

    public boolean isHasSelection()
    {
        if (this.selectedItems==null)
            return false;
        // Has selected Items
        return (selectedItems.size()>0 || selectedItems.isInvertSelection());
    }

    public int getSelectedItemCount()
    {
        if (this.selectedItems==null)
            return 0;
        // Item count
        return this.selectedItems.size();
    }

    public Set<Object[]> getSelectedItems()
    {
        // if (this.selectedItems==null)
        throw new NotSupportedException(this, "getSelectedItems");
    }
    
    public boolean isInvertSelection()
    {
        if (this.selectedItems==null)
            return false;
        // Invert selection
        return this.selectedItems.isInvertSelection();
    }

    public void setInvertSelection(boolean invertSelection)
    {
        if (this.selectedItems==null)
            throw new NotSupportedException(this, "setInvertSelection");
        // Invert
        this.selectedItems.setInvertSelection(invertSelection);
    }

    protected void assignSelectionMap(List<?> items)
    {
        // Check selectable
        if (this.selectedItems == null)
            return;
        // generate all
        for (Object item : items)
        {
            if (item instanceof SelectableItem)
            {
                ((SelectableItem) item).setSelectMap(this.selectedItems);
            }
        }
    }
}
