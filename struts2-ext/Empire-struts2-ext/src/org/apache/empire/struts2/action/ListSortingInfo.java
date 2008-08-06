/*
 * ESTEAM Software GmbH, 20.07.2007
 */
package org.apache.empire.struts2.action;


public interface ListSortingInfo
{
    String getSortColumn();

    void setSortColumn(String name);
    
    void setSortDescending(boolean desc);
    
    boolean isSortDescending();

    String getListPropertyName();
    
}
