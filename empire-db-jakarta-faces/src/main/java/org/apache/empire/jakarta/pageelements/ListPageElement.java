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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.faces.event.ActionEvent;

import org.apache.empire.commons.ArraySet;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.apache.empire.jakarta.app.FacesUtils;
import org.apache.empire.jakarta.pages.Page;
import org.apache.empire.jakarta.pages.PageElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ListPageElement<T> extends PageElement
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    private static final Logger log              = LoggerFactory.getLogger(ListPageElement.class);

    protected Class<T>          beanClass;

    protected List<T>           items            = null;

    protected SelectionSet      selectedItemKeys = null;
    
    /**
     * SelectionSet
     * Contains the key of all items implementing "SelectableItem"
     */
    public static class SelectionSet extends ArraySet<Object[]>
    {
        private static final long serialVersionUID = 1L;
        
        private boolean singleSelection = false;
        private boolean invertSelection = false;
        
        public SelectionSet()
        {
            super();
        }
        
        public SelectionSet(int size)
        {
            super(size);
        }
    
        public boolean isSingleSelection()
        {
            return singleSelection;
        }

        public void setSingleSelection(boolean singleSelection)
        {
            this.singleSelection = singleSelection;
            clear();
        }

        public boolean isInvertSelection()
        {
            return invertSelection;
        }

        public boolean set(Object[] e)
        {
            clear();
            return super.add(e);
        }

        @Override
        public boolean add(Object[] e)
        {
            if (singleSelection)
                return false;
            return super.add(e);
        }

        @Override
        public boolean remove(Object o)
        {
            if (singleSelection)
                return false;
            return super.remove(o);
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

    /**
     * Interface to uniquely identify a list item
     * This will be used for selection and navigation 
     */
    public static interface ParameterizedItem {

        public String getIdParam();
        
        public void setIdParam(String idParam);
    }
    
    /**
     * Abstract superclass to make a list item selectable
     */
    public static abstract class SelectableItem // *Deprecated* implements Serializable
    {
        // *Deprecated* private static final long serialVersionUID = 1L;

        private Object[] key;
        private SelectionSet selectSet = null;

        protected void initSelect(Object[] key, SelectionSet selectSet)
        {
            // check params
            if (key==null || selectSet==null)
                throw new InvalidArgumentException("key|selectSet", null);
            // init
            this.key = key;
            this.selectSet = selectSet;
        }

        public boolean isSelected()
        {
            if (this.key==null)
                throw new ObjectNotValidException(this);
            return this.selectSet.contains(key);
        }

        public void setSelected(boolean selected)
        {
            if (this.key==null)
                throw new ObjectNotValidException(this);
            // select or unselect
            if (selected != this.selectSet.isInvertSelection())
            {
                this.selectSet.add(key);
            }
            else
            {
                this.selectSet.remove(key);
            }
        }

        public Object[] getKey()
        {
            return key;
        }
    }

    /**
     * This class holds information about the list view to display.
     * This will be held on the session in order to maintain position and sorting when navigating back and forth. 
     */
    public static class ListTableInfo // *Deprecated* implements Serializable
    {
        // *Deprecated* private static final long serialVersionUID = 1L;

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

        /* sorting */

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

        public boolean isSortAscending()
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
        
        public void setSortColumn(ActionEvent ae)
        {
            // Get column
            Object column = FacesUtils.getActionEventAttribute(ae, "item", Object.class);
            if (column==null) {
                log.warn("Unable to set sort column: Attribute \"item\" missing on command link component!");
                return;
            }
            // Get columnName
            String columnName;
            if (column instanceof Column) {
                columnName = ((Column)column).getName();
            } else {
                columnName = column.toString();
            }
            // Change Column or Sort Order
            if (StringUtils.compareEqual(columnName, getSortColumnName(), true)) {
                // change order only
                setSortAscending(!isSortAscending());
            } else {
                setSortColumnName(columnName);
            }
        }

        /* pagination */

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

        public boolean isFirstPage()
        {
            return (isValid() && isAllowPagination() && this.position==0);
        }

        public boolean isLastPage()
        {
            return (isValid() && isAllowPagination() && this.position + this.pageSize >= this.itemCount);
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
         * @param e the action event
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
         * @param e the action event
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

        /**
         * set the next x entries
         * @param e the action event
         */
        public void firstPage(ActionEvent e)
        {
            if (!isAllowPagination())
            {
                throw new NotSupportedException(this, "nextPage");
            }
            // Check
            if (this.position==0)
            {
                return; // Already on first Page
            }
            // First page
            this.position = 0;
            this.modified = true;
        }

        /**
         * set the next x entries
         * @param e the action event
         */
        public void lastPage(ActionEvent e)
        {
            if (!isAllowPagination())
            {
                throw new NotSupportedException(this, "nextPage");
            }
            // Check 
            if (this.position == this.itemCount - this.pageSize)
            {
                return; // Already on last page
            }
            // One page forward
            this.position = this.itemCount - this.pageSize;
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
        
        public void setPageNumber(int newPageNumber) 
        {
            setPosition((--newPageNumber) * this.pageSize);
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
            this.selectedItemKeys = new SelectionSet();
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

    /**
     * returns whether the item list has been loaded 
     * @return true if the item list has been loaded even it contains no items for false otherwise
     */
    public final boolean isValid()
    {
        return (this.items != null);
    }

    /**
     * returns true if the either the item list is not valid or if it contains no items 
     * @return true if the either the item list is not valid or if it contains no items
     */
    public final boolean isEmpty()
    {
        return (this.items==null || this.items.size()==0);
    }

    /**
     * returns whether the item list is valid and contains at least one item 
     * @return true if the item list contains one or more items or false otherwise
     */
    public final boolean isNotEmpty()
    {
        return (this.items!=null && this.items.size()>0);
    }

    /**
     * added as "isEmpty" is not accessible from EL.
     * @return true if the either the item list is not valid or if it contains no items
     */
    public final boolean isBlank()
    {
        return isEmpty();
    }

    /**
     * returns the total item count of the entire list (not just the visible part)
     * @return the total number of items in the list
     */
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
        if (this.selectedItemKeys != null)
        {
            this.selectedItemKeys.clear();
        }
    }

    public boolean isHasSelection()
    {
        if (this.selectedItemKeys==null)
            return false;
        // Has selected Items
        return (selectedItemKeys.size()>0 || selectedItemKeys.isInvertSelection());
    }

    public int getSelectedItemCount()
    {
        if (this.selectedItemKeys==null)
            return 0;
        // Item count
        return this.selectedItemKeys.size();
    }

    public List<T> getSelectedItems()
    {
        if (this.selectedItemKeys==null)
            throw new NotSupportedException(this, "getSelectedItems");
        // find all items
        List<T> selection = new ArrayList<T>(this.selectedItemKeys.size());
        for (T item : getItems())
        {
            if (((SelectableItem)item).isSelected())
                selection.add(item);
        }
        return selection;
    }

    public Set<Object[]> getSelectedItemKeys()
    {
        return selectedItemKeys;
    }

    public void setSelectedItems(Set<Object[]> items)
    {
        if (selectedItemKeys == null)
            throw new NotSupportedException(this, "setSelectedItems");
        // Get the set
        selectedItemKeys = new SelectionSet(items.size());
        for (Object[] key : items)
        {
            if (key == null || key.length == 0)
            {
                log.warn("Cannot select Null-Object.");
                continue;
            }
            selectedItemKeys.add(key);
        }
    }
    
    public boolean isInvertSelection()
    {
        if (this.selectedItemKeys==null)
            return false;
        // Invert selection
        return this.selectedItemKeys.isInvertSelection();
    }

    public void setInvertSelection(boolean invertSelection)
    {
        if (this.selectedItemKeys==null)
            throw new NotSupportedException(this, "setInvertSelection");
        // Invert
        this.selectedItemKeys.setInvertSelection(invertSelection);
    }
    
    public boolean isSingleSelection()
    {
        if (this.selectedItemKeys==null)
            return false;
        // Invert selection
        return this.selectedItemKeys.isSingleSelection();
    }

    public void setSingleSelection(boolean singleSelection)
    {
        if (this.selectedItemKeys==null)
            throw new NotSupportedException(this, "setSingleSelection");
        // Invert
        this.selectedItemKeys.setSingleSelection(singleSelection);
    }

    public void setSelection(SelectableItem item)
    {
        if (this.selectedItemKeys==null)
            throw new NotSupportedException(this, "setInvertSelection");
        // Invert
        if (item!=null)
            this.selectedItemKeys.set(item.getKey());
        else
            this.selectedItemKeys.clear();
    }

    public void setSelection(SelectableItem[] items)
    {
        if (this.selectedItemKeys==null)
            throw new NotSupportedException(this, "setInvertSelection");
        // Invert
        this.selectedItemKeys.clear();
        for (SelectableItem item : items)
            this.selectedItemKeys.add(item.getKey());
    }
}
